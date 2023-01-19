package com.dian.demo.utils.share.dialog;

import android.graphics.drawable.Drawable;

public class PlatformData {
    public int id;
    public String name;
    public String packageString;
    public Drawable bitmap;
    public int shareChannel;

    public PlatformData(int id, String name, String packageString, Drawable bitmap,int shareChannel) {
        this.id = id;
        this.name = name;
        this.packageString = packageString;
        this.bitmap = bitmap;
        this.shareChannel = shareChannel;
    }
}
