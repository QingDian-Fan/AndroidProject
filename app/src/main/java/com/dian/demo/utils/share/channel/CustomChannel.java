package com.dian.demo.utils.share.channel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.dian.demo.R;
import com.dian.demo.utils.share.ShareUtils;


public class CustomChannel implements Channel {
    protected String packageName;
    protected Activity context;

    public CustomChannel(String packageName, Activity context) {
        this.packageName = packageName;
        this.context = context;

    }

    @Override
    public void shareText(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.putExtra("Kdescription", context.getString(R.string.app_name));
        if (!TextUtils.isEmpty(packageName)) {
            shareIntent.setPackage(packageName);
        }
        if (shareIntent.resolveActivity(context.getPackageManager()) == null) {
            return;
        }
        context.startActivity(shareIntent);
    }

    @Override
    public void shareBitmap(Bitmap bitmap) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, ShareUtils.createShareFile(context, bitmap));
        shareIntent.setType("image/*");
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        shareIntent.putExtra("Kdescription", context.getString(R.string.app_name));

        if (!TextUtils.isEmpty(packageName)) {
            shareIntent.setPackage(packageName);
        }

        if (shareIntent.resolveActivity(context.getPackageManager()) == null) {
            return;
        }

        context.startActivity(shareIntent);
    }

    @Override
    public void shareLink(String title, String des, String link, Bitmap bitmap) {
        shareText(des+"\n"+link);
    }
}
