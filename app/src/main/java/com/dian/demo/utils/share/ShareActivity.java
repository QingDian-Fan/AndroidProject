package com.dian.demo.utils.share;


import static com.dian.demo.utils.share.ShareFactory.SHARE_SUCCESS_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;


import com.tencent.connect.common.Constants;
import com.tencent.tauth.Tencent;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class ShareActivity extends AppCompatActivity {
    private QQShareListener listener = new QQShareListener();
    private boolean isShare = false;
    ShareCallBack callBack;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isShare && (requestCode == Constants.REQUEST_QQ_SHARE
                || requestCode == Constants.REQUEST_QZONE_SHARE
                || requestCode == Constants.REQUEST_OLD_SHARE)) {
            Tencent.onActivityResultData(requestCode, resultCode, data, listener);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(shareReceiver, new IntentFilter(SHARE_SUCCESS_ACTION));
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
        }catch (Exception e){
            e.printStackTrace();
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