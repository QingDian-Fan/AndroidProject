package com.dian.demo.utils.share.channel;

import android.app.Activity;
import android.graphics.Bitmap;

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
        WeiboMultiMessage message = new WeiboMultiMessage();
        TextObject textObject = new TextObject();
        textObject.text = text;
        message.textObject = textObject;
        mWBAPI.shareMessage(mActivity, message, true);
    }

    @Override
    public void shareBitmap(Bitmap bitmap) {
        WeiboMultiMessage message = new WeiboMultiMessage();
        ImageObject imageObject = new ImageObject();
        imageObject.setImageData(bitmap);
        message.imageObject = imageObject;
        mWBAPI.shareMessage(mActivity, message, true);
    }

    @Override
    public void shareLink(String title, String des, String link, Bitmap bitmap) {
        WeiboMultiMessage message = new WeiboMultiMessage();
        WebpageObject webObject = new WebpageObject();
        webObject.identify = UUID.randomUUID().toString();
        webObject.title = title;
        webObject.description = des;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, os);
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
        webObject.actionUrl = "https://weibo.com";
        webObject.defaultText = "分享网⻚";
        message.mediaObject = webObject;
        mWBAPI.shareMessage(mActivity, message, true);
    }
}
