package com.common.share.temp;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分享临时文件的会话登记与精准清理核心。
 *
 * <p>本类刻意不依赖任何 Android API，便于在 JVM 单元测试中验证登记、清理、过期扫描
 * 和路径安全策略；与 {@code Context}、{@code Bitmap}、{@code FileProvider} 相关的部分
 * 全部放在 {@link ShareTempFiles}。
 *
 * <p>删除规则：只删除本类在 {@link #getRoot()} 内创建并登记的文件；删除前用规范化路径
 * 二次校验目标仍位于专用目录内，因此 {@code ../}、指向外部的符号链接和目录外路径一律拒绝。
 */
public class ShareTempFileStore {

    /** 会话默认过期时间：超过该时间且没有活动引用即可清理 */
    public static final long DEFAULT_SESSION_TTL_MILLIS = 30 * 60 * 1000L;

    /** SDK 结束信号不确定（如 QQ onWarning）时预留的安全读取时间 */
    public static final long DEFAULT_SAFE_DELETE_DELAY_MILLIS = 60 * 1000L;

    /**
     * 系统分享面板预留的安全读取时间。
     *
     * <p>面板打开后用户可能停留较久才选择目标，接收方还要异步读取，
     * 因此比 SDK 场景更长；最终仍受 {@link #MAX_SAFE_DELETE_DELAY_MILLIS} 与过期扫描约束。
     */
    public static final long CHOOSER_SAFE_DELETE_DELAY_MILLIS = 3 * 60 * 1000L;

    /** 安全读取时间上限，防止调用方传入过大延迟导致临时文件长期驻留 */
    public static final long MAX_SAFE_DELETE_DELAY_MILLIS = 5 * 60 * 1000L;

    /** 单次过期扫描最多检查的会话目录数量 */
    public static final int MAX_SWEEP_DIRECTORIES = 64;

    /** 单次过期扫描最多删除的文件数量 */
    public static final int MAX_SWEEP_FILES = 256;

    /** 单次过期扫描的时间上限，避免缓存异常时长时间占用 I/O */
    public static final long MAX_SWEEP_DURATION_MILLIS = 2000L;

    /** 时间源，便于单元测试推进过期时间 */
    public interface TimeSource {
        long nowMillis();
    }

    private final File root;
    private final long sessionTtlMillis;
    private final TimeSource timeSource;

    private final ConcurrentHashMap<String, ShareTempSession> sessions = new ConcurrentHashMap<>();

    public ShareTempFileStore(File root) {
        this(root, DEFAULT_SESSION_TTL_MILLIS, null);
    }

    public ShareTempFileStore(File root, long sessionTtlMillis, TimeSource timeSource) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root;
        this.sessionTtlMillis = sessionTtlMillis > 0 ? sessionTtlMillis : DEFAULT_SESSION_TTL_MILLIS;
        this.timeSource = timeSource != null ? timeSource : new SystemTimeSource();
    }

    public File getRoot() {
        return root;
    }

    public long nowMillis() {
        return timeSource.nowMillis();
    }

    /** 当前仍在登记中的会话数量，供测试与自检使用 */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    public ShareTempSession getSession(String sessionId) {
        return sessionId == null ? null : sessions.get(sessionId);
    }

    /**
     * 创建一个新的分享会话及其独立目录。
     *
     * @return 创建成功的会话；目录不可用时返回 {@code null}，调用方应据此提示失败而不是继续传空路径
     */
    public ShareTempSession createSession(int channel) {
        long now = timeSource.nowMillis();
        // 会话标识使用不可预测的唯一值，不依赖毫秒时间戳，避免并发分享互相覆盖
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        File directory = new File(root, sessionId);
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }
        if (!directory.isDirectory() || !isInsideRoot(directory)) {
            return null;
        }
        ShareTempSession session =
                new ShareTempSession(sessionId, channel, directory, now, now + sessionTtlMillis);
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * 在会话目录内创建一个临时文件并完成登记。
     *
     * <p>先登记再交给目标应用，保证进程异常退出后仍能靠过期扫描找回。
     *
     * @param extension 不含点号的扩展名，必须与实际写入的图片格式一致
     * @return 已登记的空文件；创建失败返回 {@code null}
     */
    public File createTempFile(ShareTempSession session, String extension) {
        if (session == null) {
            return null;
        }
        String safeExtension = sanitizeExtension(extension);
        File file = new File(session.getDirectory(),
                UUID.randomUUID().toString().replace("-", "") + "." + safeExtension);
        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        if (!isInsideRoot(file)) {
            // 理论上不可达；一旦会话目录被替换成指向外部的符号链接则拒绝使用
            deleteQuietly(file);
            return null;
        }
        session.addFile(file);
        return file;
    }

    /** 登记本次对外授权的 content URI，删除文件后据此撤销临时授权 */
    public void registerUri(ShareTempSession session, String uri) {
        if (session != null) {
            session.addUri(uri);
        }
    }

    public void markReady(ShareTempSession session) {
        setState(session, ShareSessionState.READY);
    }

    public void markLaunched(ShareTempSession session) {
        setState(session, ShareSessionState.LAUNCHED);
    }

    public void markWaitingResult(ShareTempSession session) {
        setState(session, ShareSessionState.WAITING_RESULT);
    }

    /**
     * 进入安全延迟删除状态。
     *
     * @param delayMillis 期望的安全读取时间，会被收敛到 {@code [0, MAX_SAFE_DELETE_DELAY_MILLIS]}
     * @return 本次会话的最早删除时间；会话不存在时返回 {@code 0}
     */
    public long markPendingSafeDelete(String sessionId, long delayMillis) {
        ShareTempSession session = getSession(sessionId);
        if (session == null) {
            return 0L;
        }
        session.extendEarliestDeleteAt(timeSource.nowMillis() + clampSafeDelay(delayMillis));
        setState(session, ShareSessionState.PENDING_SAFE_DELETE);
        return session.getEarliestDeleteAtMillis();
    }

    /** 把期望的安全读取时间收敛到允许区间 */
    public static long clampSafeDelay(long delayMillis) {
        if (delayMillis < 0) {
            return 0L;
        }
        return Math.min(delayMillis, MAX_SAFE_DELETE_DELAY_MILLIS);
    }

    /**
     * 立即结束会话并删除本次登记的全部临时文件。
     *
     * <p>幂等：重复调用只在第一次真正删除，之后直接返回 {@code true}。
     *
     * @return 是否已全部删除完毕
     */
    public boolean finishSession(String sessionId) {
        ShareTempSession session = getSession(sessionId);
        if (session == null) {
            // 已经删除并移除登记，视为完成
            return true;
        }
        return deleteSession(session);
    }

    /**
     * 到达最早安全删除时间后再结束会话；未到时间则保持文件不动。
     *
     * @return 是否已完成删除
     */
    public boolean finishSessionIfSafe(String sessionId) {
        ShareTempSession session = getSession(sessionId);
        if (session == null) {
            return true;
        }
        if (!session.isSafeToDeleteAt(timeSource.nowMillis())) {
            return false;
        }
        return deleteSession(session);
    }

    /**
     * 过期扫描：清理已过期的登记会话，以及进程重启后遗留在专用目录内的孤儿会话目录。
     *
     * <p>仅遍历 {@link #getRoot()}，并受数量与时间上限约束；仍在活动中且未过期的会话不会被删除。
     *
     * @return 本次删除的文件与目录数量
     */
    public int sweepExpired() {
        long deadline = timeSource.nowMillis() + MAX_SWEEP_DURATION_MILLIS;
        int deleted = 0;

        for (ShareTempSession session : sessions.values()) {
            if (deleted >= MAX_SWEEP_FILES || timeSource.nowMillis() >= deadline) {
                return deleted;
            }
            if (session.isExpired(timeSource.nowMillis())) {
                int count = session.getFiles().size() + 1;
                if (deleteSession(session)) {
                    deleted += count;
                }
            }
        }

        File[] children = root.listFiles();
        if (children == null) {
            return deleted;
        }
        int scanned = 0;
        for (File child : children) {
            if (scanned++ >= MAX_SWEEP_DIRECTORIES) {
                break;
            }
            if (deleted >= MAX_SWEEP_FILES || timeSource.nowMillis() >= deadline) {
                break;
            }
            // 仍在登记中的会话由上面的过期判断负责，这里绝不触碰
            if (sessions.containsKey(child.getName())) {
                continue;
            }
            if (!isInsideRoot(child)) {
                continue;
            }
            long now = timeSource.nowMillis();
            if (child.isDirectory()) {
                if (now - latestModified(child) < sessionTtlMillis) {
                    continue;
                }
                deleted += deleteOrphanDirectory(child);
            } else if (child.isFile()) {
                if (now - child.lastModified() < sessionTtlMillis) {
                    continue;
                }
                if (deleteQuietly(child)) {
                    deleted++;
                }
            }
        }
        return deleted;
    }

    /**
     * 删除单个文件，删除前用规范化路径校验目标仍在专用目录内。
     *
     * @return 目标已不存在或删除成功返回 {@code true}；越界路径与删除失败返回 {@code false}
     */
    public boolean deleteRegisteredFile(File file) {
        if (!isInsideRoot(file)) {
            return false;
        }
        return deleteQuietly(file);
    }

    /** 目标是否位于专用目录内部（规范化后比较，可拦截 {@code ../} 与指向外部的符号链接） */
    public boolean isInsideRoot(File file) {
        if (file == null) {
            return false;
        }
        try {
            String rootPath = root.getCanonicalPath();
            String targetPath = file.getCanonicalPath();
            if (targetPath.equals(rootPath)) {
                // 专用目录本身不允许作为删除目标
                return false;
            }
            return targetPath.startsWith(rootPath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private void setState(ShareTempSession session, ShareSessionState state) {
        if (session != null) {
            session.setState(state);
        }
    }

    private boolean deleteSession(ShareTempSession session) {
        if (session.getState().isDeleted()) {
            return true;
        }
        boolean allDeleted = true;
        for (File file : session.getFiles()) {
            if (!deleteRegisteredFile(file)) {
                allDeleted = false;
            }
        }
        File directory = session.getDirectory();
        if (isInsideRoot(directory) && directory.exists()) {
            File[] remaining = directory.listFiles();
            if (remaining == null || remaining.length == 0) {
                if (!directory.delete()) {
                    allDeleted = false;
                }
            } else {
                // 目录内出现未登记内容时保留，交由过期扫描按孤儿目录处理
                allDeleted = false;
            }
        }
        session.setState(allDeleted ? ShareSessionState.DELETED : ShareSessionState.DELETE_FAILED);
        if (allDeleted) {
            sessions.remove(session.getSessionId());
        }
        return allDeleted;
    }

    /** 只清理一层会话目录内的普通文件；出现嵌套目录时保留，避免无界递归删除 */
    private int deleteOrphanDirectory(File directory) {
        int deleted = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return deleted;
        }
        boolean cleared = true;
        for (File file : files) {
            if (file.isFile() && deleteRegisteredFile(file)) {
                deleted++;
            } else {
                cleared = false;
            }
        }
        if (cleared && isInsideRoot(directory) && directory.delete()) {
            deleted++;
        }
        return deleted;
    }

    /** 目录及其直接子文件中最新的修改时间，避免刚写入文件的目录被误判为过期 */
    private long latestModified(File directory) {
        long latest = directory.lastModified();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                latest = Math.max(latest, file.lastModified());
            }
        }
        return latest;
    }

    private static boolean deleteQuietly(File file) {
        try {
            if (!file.exists()) {
                return true;
            }
            return file.delete();
        } catch (SecurityException e) {
            return false;
        }
    }

    private static String sanitizeExtension(String extension) {
        if (extension == null) {
            return "img";
        }
        String trimmed = extension.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }
        StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
            }
        }
        return builder.length() == 0 ? "img" : builder.toString();
    }

    private static final class SystemTimeSource implements TimeSource {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    }
}
