package com.dian.demo.utils.share.channel;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;


import com.dian.demo.ProjectApplication;
import com.dian.demo.R;
import com.dian.demo.config.AppConfig;
import com.dian.demo.utils.ResourcesUtils;
import com.dian.demo.utils.ToastUtils;
import com.dian.demo.utils.share.QQShareListener;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzonePublish;
import com.tencent.connect.share.QzoneShare;
import com.tencent.tauth.Tencent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class QQChannel extends CustomChannel {
    private boolean isQQZone;
    private Activity context;
    private Tencent mTencent;
    private QQShareListener listener = new QQShareListener();


    public QQChannel(boolean isQQZone, Activity context) {
        super(Channel.PACKAGE_QQ, context);
        this.isQQZone = isQQZone;
        this.context = context;
        mTencent = Tencent.createInstance(AppConfig.QQ_APPID, context.getApplicationContext());
    }

    @Override
    public void shareText(String text) {
        if (!isQQZone) {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "分享");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.tencent.mobileqq", "com.tencent.mobileqq.activity.JumpActivity"));
            ProjectApplication.getAppContext().startActivity(intent);
        } else {
            ToastUtils.showToast(ProjectApplication.getAppContext(),"暂不支持分享纯文本",false, Gravity.CENTER);
        }

    }

    @Override
    public void shareBitmap(Bitmap bitmap) {

        Bundle params = new Bundle();
        // 保存图片bitmap到本地
        String bmpUri = saveImageToLocal(bitmap);
        if (!isQQZone) {
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, bmpUri);
            params.putString(QQShare.SHARE_TO_QQ_APP_NAME, "");
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
            mTencent.shareToQQ(context, params, listener);
        } else {
            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzonePublish.PUBLISH_TO_QZONE_TYPE_PUBLISHMOOD);
            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, "");//"摘要"
            ArrayList<String> bmpUriList = new ArrayList<>();
            bmpUriList.add(bmpUri);
            params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, bmpUriList);
            Bundle extParams = new Bundle();
            params.putBundle(QzonePublish.PUBLISH_TO_QZONE_EXTMAP, extParams);
            mTencent.publishToQzone(context, params, listener);
        }
    }

    @Override
    public void shareLink(String title, String des, String link, Bitmap bitmap) {
        Bundle params = new Bundle();
        // 保存图片bitmap到本地
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        }
        String bmpUri = saveImageToLocal(bitmap);
        if (!isQQZone) {
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
            params.putString(QQShare.SHARE_TO_QQ_TITLE, title);
            params.putString(QQShare.SHARE_TO_QQ_APP_NAME, ResourcesUtils.getString(R.string.app_name));
            params.putString(QQShare.SHARE_TO_QQ_SUMMARY, des);//"摘要"
            params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, link);
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, bmpUri);
            mTencent.shareToQQ(context, params, listener);
        } else {
            params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
            params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title);
            params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, des);//"摘要"
            params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, link);
            ArrayList<String> bmpUriList = new ArrayList();
            bmpUriList.add(bmpUri);
            params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, bmpUriList);
            mTencent.shareToQzone(context, params, listener);
        }
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
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, fos);//通过io流的方式来压缩保存图片
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return storePath + File.separator + fileName;
    }


}
