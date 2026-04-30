package com.common.share.channel;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.common.share.R;
import com.common.share.ShareUtils;


public class CustomChannel implements Channel {
    protected String packageName;
    protected Activity context;

    public CustomChannel(String packageName, Activity context) {
        this.packageName = packageName;
        this.context = context;

    }

    @Override
    public void shareText(String text) {
        if (context == null || context.isFinishing() || TextUtils.isEmpty(text)) {
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.putExtra("description", context.getString(R.string.app_name));
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
        if (context == null || context.isFinishing() || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        String shareFile = ShareUtils.createShareFile(context, bitmap);
        if (TextUtils.isEmpty(shareFile)) {
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(shareFile));
        shareIntent.setType("image/*");
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putExtra("description", context.getString(R.string.app_name));

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
        if (TextUtils.isEmpty(link)) {
            return;
        }
        shareText(des+"\n"+link);
    }
}
