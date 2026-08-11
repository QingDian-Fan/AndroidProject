package com.common.share.channel;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;


import com.common.share.QQShareListener;
import com.common.share.R;
import com.common.share.ShareConfig;
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

    private static final int LAUNCH_QQ = 0;
    private static final int LAUNCH_QZONE_PUBLISH = 1;
    private static final int LAUNCH_QZONE_SHARE = 2;

    private boolean isQQZone;
    private Activity context;
    private Tencent mTencent;
    private QQShareListener listener = new QQShareListener();


    public QQChannel(boolean isQQZone, Activity context) {
        super(Channel.PACKAGE_QQ, context);
        this.isQQZone = isQQZone;
        this.context = context;
        mTencent = Tencent.createInstance(ShareConfig.QQ_APPID, context.getApplicationContext());
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

        // QQ SDK 只接受本地文件路径，按需求写入应用自有的 share_temp/<sessionId>/ 目录，
        // 不再写入公共 Pictures/qq_share 或外部存储根目录 /share
        ShareTempSession session = createSession();
        String bmpUri = writeShareImage(session, bitmap);
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
        String bmpUri = writeShareImage(session, shareBitmap);
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
            ArrayList<String> bmpUriList = new ArrayList();
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

    /**
     * 写入本次分享的临时图片。
     *
     * @return 临时文件绝对路径；任一步骤失败都会清理已创建的半成品并返回 {@code null}
     */
    private String writeShareImage(ShareTempSession session, Bitmap bitmap) {
        if (session == null) {
            return null;
        }
        File file = ShareTempFiles.writeBitmap(session, bitmap, Bitmap.CompressFormat.JPEG, 90);
        if (file == null) {
            ShareTempFiles.finishNow(session.getSessionId());
            notifyImageFailed();
            return null;
        }
        return file.getAbsolutePath();
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
