package com.dian.demo.utils.share;

import android.graphics.Bitmap;

public class ShareModel {
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_BITMAP = 2;
    public static final int TYPE_HTML = 3;


    public String title;
    public String des;
    public String link;
    public String imgLink;
    public int type;

    public Bitmap bitmap;

    /**
     * 分享文本
     *
     * @param text
     * @return
     */
    public static ShareModel shareText(String text) {

        ShareModel shareModel = new ShareModel();
        shareModel.des = text;
        shareModel.type = TYPE_TEXT;

        return shareModel;
    }


    /**
     * 分享图片
     *
     * @return
     */
    public static ShareModel shareBitmap(Bitmap bitmap) {
        ShareModel shareModel = new ShareModel();
        shareModel.bitmap = bitmap;
        shareModel.type = TYPE_BITMAP;

        return shareModel;
    }

    public static ShareModel shareBitmap(String imgUrl) {
        ShareModel shareModel = new ShareModel();
        shareModel.imgLink = imgUrl;
        shareModel.type = TYPE_BITMAP;

        return shareModel;
    }


    /**
     * 分享html
     *
     * @return
     */
    public static ShareModel shareUrl(String title, String des, String link, String imgLink) {

        ShareModel shareModel = new ShareModel();
        shareModel.type = TYPE_HTML;
        shareModel.title = title;
        shareModel.des = des;
        shareModel.link = link;
        shareModel.imgLink = imgLink;
        return shareModel;
    }

    public static ShareModel shareUrl(String title, String des, String link, Bitmap bitmap) {

        ShareModel shareModel = new ShareModel();
        shareModel.type = TYPE_HTML;
        shareModel.title = title;
        shareModel.des = des;
        shareModel.link = link;
        shareModel.bitmap = bitmap;

        return shareModel;
    }

}
