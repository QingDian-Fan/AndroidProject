package com.common.share.channel;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;


import android.net.Uri;

import com.common.share.QQShareListener;
import com.common.share.R;
import com.common.share.ShareConfig;
import com.common.share.temp.ShareFileProviders;
import com.common.share.temp.ShareImageValidator;
import com.common.share.temp.ShareTempFiles;
import com.common.share.temp.ShareTempSession;
import com.common.utils.LogUtil;
import com.common.utils.ResourcesUtil;
import com.common.utils.ToastUtil;
import com.common.utils.Utils;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzonePublish;
import com.tencent.connect.share.QzoneShare;
import com.tencent.tauth.Tencent;

import java.io.File;
import java.util.ArrayList;

public class QQChannel extends CustomChannel {

    private static final String TAG = "QQ-Share";

    private static final int LAUNCH_QQ = 0;
    private static final int LAUNCH_QZONE_PUBLISH = 1;
    private static final int LAUNCH_QZONE_SHARE = 2;

    private boolean isQQZone;
    private Activity context;
    private Tencent mTencent;
    private QQShareListener listener = new QQShareListener();

    /** 与 Manifest ${applicationId}.provider 完全一致的运行时 authority */
    private final String fileProviderAuthority;


    public QQChannel(boolean isQQZone, Activity context) {
        super(Channel.PACKAGE_QQ, context);
        this.isQQZone = isQQZone;
        this.context = context;
        // authority 必须按运行时包名生成：debug 变体带 applicationIdSuffix，硬编码会取错
        this.fileProviderAuthority = ShareFileProviders.authorityOf(context);
        this.mTencent = createTencent();
    }

    /**
     * 使用三参数初始化 QQ OpenSDK。
     *
     * <p>只有把 FileProvider authority 交给 SDK，SDK 才能在分享时用该 authority 为本地文件
     * 生成 {@code content://} URI 并向 {@code com.tencent.mobileqq} 授予临时读权限。
     * 两参数初始化会让 SDK 只能把应用专属缓存的裸绝对路径写进分享协议，
     * QQ 作为另一个进程无法读取，表现为「图片加载失败」。
     *
     * @return Tencent 实例；authority 缺失或 SDK 初始化失败时返回 {@code null}
     */
    private Tencent createTencent() {
        if (fileProviderAuthority == null) {
            LogUtil.e(TAG, "file provider authority is null, QQ share disabled");
            return null;
        }
        try {
            return Tencent.createInstance(
                    ShareConfig.QQ_APPID,
                    context.getApplicationContext(),
                    fileProviderAuthority
            );
        } catch (Exception e) {
            LogUtil.printStackTrace(e);
            return null;
        }
    }

