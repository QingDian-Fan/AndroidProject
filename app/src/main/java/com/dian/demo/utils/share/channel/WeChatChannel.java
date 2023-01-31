package com.dian.demo.utils.share.channel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import com.dian.demo.BuildConfig;
import com.dian.demo.ProjectApplication;
import com.dian.demo.R;
import com.dian.demo.config.AppConfig;
import com.dian.demo.utils.ResourcesUtil;
import com.dian.demo.utils.share.ShareUtils;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WeChatChannel extends CustomChannel {
    private IWXAPI iwxapi;
    private static final int THUMB_SIZE = 150;
    private Context context;
    private boolean isTimeLine;


    public WeChatChannel(boolean isTimeLine, Activity context) {
        super(Channel.PACKAGE_WECHAT, context);
        this.isTimeLine = isTimeLine;
        iwxapi = WXAPIFactory.createWXAPI(context, AppConfig.WX_APPID);
        iwxapi.registerApp(AppConfig.WX_APPID);
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
        if (iwxapi == null || bitmap == null) {
            return;
        }
        WXImageObject imgObj;
        String path;
        if (bitmap.getByteCount() > 1000000) {
            path = saveImageToLocal(bitmap);
            imgObj = new WXImageObject();
            imgObj.setImagePath(path);
        } else {
            imgObj = new WXImageObject(bitmap);
        }
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bitmap, THUMB_SIZE, THUMB_SIZE, true);
        bitmap.recycle();
        msg.thumbData = ShareUtils.bmpToByteArray(thumbBmp, true);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("img");
        req.message = msg;
        req.scene = isTimeLine ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(req);
    }

    @Override
    public void shareLink(String title, String des, String link, Bitmap bitmap) {
        if (iwxapi == null) {
            return;
        }
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = link;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = title;
        msg.description = des;
        Bitmap bmp;
        if (bitmap == null) {
            bmp = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        } else {
            bmp = bitmap;
        }
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
        bmp.recycle();
        msg.thumbData = ShareUtils.bmpToByteArray(thumbBmp, true);
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = isTimeLine ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        iwxapi.sendReq(req);
    }


    public String saveImageToLocal(Bitmap bmp) {
        String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "share";// 首先保存图片
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);//通过io流的方式来压缩保存图片
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (checkVersionValid(ProjectApplication.getAppContext()) && checkAndroidNotBelowN()) {
            String fileUri = getFileUri(ProjectApplication.getAppContext(), file);
            return fileUri;
        }
        return storePath + File.separator + fileName;
    }

    public boolean checkVersionValid(Context context) { // 判断微信版本是否为7.0.13及以上
        IWXAPI api = WXAPIFactory.createWXAPI(context, AppConfig.WX_APPID, true);
        return api.getWXAppSupportAPI() >= 0x27000D00;
    }


    public boolean checkAndroidNotBelowN() {// 判断Android版本是否7.0及以上
        return android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.N;
    }


    public String getFileUri(Context context, File file) {//android7.0以上 获取文件路径
        if (file == null) {
            return null;
        }
        Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        // 授权给微信访问路径
        context.grantUriPermission("com.tencent.mm", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);// 这里填微信包名
        return contentUri.toString();   // contentUri.toString() 即是以"content://"开头的用于共享的路径
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
