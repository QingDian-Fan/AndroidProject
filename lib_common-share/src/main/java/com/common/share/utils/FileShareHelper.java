package com.common.share.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileShareHelper {


    public static  String getBitmapPath(Context context, Bitmap bitmap){
        if (context == null || bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveBitmap(context,bitmap);
        }else {
            return saveImageToLocal(bitmap);
        }
    }
    private static String saveBitmap(Context context, Bitmap bitmap) {
        if (context == null || bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        String fileName = "share_" + System.currentTimeMillis() + ".jpg";
        OutputStream os = null;

        try {
            // 1. 保存到 MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/qq_share"); // 公共目录
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri == null) return null;

            os = context.getContentResolver().openOutputStream(uri);
            if (os != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
                os.flush();
                os.close();
            }

            // 2. 标记写入完成
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(uri, values, null, null);

            // 3. 拼接真实路径（QQ SDK 使用）
            String realPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Pictures/qq_share/" + fileName;

            return realPath;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { if (os != null) os.close(); } catch (Exception ignored) {}
        }
    }

    private static String saveImageToLocal(Bitmap bmp) {
        if (bmp == null || bmp.isRecycled()) {
            return null;
        }
        String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "share";// 首先保存图片
        File appDir = new File(storePath);
        if (!appDir.exists() && !appDir.mkdirs()) {
            return null;
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, fos);//通过io流的方式来压缩保存图片
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return storePath + File.separator + fileName;
    }
}