    @Override
    public void shareText(String text) {
        if (context == null || context.isFinishing() || TextUtils.isEmpty(text)) {
            return;
        }
        if (!isQQZone) {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, ResourcesUtil.getString(R.string.share_subject));
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity"));
            Utils.INSTANCE.getAppContext().startActivity(intent);
        } else {
            ToastUtil.showToast(Utils.INSTANCE.getAppContext(), ResourcesUtil.getString(R.string.share_text_not_supported), false, Gravity.CENTER);
        }

    }

    @Override
    public void shareBitmap(Bitmap bitmap) {
        if (context == null || context.isFinishing() || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        if (!ensureSdkReady()) {
            return;
        }

        // QQ SDK 只接受本地文件路径，按需求写入应用自有的 share_temp/<sessionId>/ 目录，
        // 不再写入公共 Pictures/qq_share 或外部存储根目录 /share
        ShareTempSession session = createSession();
        String bmpUri = writeShareImage(session, bitmap,
                isQQZone ? "qzone-image" : "qq-image");
        if (TextUtils.isEmpty(bmpUri)) {
            return;
        }

        Bundle params = new Bundle();
        if (!isQQZone) {
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, bmpUri);
            params.putString(QQShare.SHARE_TO_QQ_APP_NAME, "");
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
            launch(session, params, LAUNCH_QQ);
        } else {
            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzonePublish.PUBLISH_TO_QZONE_TYPE_PUBLISHMOOD);
            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, "");//"摘要"
            ArrayList<String> bmpUriList = new ArrayList<>();
            bmpUriList.add(bmpUri);
            params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, bmpUriList);
            Bundle extParams = new Bundle();
            params.putBundle(QzonePublish.PUBLISH_TO_QZONE_EXTMAP, extParams);
            launch(session, params, LAUNCH_QZONE_PUBLISH);
        }
    }

    @Override
    public void shareLink(String title, String des, String link, Bitmap bitmap) {
        if (context == null || context.isFinishing() || TextUtils.isEmpty(link)) {
            return;
        }
        if (!ensureSdkReady()) {
            return;
        }
        // 保存图片bitmap到本地
        Bitmap shareBitmap = bitmap;
        boolean shouldRecycleFallback = false;
        if (shareBitmap == null || shareBitmap.isRecycled()) {
            shareBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            shouldRecycleFallback = true;
        }
        if (shareBitmap == null) {
            return;
        }
        ShareTempSession session = createSession();
        String bmpUri = writeShareImage(session, shareBitmap,
                isQQZone ? "qzone-link" : "qq-link");
        if (shouldRecycleFallback && !shareBitmap.isRecycled()) {
            // 兜底图由本方法创建，写盘完成后即可回收；
            // 调用方传入的 Bitmap 生命周期不归这里管理，也不能用回收代替文件清理
            shareBitmap.recycle();
        }
        if (TextUtils.isEmpty(bmpUri)) {
            return;
        }

        Bundle params = new Bundle();
        if (!isQQZone) {
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
            params.putString(QQShare.SHARE_TO_QQ_TITLE, title);
            params.putString(QQShare.SHARE_TO_QQ_APP_NAME, ResourcesUtil.getString(R.string.app_name));
            params.putString(QQShare.SHARE_TO_QQ_SUMMARY, des);//"摘要"
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, link);
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, bmpUri);
            launch(session, params, LAUNCH_QQ);
        } else {
            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
            params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title);
            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, des);//"摘要"
            params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, link);
            ArrayList<String> bmpUriList = new ArrayList<>();
            bmpUriList.add(bmpUri);
            params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, bmpUriList);
            launch(session, params, LAUNCH_QZONE_SHARE);
        }
    }

    /** 创建本次分享的临时文件会话；QQ 只认文件路径，优先使用外部缓存目录 */
    private ShareTempSession createSession() {
        ShareTempSession session = ShareTempFiles.beginSession(
                Utils.INSTANCE.getAppContext(),
                isQQZone ? Channel.QQ_ZONE : Channel.QQ,
                true
        );
        if (session == null) {
            notifyImageFailed();
        }
        return session;
    }

    /** SDK 是否可用；不可用时直接终止分享并提示，不继续调用 SDK */
    private boolean ensureSdkReady() {
        if (mTencent != null) {
            return true;
        }
        LogUtil.e(TAG, "Tencent instance unavailable, abort share");
        notifyImageFailed();
        return false;
    }

    /**
     * 写入并校验本次分享的临时图片，同时为 QQ 预授权本次 content URI。
     *
     * <p>返回的仍然是<b>本地绝对路径</b>：{@code SHARE_TO_QQ_IMAGE_LOCAL_URL} /
     * {@code SHARE_TO_QQ_IMAGE_URL} 在 3.51.1 中会对入参执行 {@code new File(path)}、
     * 存在性与大小检查，把 {@code content://} 字符串塞进去会直接判定文件不存在。
     * URI 由已配置 authority 的 SDK 内部生成；这里额外自行生成同一 URI 并显式授权，
     * 是为了在 SDK 授权路径与设备 QQ 版本不匹配时仍然可读。
     *
     * @return 临时文件绝对路径；任一步骤失败都会清理已创建的半成品并返回 {@code null}
     */
    private String writeShareImage(ShareTempSession session, Bitmap bitmap, String shareType) {
        if (session == null) {
            return null;
        }
        File file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.JPEG, 90);
        if (file == null) {
            return abortShare(session, "write failed", shareType);
        }

        ShareImageValidator.Result result = ShareTempFiles.validateImage(
                session, file, ShareImageValidator.QQ_MAX_IMAGE_BYTES);
        if (!result.isOk()) {
            return abortShare(session, "invalid image: " + result, shareType);
        }

        // 用与 SDK 完全相同的 authority 生成 URI；失败说明 file_provider_paths.xml
        // 未覆盖临时目录，SDK 内部同样会失败，此时不应继续调起
        Uri contentUri = ShareTempFiles.shareUri(context, session, file);
        if (contentUri == null) {
            return abortShare(session, "content uri unavailable", shareType);
        }
        boolean granted = ShareTempFiles.grantRead(context, contentUri, Channel.PACKAGE_QQ);

        String localPath = file.getAbsolutePath();
        // SHARE_TO_QQ_IMAGE_URL 同时接受网络 URL 与本地路径，这里明确断言传出的是本地路径，
        // 防止后续改动误把 content:// 或 http(s):// 塞进本地路径参数
        if (!ShareImageValidator.isLocalFilePath(localPath)) {
            return abortShare(session, "not a local file path", shareType);
        }

        logDiagnostics(shareType, file, contentUri, granted);
        return localPath;
    }

    /** 结束本次会话并提示失败；日志只记录原因，不包含完整路径与 URI */
    private String abortShare(ShareTempSession session, String reason, String shareType) {
        LogUtil.e(TAG, "abort " + shareType + " share: " + reason);
        ShareTempFiles.finishNow(session.getSessionId());
        notifyImageFailed();
        return null;
    }

    /**
     * Debug 环境下输出可诊断但不敏感的信息。
     *
     * <p>不输出完整绝对路径、完整 content URI、QQ AppId 和图片内容。
     */
    private void logDiagnostics(String shareType, File file, Uri contentUri, boolean granted) {
        if (!Utils.INSTANCE.isDebug()) {
            return;
        }
        LogUtil.d(TAG, "type=" + shareType
                + ", authorityMatchesAppId=" + ShareFileProviders.matchesApplicationId(
                        fileProviderAuthority, context.getPackageName())
                + ", qqVersion=" + qqVersionName()
                + ", fileExists=" + file.exists()
                + ", fileReadable=" + file.canRead()
                + ", fileBytes=" + file.length()
                + ", ext=" + ShareImageValidator.extensionOf(file.getName())
                + ", uriScheme=" + contentUri.getScheme()
                + ", grantedToQQ=" + granted);
    }

    /** 设备上安装的 QQ 版本号；未安装或读取失败返回 unknown */
    private String qqVersionName() {
        try {
            String name = context.getPackageManager()
                    .getPackageInfo(Channel.PACKAGE_QQ, 0).versionName;
            return name == null ? "unknown" : name;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 调起 QQ / QQ 空间。
     *
     * <p>先把会话登记为「等待结果」再调起，保证 SDK 读取文件期间不会被清理；
     * 调起本身抛异常时目标应用不会读取文件，立即清理本次会话。
     */
    private void launch(ShareTempSession session, Bundle params, int launchType) {
        if (session == null) {
            return;
        }
        // QQ 与 QQ 空间共用同一条结果回调链路，统一按 Channel.QQ 登记等待中的会话
        ShareTempFiles.setPendingSession(Channel.QQ, session);
        try {
            switch (launchType) {
                case LAUNCH_QZONE_PUBLISH:
                    mTencent.publishToQzone(context, params, listener);
                    break;
                case LAUNCH_QZONE_SHARE:
                    mTencent.shareToQzone(context, params, listener);
                    break;
                default:
                    mTencent.shareToQQ(context, params, listener);
                    break;
            }
        } catch (Exception e) {
            LogUtil.printStackTrace(e);
            ShareTempFiles.finishPendingSession(Channel.QQ);
            notifyImageFailed();
        }
    }

    private void notifyImageFailed() {
        ToastUtil.showToast(Utils.INSTANCE.getAppContext(),
                ResourcesUtil.getString(R.string.share_image_failed), false, Gravity.CENTER);
    }
}
