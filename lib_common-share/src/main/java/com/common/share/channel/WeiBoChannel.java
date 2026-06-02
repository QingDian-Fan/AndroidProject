package com.common.share.channel;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.common.share.R;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.openapi.IWBAPI;
import com.sina.weibo.sdk.openapi.WBAPIFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;


public class WeiBoChannel extends CustomChannel {

    private final IWBAPI mWBAPI;
    private Activity mActivity;

    public WeiBoChannel(Activity mContext) {
        super(Channel.PACKAGE_WEIBO, mContext);
        mWBAPI = WBAPIFactory.createWBAPI(mContext);
        mActivity=mContext;
    }

    @Override
    public void shareText(String text) {
        if (mWBAPI == null || mActivity == null || mActivity.isFinishing()) {
            return;
        }
        WeiboMultiMessage message = new WeiboMultiMessage();
        TextObject textObject = new TextObject();
        textObject.text = text;
        message.textObject = textObject;
        mWBAPI.shareMessage(mActivity, message, true);
    }

    @Override
    public void shareBitmap(Bitmap bitmap) {
        if (mWBAPI == null || mActivity == null || mActivity.isFinishing() || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        WeiboMultiMessage message = new WeiboMultiMessage();
        ImageObject imageObject = new ImageObject();
        imageObject.setImageData(bitmap);
        message.imageObject = imageObject;
        mWBAPI.shareMessage(mActivity, message, true);
    }

    @Override
    public void shareLink(String title, String des, String link, Bitmap bitmap) {
        if (mWBAPI == null || mActivity == null || mActivity.isFinishing()) {
            return;
        }
        WeiboMultiMessage message = new WeiboMultiMessage();
        WebpageObject webObject = new WebpageObject();
        webObject.identify = UUID.randomUUID().toString();
        webObject.title = title;
        webObject.description = des;
        Bitmap thumbBitmap = bitmap != null && !bitmap.isRecycled() ? bitmap : BitmapFactory.decodeResource(mActivity.getResources(), R.mipmap.ic_launcher);
        if (thumbBitmap == null || thumbBitmap.isRecycled()) {
            return;
        }
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, os);
            webObject.thumbData = os.toByteArray();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (os != null) {
                    os.close(); }
            } catch (IOException e) { e.printStackTrace();
            }
        }
        webObject.actionUrl = link;
        webObject.defaultText = "分享网⻚";
        message.mediaObject = webObject;
        mWBAPI.shareMessage(mActivity, message, true);
    }
}
