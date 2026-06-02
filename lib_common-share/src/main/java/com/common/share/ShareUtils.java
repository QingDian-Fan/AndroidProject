package com.common.share;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareUtils {

    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        if (bmp == null || bmp.isRecycled()) {
            return new byte[0];
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle && !bmp.isRecycled()) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 创建分享的图片文件
     */
    public static String createShareFile(Context context, Bitmap bitmap) {
        if (context == null || bitmap == null || bitmap.isRecycled()) {
            return "";
        }
        //将生成的Bitmap插入到手机的图片库当中，获取到图片路径
        String filePath = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, null, null);
        //转uri之前必须判空，防止保存图片失败
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }
        return filePath;
    }

    private static String getRealPathFromURI(Context context, Uri contentUri) {
        if (context == null || contentUri == null) {
            return "";
        }
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) {
                return "";
            }
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            if (!cursor.moveToFirst()) {
                return "";
            }
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    /**
     * @param bmp     获取的bitmap数据
     * @param picName 自定义的图片名
     */
    public static String saveBmp2Uri(Context context, Bitmap bmp, String picName) {
        if (context == null || bmp == null || bmp.isRecycled() || TextUtils.isEmpty(picName)) {
            return "";
        }

        //系统相册目录
        String galleryPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Camera" + File.separator;

        File galleryDir = new File(galleryPath);
        if (!galleryDir.exists() && !galleryDir.mkdirs()) {
            return "";
        }

        // 声明文件对象
        File file = new File(galleryDir, picName + ".png");
        String fileName = file.toString();

        try (FileOutputStream outStream = new FileOutputStream(fileName)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            outStream.flush();
        } catch (Exception e) {
            e.getStackTrace();
            return "";
        }
        //通知相册更新
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
        context.sendBroadcast(intent);
        return fileName;
    }


}
