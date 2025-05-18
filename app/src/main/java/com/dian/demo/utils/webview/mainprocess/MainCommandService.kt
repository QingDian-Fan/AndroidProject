package com.dian.demo.utils.webview.mainprocess

import android.app.Service
import android.content.Intent
import android.os.IBinder


class MainCommandService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        return MainCommandsManager.instance
    }
}