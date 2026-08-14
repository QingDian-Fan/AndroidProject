package com.common.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

object NightModeManager {
    private const val PREFS_NAME = "common_theme_night_mode"
    private const val KEY_NIGHT_MODE = "night_mode"
    private const val ACTION_NIGHT_MODE_CHANGED =
        "com.common.theme.action.NIGHT_MODE_CHANGED"
    private const val EXTRA_NIGHT_MODE = "extra_night_mode"

    private var initialized = false
    private var receiverRegistered = false
    private var mode: NightMode = NightMode.FOLLOW_SYSTEM

    private val modeChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_NIGHT_MODE_CHANGED) return
            applyMode(NightMode.fromStorageKey(intent.getStringExtra(EXTRA_NIGHT_MODE)))
        }
    }

    @JvmStatic
    val currentMode: NightMode
        get() = mode

    @JvmStatic
    fun init(context: Context) {
        val appContext = context.applicationContext
        registerModeChangedReceiver(appContext)
        applyMode(readMode(appContext))
        initialized = true
    }

    @JvmStatic
    fun setMode(context: Context, nightMode: NightMode) {
        val appContext = context.applicationContext
        if (!initialized) {
            init(appContext)
        }
        if (mode == nightMode) {
            notifyModeChanged(appContext, nightMode)
            return
        }

        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NIGHT_MODE, nightMode.storageKey)
            .commit()
        applyMode(nightMode)
        notifyModeChanged(appContext, nightMode)
    }

    private fun registerModeChangedReceiver(context: Context) {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            modeChangedReceiver,
            IntentFilter(ACTION_NIGHT_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    private fun notifyModeChanged(context: Context, nightMode: NightMode) {
        val intent = Intent(ACTION_NIGHT_MODE_CHANGED)
            .setPackage(context.packageName)
            .putExtra(EXTRA_NIGHT_MODE, nightMode.storageKey)
        context.sendBroadcast(intent)
    }

    private fun applyMode(nightMode: NightMode) {
        mode = nightMode
        if (AppCompatDelegate.getDefaultNightMode() != nightMode.delegateMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode.delegateMode)
        }
    }

    private fun readMode(context: Context): NightMode {
        val storageKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NIGHT_MODE, NightMode.FOLLOW_SYSTEM.storageKey)
        return NightMode.fromStorageKey(storageKey)
    }
}
