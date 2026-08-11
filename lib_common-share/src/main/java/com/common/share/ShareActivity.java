package com.common.share;


import static com.common.share.ShareFactory.SHARE_SUCCESS_ACTION;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;


import com.common.share.temp.ShareTempFiles;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.Tencent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.concurrent.atomic.AtomicBoolean;


public class ShareActivity extends AppCompatActivity {

    /** 进程内只在首个页面创建时触发一次启动期过期扫描，避免每个 Activity 都扫一遍 */
    private static final AtomicBoolean START_UP_SWEPT = new AtomicBoolean(false);

    private final QQShareListener listener = new QQShareListener();
    private boolean isShare = false;
    ShareCallBack callBack;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isShare && (requestCode == Constants.REQUEST_QQ_SHARE
                || requestCode == Constants.REQUEST_QZONE_SHARE
                || requestCode == Constants.REQUEST_OLD_SHARE)) {
            // 结果分发到 QQShareListener，由它按会话标识精准清理本次临时文件；
            // 不再删除外部存储根目录下的整个 /share 目录
            Tencent.onActivityResultData(requestCode, resultCode, data, listener);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this,shareReceiver, new IntentFilter(SHARE_SUCCESS_ACTION), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(shareReceiver, new IntentFilter(SHARE_SUCCESS_ACTION));
        }
        if (START_UP_SWEPT.compareAndSet(false, true)) {
            // 兜底上次进程被杀、回调丢失遗留的过期临时文件；扫描在后台线程执行
            ShareTempFiles.sweepAsync(getApplicationContext());
        }
    }

    private BroadcastReceiver shareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int channel = intent.getIntExtra(ShareFactory.SUCCESS_CHANNEL, -1);
            onShareSuccess(channel);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (shareReceiver != null) {
                unregisterReceiver(shareReceiver);
            }
        } catch (Exception e) {
            com.common.utils.LogUtil.printStackTrace(e);
        }
    }

    private void onShareSuccess(int channel) {
        if (callBack != null) {
            callBack.shareCallBack(channel);
        }
    }

    public void setShareCallBack(ShareCallBack shareCallBack) {
        this.callBack = shareCallBack;
    }

    public void initShare() {
        isShare = true;
    }

}
