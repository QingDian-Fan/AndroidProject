package com.dian.demo.utils;

import android.content.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WriteLog {
    private FileOutputStream fos;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private final StringBuilder stringBuilder = new StringBuilder();

    private boolean isIsDebug() {
        return true;
    }

    private WriteLog() {
    }

    private static class InnerClass {
        private static final WriteLog INSTANCE = new WriteLog();
    }

    public static WriteLog getInstance() {
        return InnerClass.INSTANCE;
    }

    public void openStream(Context context) {
        if (!isIsDebug()) {
            return;
        }
        try {
            String logPath = context.getFilesDir().getAbsolutePath() + File.separator + "log.txt";
            LogUtil.e("LogPath:" + logPath);
            fos = new FileOutputStream(logPath, true);
        } catch (FileNotFoundException e) {
            LogUtil.e("WriteLog::error:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void writeLogs(String logString) {
        if (!isIsDebug()) {
            return;
        }
        try {
            stringBuilder.setLength(0);
            stringBuilder.append(simpleDateFormat.format(new Date(System.currentTimeMillis())));
            stringBuilder.append("    ");
            stringBuilder.append(logString);
            fos.write(stringBuilder.toString().getBytes());
            fos.write("\r\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeStream() {
        if (!isIsDebug()) {
            return;
        }
        // 释放资源
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
