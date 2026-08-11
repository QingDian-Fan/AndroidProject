package com.common.share.temp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import com.common.utils.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分享临时图片的统一入口。
 *
 * <p>职责：把 Bitmap 落盘到应用自有的 {@code share_temp/<sessionId>/} 目录、生成 FileProvider URI，
 * 并在分享成功、取消、失败、调起异常或安全读取期结束后按会话精准删除本次生成的文件。
 *
 * <p>不负责删除用户保存到相册的图片和调用方传入的原始文件——它们不会被登记，
 * 因此永远不在自动清理范围内。
 *
 * <p>所有删除与扫描都放到后台单线程执行，不阻塞主线程与分享 UI；
 * 只持有 {@code applicationContext}，不持有 Activity、Fragment、View 或 binding。
 */
public final class ShareTempFiles {

    private static final String TAG = "ShareTempFiles";

    /** 分享临时文件专用目录名，位于 cacheDir / externalCacheDir 之下 */
    public static final String DIR_NAME = "share_temp";

    /** 两次过期扫描之间的最小间隔，避免频繁进入分享功能时反复扫描 */
    private static final long SWEEP_THROTTLE_MILLIS = 5 * 60 * 1000L;

    /** sessionId -> 所属 store，供结果回调按标识精准清理 */
    private static final ConcurrentHashMap<String, ShareTempFileStore> SESSION_STORES =
            new ConcurrentHashMap<>();

    /** 渠道 -> 等待结果的 sessionId；SDK 回调不携带自定义参数时据此解析会话 */
    private static final ConcurrentHashMap<Integer, String> PENDING_SESSIONS =
            new ConcurrentHashMap<>();

    private static final AtomicLong LAST_SWEEP_AT = new AtomicLong(0L);

    private static volatile ShareTempFileStore internalStore;
    private static volatile ShareTempFileStore externalStore;
    private static volatile ScheduledExecutorService executor;
    private static volatile Context appContext;

    private ShareTempFiles() {
    }

    /**
     * 创建一次分享的临时文件会话。
     *
     * @param preferExternal 目标 SDK 只接受文件路径时传 {@code true}，优先使用外部缓存；
     *                       外部缓存不可用时安全回退到内部缓存
     * @return 会话对象；目录创建失败时返回 {@code null}，调用方应提示失败而不是继续传空路径
     */
    public static ShareTempSession beginSession(Context context, int channel, boolean preferExternal) {
        ShareTempFileStore store = resolveStore(context, preferExternal);
        if (store == null) {
            LogUtil.e(TAG, "share temp dir unavailable, channel=" + channel);
            return null;
        }
        // 创建新会话时顺带做一次受限的过期扫描，兜底进程被杀留下的孤儿文件
        sweepAsync(context);
        ShareTempSession session = store.createSession(channel);
        if (session == null) {
            LogUtil.e(TAG, "create share temp session failed, channel=" + channel);
            return null;
        }
        SESSION_STORES.put(session.getSessionId(), store);
        return session;
    }

    /**
     * 把 Bitmap 写入会话目录。
     *
     * <p>扩展名与压缩格式保持一致；压缩或写入失败时立即清理半成品，不会把不可读文件交给目标应用。
     *
     * @return 写入完成的文件；失败返回 {@code null}
     */
    public static File writeBitmap(ShareTempSession session, Bitmap bitmap,
                                   Bitmap.CompressFormat format, int quality) {
        if (session == null || bitmap == null || bitmap.isRecycled() || format == null) {
            return null;
        }
        ShareTempFileStore store = SESSION_STORES.get(session.getSessionId());
        if (store == null) {
            return null;
        }
        File file = store.createTempFile(session, extensionOf(format));
        if (file == null) {
            LogUtil.e(TAG, "create share temp file failed, session=" + session.getSessionId());
            return null;
        }
        boolean compressed;
        try (FileOutputStream out = new FileOutputStream(file)) {
            compressed = bitmap.compress(format, clampQuality(quality), out);
            out.flush();
        } catch (Exception e) {
            LogUtil.printStackTrace(e);
            compressed = false;
        }
        if (!compressed || file.length() <= 0) {
            store.deleteRegisteredFile(file);
            LogUtil.e(TAG, "compress share bitmap failed, session=" + session.getSessionId());
            return null;
        }
        store.markReady(session);
        return file;
    }

    /**
     * 生成本次分享使用的 FileProvider content URI 并登记。
     *
     * @return content URI；生成失败返回 {@code null}
     */
    public static Uri shareUri(Context context, ShareTempSession session, File file) {
        if (context == null || session == null || file == null || !file.exists()) {
            return null;
        }
        try {
            Uri uri = FileProvider.getUriForFile(
                    context.getApplicationContext(),
                    context.getPackageName() + ".provider",
                    file
            );
            ShareTempFileStore store = SESSION_STORES.get(session.getSessionId());
            if (store != null) {
                store.registerUri(session, uri.toString());
            }
            return uri;
        } catch (IllegalArgumentException e) {
            LogUtil.printStackTrace(e);
            return null;
        }
    }

