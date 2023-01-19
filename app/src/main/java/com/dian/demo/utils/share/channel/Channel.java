package com.dian.demo.utils.share.channel;

import android.graphics.Bitmap;

public interface Channel {
    int WECHAT = 1;
    int WECHAT_TIMELINE = 2;
    int QQ = 3;
    int QQ_ZONE = 4;
    int COPY_LINK = 5;
    int SAVE_LOCAL = 6;
    int WEIBO = 7;


    String PACKAGE_WECHAT = "com.tencent.mm";
    String PACKAGE_QQ = "com.tencent.mobileqq";
    String PACKAGE_THIS = "com.android.project";
    String PACKAGE_WEIBO = "com.sina.weibo";


    void shareText(String text);

    void shareBitmap(Bitmap bitmap);

    void shareLink(String title, String des, String link, Bitmap bitmap);

}
