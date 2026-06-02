package com.demo.project.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.view.Gravity;
import java.util.Objects;
import com.common.utils.NetWorkUtil;
import com.common.utils.ToastUtil;

/**
 * 监听网络状态的广播接收者
 * 例如：可以收到 “网络不给力”
 */
public class NetworkStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (!NetWorkUtil.isNetworkAvailable()) {
                ToastUtil.showToast(context,"网络不给力",false, Gravity.CENTER);
            }
        }
    }
}

