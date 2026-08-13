package com.common.share.temp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * 交给三方 SDK 之前对分享临时图片做的一致性校验。
 *
 * <p>QQ OpenSDK 会对本地路径执行 {@code new File(path)}、存在性检查和大小检查，
 * 任何一项不满足都会在 QQ 侧表现为「图片加载失败」，而且 SDK 不一定给出可区分的回调。
 * 因此在调起之前先自查，把问题变成可诊断的失败提示。
 *
 * <p>格式校验只读文件头几个字节，不解码图片，也不把文件内容写进日志。
 */
public final class ShareImageValidator {

    /** QQ / QQ 空间对本地分享图片的大小上限（5MB） */
    public static final long QQ_MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    /** 读取文件头需要的最大字节数（PNG 签名 8 字节） */
    private static final int HEADER_LENGTH = 8;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    public enum Result {
        /** 校验通过，可以交给 SDK */
        OK,
        /** 文件不存在 */
        MISSING,
        /** 目标不是普通文件 */
        NOT_A_FILE,
        /** 文件长度为 0，压缩或写入实际失败 */
        EMPTY,
        /** 超过目标 SDK 的大小限制 */
        TOO_LARGE,
        /** 不在本次会话的专用目录内 */
        OUTSIDE_SESSION,
        /** 扩展名与实际图片格式不一致 */
        FORMAT_MISMATCH,
        /** 参数缺失 */
        INVALID_ARGUMENT;

        public boolean isOk() {
            return this == OK;
        }
    }

    private ShareImageValidator() {
    }

    /**
     * 完整校验一张待分享的临时图片。
     *
     * @param maxBytes 目标 SDK 的大小上限，传 {@code <= 0} 表示不限制
     */
    public static Result validate(ShareTempFileStore store, ShareTempSession session,
                                  File file, long maxBytes) {
        if (store == null || session == null || file == null) {
            return Result.INVALID_ARGUMENT;
        }
        if (!file.exists()) {
            return Result.MISSING;
        }
        if (!file.isFile()) {
            return Result.NOT_A_FILE;
        }
        long length = file.length();
        if (length <= 0) {
            return Result.EMPTY;
        }
        if (maxBytes > 0 && length > maxBytes) {
            return Result.TOO_LARGE;
        }
        // 必须是本次会话登记过、且仍位于专用目录内的文件，避免把别的任务或用户原图交出去
        if (!store.isInsideRoot(file) || !isRegisteredIn(session, file)) {
            return Result.OUTSIDE_SESSION;
        }
        if (!matchesExtension(extensionOf(file.getName()), readHeader(file))) {
            return Result.FORMAT_MISMATCH;
        }
        return Result.OK;
    }

    /**
     * 扩展名与实际图片格式是否一致（按文件头魔数判断）。
     *
     * <p>纯函数，便于单元测试；未知扩展名一律视为不一致，避免把 MIME 写错的文件发出去。
     *
     * @param extension 不含点号的小写扩展名
     * @param header    文件头字节，长度不足时返回 false
     */
    public static boolean matchesExtension(String extension, byte[] header) {
        if (extension == null || header == null) {
            return false;
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        if ("jpg".equals(normalized) || "jpeg".equals(normalized)) {
            return header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
        }
        if ("png".equals(normalized)) {
            if (header.length < PNG_SIGNATURE.length) {
                return false;
            }
            for (int i = 0; i < PNG_SIGNATURE.length; i++) {
                if (header[i] != PNG_SIGNATURE[i]) {
                    return false;
                }
            }
            return true;
        }
        if ("webp".equals(normalized)) {
            // RIFF....WEBP，前 4 字节即可区分
            return header.length >= 4
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F';
        }
        return false;
    }

    /** 取不含点号的小写扩展名；没有扩展名时返回空串 */
    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 待分享的路径是否是本地文件路径（而不是网络 URL 或 content URI） */
    public static boolean isLocalFilePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        String lower = path.trim().toLowerCase(Locale.ROOT);
        return !lower.startsWith("http://")
                && !lower.startsWith("https://")
                && !lower.startsWith("content://")
                && !lower.startsWith("file://");
    }

    private static boolean isRegisteredIn(ShareTempSession session, File file) {
        for (File registered : session.getFiles()) {
            if (registered.equals(file)) {
                return true;
            }
        }
        return false;
    }

    /** 只读文件头，失败返回空数组；不抛异常，不记录文件内容 */
    private static byte[] readHeader(File file) {
        byte[] buffer = new byte[HEADER_LENGTH];
        int read = 0;
        try (FileInputStream in = new FileInputStream(file)) {
            int count;
            while (read < buffer.length && (count = in.read(buffer, read, buffer.length - read)) > 0) {
                read += count;
            }
        } catch (IOException | SecurityException e) {
            return new byte[0];
        }
        if (read == buffer.length) {
            return buffer;
        }
        byte[] shorter = new byte[read];
        System.arraycopy(buffer, 0, shorter, 0, read);
        return shorter;
    }
}