    /** 只针对本次 URI 授予目标应用读权限，不做目录级或长期授权 */
    public static void grantRead(Context context, Uri uri, String targetPackage) {
        if (context == null || uri == null || TextUtils.isEmpty(targetPackage)) {
            return;
        }
        try {
            context.getApplicationContext()
                    .grantUriPermission(targetPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            LogUtil.printStackTrace(e);
        }
    }

    /** 标记已调起目标应用 */
    public static void markLaunched(ShareTempSession session) {
        ShareTempFileStore store = storeOf(session);
        if (store != null) {
            store.markLaunched(session);
        }
    }

    /** 标记已调起目标应用并等待 SDK 结果回调 */
    public static void markWaitingResult(ShareTempSession session) {
        ShareTempFileStore store = storeOf(session);
        if (store != null) {
            store.markWaitingResult(session);
        }
    }

    /**
     * 登记「某渠道正在等待结果」的会话标识。
     *
     * <p>QQ 与微信的结果回调不携带自定义参数，需要按渠道解析会话；
     * 这里是显式登记，不是「删除最近一张图片」。
     */
    public static void setPendingSession(int channelGroup, ShareTempSession session) {
        if (session == null) {
            return;
        }
        String previous = PENDING_SESSIONS.put(channelGroup, session.getSessionId());
        if (previous != null && !previous.equals(session.getSessionId())) {
            // 上一次分享没有收到回调（进程被杀、SDK 未回调），按安全读取期兜底清理，
            // 不影响本次会话
            finishAfterSafeDelay(previous, ShareTempFileStore.DEFAULT_SAFE_DELETE_DELAY_MILLIS);
        }
        markWaitingResult(session);
    }

    /** 取出并清除某渠道等待结果的会话标识 */
    public static String takePendingSessionId(int channelGroup) {
        return PENDING_SESSIONS.remove(channelGroup);
    }

    /**
     * 结束某渠道当前等待结果的会话并清理其临时文件。
     *
     * <p>成功、取消、失败都应调用；重复调用是幂等的。
     */
    public static void finishPendingSession(int channelGroup) {
        finishNow(takePendingSessionId(channelGroup));
    }

    /**
     * 结束某渠道等待结果的会话，但保留安全读取期后再删除。
     *
     * <p>用于「不确定目标应用是否已经读完」的结束信号，例如 QQ 的 {@code onWarning}。
     */
    public static void finishPendingSessionAfterSafeDelay(int channelGroup, long delayMillis) {
        finishAfterSafeDelay(takePendingSessionId(channelGroup), delayMillis);
    }

    /**
     * 立即结束会话并在后台删除本次登记的临时文件。
     *
     * <p>仅用于「目标应用已明确读取完成（SDK 回调）」或「文件还没交出去（生成、调起失败）」的场景。
     */
    public static void finishNow(String sessionId) {
        if (TextUtils.isEmpty(sessionId)) {
            return;
        }
        execute(new DeleteTask(sessionId, false));
    }

    /**
     * 进入安全延迟清理：预留读取时间后再删除。
     *
     * <p>系统分享面板无法证明接收方已经读完文件，只能靠安全读取期 + 过期扫描兜底。
     */
    public static void finishAfterSafeDelay(String sessionId, long delayMillis) {
        if (TextUtils.isEmpty(sessionId)) {
            return;
        }
        ShareTempFileStore store = SESSION_STORES.get(sessionId);
        if (store == null) {
            return;
        }
        long safeDelay = ShareTempFileStore.clampSafeDelay(delayMillis);
        store.markPendingSafeDelete(sessionId, safeDelay);
        schedule(new DeleteTask(sessionId, true), safeDelay);
    }

    /** 后台执行一次受限的过期扫描，按节流间隔避免高频 I/O */
    public static void sweepAsync(Context context) {
        final ShareTempFileStore internal = resolveStore(context, false);
        final ShareTempFileStore external = resolveStore(context, true);
        if (internal == null && external == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = LAST_SWEEP_AT.get();
        if (last != 0L && now - last < SWEEP_THROTTLE_MILLIS) {
            return;
        }
        if (!LAST_SWEEP_AT.compareAndSet(last, now)) {
            return;
        }
        execute(new Runnable() {
            @Override
            public void run() {
                sweepQuietly(internal);
                if (external != null && external != internal) {
                    sweepQuietly(external);
                }
            }
        });
    }

    /** PNG/JPEG/WEBP 对应的 MIME 类型，保证扩展名、压缩格式与 MIME 三者一致 */
    public static String mimeTypeOf(Bitmap.CompressFormat format) {
        if (format == Bitmap.CompressFormat.PNG) {
            return "image/png";
        }
        if (format == Bitmap.CompressFormat.JPEG) {
            return "image/jpeg";
        }
        return "image/webp";
    }

    private static String extensionOf(Bitmap.CompressFormat format) {
        if (format == Bitmap.CompressFormat.PNG) {
            return "png";
        }
        if (format == Bitmap.CompressFormat.JPEG) {
            return "jpg";
        }
        return "webp";
    }

    private static int clampQuality(int quality) {
        if (quality < 1) {
            return 1;
        }
        return Math.min(quality, 100);
    }

    private static ShareTempFileStore storeOf(ShareTempSession session) {
        return session == null ? null : SESSION_STORES.get(session.getSessionId());
    }

    /** 后台删除任务；只持有 sessionId，不持有 Activity、View 或 Bitmap */
    private static final class DeleteTask implements Runnable {
        private final String sessionId;
        private final boolean respectSafeDelay;

        DeleteTask(String sessionId, boolean respectSafeDelay) {
            this.sessionId = sessionId;
            this.respectSafeDelay = respectSafeDelay;
        }

        @Override
        public void run() {
            try {
                deleteSession(sessionId, respectSafeDelay);
            } catch (Exception e) {
                // 删除失败不得导致崩溃，等待下一次过期扫描重试
                LogUtil.printStackTrace(e);
            }
        }
    }

    private static void deleteSession(String sessionId, boolean respectSafeDelay) {
        ShareTempFileStore store = SESSION_STORES.get(sessionId);
        if (store == null) {
            return;
        }
        ShareTempSession session = store.getSession(sessionId);
        List<String> uris = session != null ? session.getUris() : Collections.<String>emptyList();
        boolean deleted = respectSafeDelay
                ? store.finishSessionIfSafe(sessionId)
                : store.finishSession(sessionId);
        if (!deleted) {
            // 删除失败或安全期未到都不算分享失败，等待下一次过期扫描重试；
            // 日志只输出会话标识，不包含文件路径与 URI 参数
            LogUtil.w(TAG, "share temp session not cleaned yet, id=" + sessionId);
            return;
        }
        SESSION_STORES.remove(sessionId);
        revokeUriPermissions(uris);
    }

    /** 文件删除后撤销本次临时授权；失败只记录，不影响分享结果 */
    private static void revokeUriPermissions(List<String> uris) {
        Context context = appContext;
        if (context == null || uris == null || uris.isEmpty()) {
            return;
        }
        for (String uri : uris) {
            try {
                context.revokeUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                LogUtil.printStackTrace(e);
            }
        }
    }

    private static void sweepQuietly(ShareTempFileStore store) {
        if (store == null) {
            return;
        }
        try {
            store.sweepExpired();
        } catch (Exception e) {
            LogUtil.printStackTrace(e);
        }
    }

    private static ShareTempFileStore resolveStore(Context context, boolean preferExternal) {
        if (context == null) {
            return null;
        }
        Context application = context.getApplicationContext();
        appContext = application;
        if (preferExternal) {
            if (externalStore == null) {
                synchronized (ShareTempFiles.class) {
                    if (externalStore == null) {
                        File root = createRoot(application.getExternalCacheDir());
                        externalStore = root != null ? new ShareTempFileStore(root) : null;
                    }
                }
            }
            if (externalStore != null) {
                return externalStore;
            }
            // 外部缓存不可用（未挂载、被系统回收）时安全回退到内部缓存
        }
        if (internalStore == null) {
            synchronized (ShareTempFiles.class) {
                if (internalStore == null) {
                    File root = createRoot(application.getCacheDir());
                    internalStore = root != null ? new ShareTempFileStore(root) : null;
                }
            }
        }
        return internalStore;
    }

    private static File createRoot(File cacheDir) {
        if (cacheDir == null) {
            return null;
        }
        File root = new File(cacheDir, DIR_NAME);
        if (!root.exists() && !root.mkdirs()) {
            return null;
        }
        return root.isDirectory() ? root : null;
    }

    private static void execute(Runnable task) {
        schedule(task, 0L);
    }

    private static void schedule(Runnable task, long delayMillis) {
        try {
            scheduler().schedule(task, Math.max(0L, delayMillis), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LogUtil.printStackTrace(e);
        }
    }

    private static ScheduledExecutorService scheduler() {
        if (executor == null) {
            synchronized (ShareTempFiles.class) {
                if (executor == null) {
                    executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable runnable) {
                            Thread thread = new Thread(runnable, "share-temp-cleaner");
                            // 守护线程：进程退出时不阻塞，遗留文件由下次过期扫描兜底
                            thread.setDaemon(true);
                            thread.setPriority(Thread.MIN_PRIORITY);
                            return thread;
                        }
                    });
                }
            }
        }
        return executor;
    }
}
