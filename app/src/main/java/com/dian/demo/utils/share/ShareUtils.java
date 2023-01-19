package com.dian.demo.utils.share;

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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
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
        //将生成的Bitmap插入到手机的图片库当中，获取到图片路径
        String filePath = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, null, null);
        //及时回收Bitmap对象，防止OOM
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        //转uri之前必须判空，防止保存图片失败
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }
        return getRealPathFromURI(context, Uri.parse(filePath));
    }

    private static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor == null) {
                return "";
            }
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
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

        String fileName = null;
        //系统相册目录
        String galleryPath = Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Camera" + File.separator;


        // 声明文件对象
        File file = null;
        // 声明输出流
        FileOutputStream outStream = null;

        try {
            // 如果有目标文件，直接获得文件对象，否则创建一个以filename为名称的文件
            file = new File(galleryPath, picName + ".png");
            // 获得文件相对路径
            fileName = file.toString();
            // 获得输出流，如果文件中有内容，追加内容
            outStream = new FileOutputStream(fileName);
            if (null != outStream) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            }
            bmp.recycle();
        } catch (Exception e) {
            e.getStackTrace();
            return "";
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //通知相册更新
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
        context.sendBroadcast(intent);
        return fileName;
    }


}
