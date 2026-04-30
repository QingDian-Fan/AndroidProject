package com.common.utils;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;


import com.common.theme.BaseApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * 抓错误日志
 */
public class ExceptionHandlerUtil implements UncaughtExceptionHandler {


    private static String LOG_PATH_SDCARD_DIR;

    private static final String LOG_NAME = "crash.txt";

    private static final long MAX_SIZE = 1024 * 1024 * 10;

    private ExceptionHandlerUtil() {
    }


    public static void init(Context mContext) {
        LOG_PATH_SDCARD_DIR = mContext.getFilesDir().getPath() + File.separator + "crash";
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandlerUtil());
    }


    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        handException(ex);
    }

    /**
     * 处理错误信息
     *
     * @param ex
     */
    private void handException(Throwable ex) {
        ex.printStackTrace();
        saveCrashToFile(ex);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Process.killProcess(Process.myPid());
        }
    }


    private void createDir() {
        File dir = new File(LOG_PATH_SDCARD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

  //  @CheckPermissions(value = Manifest.permission.WRITE_EXTERNAL_STORAGE, isMust = true)
    private void saveCrashToFile(final Throwable ex) {
        createDir();
        FileOutputStream fileOutputStream;
        File crashFile = new File(LOG_PATH_SDCARD_DIR, LOG_NAME);
        try {
            if (crashFile.exists() && crashFile.length() > MAX_SIZE) {
                crashFile.delete();
                crashFile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(crashFile, true);
            PrintWriter printWriter = new PrintWriter(fileOutputStream);
            ex.printStackTrace(printWriter);
            printWriter.append(DateFormatUtil.getShareDate());
            printWriter.append("\n\n");
            printWriter.close();
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void doShareExceptionFile() {
        File file = new File(LOG_PATH_SDCARD_DIR, LOG_NAME);
        if (!file.exists()) {
            ToastUtil.showToast(Utils.INSTANCE.getAppContext(), "木有找到日志文件", false, Gravity.CENTER);
            return;
        }
        //Uri logUri = Uri.parse(file.getAbsolutePath());
        Uri logUri = FileProvider.getUriForFile(Utils.INSTANCE.getAppInstance(),
                Utils.INSTANCE.getApplicationId() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("subject", "DomeProject日志"); //
        intent.putExtra(Intent.EXTRA_STREAM, logUri); // 添加附件，附件为file对象
        intent.setType("text/plain"); // 纯文本则用text/plain的mime
        Utils.INSTANCE.getAppContext().startActivity(intent);
    }


    public static String getExceptionFilePath() {
        return LOG_PATH_SDCARD_DIR;
    }


}

