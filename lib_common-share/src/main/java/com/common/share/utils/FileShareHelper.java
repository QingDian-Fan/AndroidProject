package com.common.share.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;

public class FileShareHelper {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private FileShareHelper() {
    }

    public static boolean isReadableFile(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }

    public static Uri getShareUri(Context context, File file) {
        if (context == null || !isReadableFile(file)) {
            return null;
        }
        try {
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
            );
        } catch (IllegalArgumentException e) {
            com.common.utils.LogUtil.printStackTrace(e);
            return null;
        }
    }

    public static String resolveMimeType(File file, String mimeType) {
        if (!TextUtils.isEmpty(mimeType)) {
            return mimeType;
        }
        if (file == null) {
            return DEFAULT_MIME_TYPE;
        }
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        if (TextUtils.isEmpty(extension)) {
            return DEFAULT_MIME_TYPE;
        }
        String resolvedType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.toLowerCase(java.util.Locale.ROOT));
        return TextUtils.isEmpty(resolvedType) ? DEFAULT_MIME_TYPE : resolvedType;
    }

    // 分享临时图片的落盘与清理统一由 com.common.share.temp.ShareTempFiles 负责：
    // 原 getBitmapPath() 会在 Android 10+ 写入公共 Pictures/qq_share、在更低版本写入
    // 外部存储根目录 /share，两者都无法按会话精准清理，已按需求移除。
}
