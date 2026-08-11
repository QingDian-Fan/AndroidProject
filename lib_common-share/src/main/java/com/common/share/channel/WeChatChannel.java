package com.common.share.channel;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;

import com.common.share.R;
import com.common.share.ShareConfig;
import com.common.share.ShareUtils;
import com.common.share.temp.ShareTempFiles;
import com.common.share.temp.ShareTempSession;
import com.common.share.utils.FileShareHelper;
import com.common.utils.LogUtil;
import com.common.utils.ResourcesUtil;
import com.common.utils.ToastUtil;
import com.common.utils.Utils;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXFileObject;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;

public class WeChatChannel extends CustomChannel {
    private IWXAPI iwxapi;
    private static final int THUMB_SIZE = 150;
    private Context context;
    private boolean isTimeLine;


    public WeChatChannel(boolean isTimeLine, Activity context) {
        super(Channel.PACKAGE_WECHAT, context);
        this.isTimeLine = isTimeLine;
        iwxapi = WXAPIFactory.createWXAPI(context, ShareConfig.WX_APPID);
        iwxapi.registerApp(ShareConfig.WX_APPID);
        this.context = context;
    }

    @Override
    public void shareText(String text) {
        if (iwxapi == null || TextUtils.isEmpty(text)) {
            return;
        }
        WXTextObject textObj = new WXTextObject();
        textObj.text = text;
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        msg.description = text;
        msg.mediaTagName = ResourcesUtil.getString(R.string.app_name);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("text");
        req.message = msg;
        req.scene = isTimeLine ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(req);
    }

    @Override
    public void shareBitmap(Bitmap bitmap) {
        if (iwxapi == null || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        WXImageObject imgObj;
        // 只有大图需要落盘；小图直接以 Bitmap 传给 SDK，不产生临时文件也就无需清理
        ShareTempSession session = null;
        if (bitmap.getByteCount() > 1000000) {
            session = beginSession();
            String path = saveImageToLocal(session, bitmap);
            if (TextUtils.isEmpty(path)) {
                return;
            }
            imgObj = new WXImageObject();
            imgObj.setImagePath(path);
        } else {
            imgObj = new WXImageObject(bitmap);
        }
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, THUMB_SIZE, THUMB_SIZE, true);
        msg.thumbData = ShareUtils.bmpToByteArray(thumbBmp, true);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("img");
        req.message = msg;
        req.scene = isTimeLine ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        sendReq(req, session);
    }

    @Override
    public void shareLink(String title, String des, String link, Bitmap bitmap) {
        if (iwxapi == null || context == null || TextUtils.isEmpty(link)) {
            return;
        }
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = link;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = title;
        msg.description = des;
        Bitmap bmp;
        boolean shouldRecycleSource = false;
        if (bitmap == null || bitmap.isRecycled()) {
            bmp = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            shouldRecycleSource = true;
        } else {
            bmp = bitmap;
        }
        if (bmp == null || bmp.isRecycled()) {
            return;
        }
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
        if (shouldRecycleSource && !bmp.isRecycled()) {
            bmp.recycle();
        }
        msg.thumbData = ShareUtils.bmpToByteArray(thumbBmp, true);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = isTimeLine ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(req);
    }

    @Override
    public void shareFile(File file, String mimeType) {
        if (iwxapi == null || isTimeLine || !FileShareHelper.isReadableFile(file)) {
            return;
        }
        WXFileObject fileObject = new WXFileObject();
        fileObject.setFilePath(file.getAbsolutePath());
        WXMediaMessage message = new WXMediaMessage(fileObject);
        message.title = file.getName();
        message.description = file.getName();
        SendMessageToWX.Req request = new SendMessageToWX.Req();
        request.transaction = buildTransaction("file");
        request.message = message;
        request.scene = SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(request);
    }


    /** 创建本次微信分享的临时文件会话；低版本微信只认文件路径，优先使用外部缓存目录 */
    private ShareTempSession beginSession() {
        ShareTempSession session = ShareTempFiles.beginSession(
                Utils.INSTANCE.getAppContext(),
                isTimeLine ? Channel.WECHAT_TIMELINE : Channel.WECHAT,
                true
        );
        if (session == null) {
            notifyImageFailed();
        }
        return session;
    }

    /**
     * 把大图写入本次会话目录。
     *
     * <p>微信 7.0.13 及以上且系统 7.0 以上时返回 content URI，否则返回文件绝对路径；
     * 写盘或生成 URI 失败都会立即清理半成品，不会把空路径继续传给 SDK。
     *
     * @return 可交给微信的路径或 content URI 字符串；失败返回 {@code null}
     */
    public String saveImageToLocal(ShareTempSession session, Bitmap bmp) {
        if (session == null || bmp == null || bmp.isRecycled()) {
            return null;
        }
        File file = ShareTempFiles.writeBitmap(session, bmp, Bitmap.CompressFormat.JPEG, 90);
        if (file == null) {
            ShareTempFiles.finishNow(session.getSessionId());
            notifyImageFailed();
            return null;
        }
        if (checkVersionValid(Utils.INSTANCE.getAppContext()) && checkAndroidNotBelowN()) {
            String fileUri = getFileUri(Utils.INSTANCE.getAppContext(), session, file);
            if (TextUtils.isEmpty(fileUri)) {
                ShareTempFiles.finishNow(session.getSessionId());
                notifyImageFailed();
                return null;
            }
            return fileUri;
        }
        return file.getAbsolutePath();
    }

    /**
     * 调起微信。
     *
     * <p>先登记「等待结果」再 {@code sendReq}，保证微信读取文件期间不会被清理；
     * {@code sendReq} 返回 false 或抛异常都说明微信不会读取文件，立即清理本次会话。
     */
    private void sendReq(SendMessageToWX.Req req, ShareTempSession session) {
        if (session != null) {
            ShareTempFiles.setPendingSession(Channel.WECHAT, session);
        }
        boolean sent;
        try {
            sent = iwxapi.sendReq(req);
        } catch (Exception e) {
            LogUtil.printStackTrace(e);
            sent = false;
        }
        if (!sent && session != null) {
            ShareTempFiles.finishPendingSession(Channel.WECHAT);
            notifyImageFailed();
        }
    }

    private void notifyImageFailed() {
        ToastUtil.showToast(Utils.INSTANCE.getAppContext(),
                ResourcesUtil.getString(R.string.share_image_failed), false, Gravity.CENTER);
    }

    public boolean checkVersionValid(Context context) { // 判断微信版本是否为7.0.13及以上
        if (context == null) {
            return false;
        }
        IWXAPI api = WXAPIFactory.createWXAPI(context, ShareConfig.WX_APPID, true);
        return api.getWXAppSupportAPI() >= 0x27000D00;
    }


    public boolean checkAndroidNotBelowN() {// 判断Android版本是否7.0及以上
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
    }


    public String getFileUri(Context context, ShareTempSession session, File file) {//android7.0以上 获取文件路径
        if (context == null || session == null || file == null) {
            return null;
        }
        // 由 ShareTempFiles 统一生成并登记 content URI，删除文件后据此撤销临时授权
        Uri contentUri = ShareTempFiles.shareUri(context, session, file);
        if (contentUri == null) {
            return null;
        }
        // 只针对本次 URI 授权给微信，不做目录级或长期授权
        ShareTempFiles.grantRead(context, contentUri, Channel.PACKAGE_WECHAT);
        return contentUri.toString();   // contentUri.toString() 即是以"content://"开头的用于共享的路径
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
