package com.dian.demo.skin

import android.os.Bundle
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.SkinAppCompatDelegateImpl
import com.dian.demo.utils.mode.UIModeListener
import com.dian.demo.utils.mode.UIModeManager


open class SkinActivity: AppCompatActivity() , UIModeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UIModeManager.getInstance().registerUIModeListener(this)
    }
    override fun getDelegate(): AppCompatDelegate {
        return SkinAppCompatDelegateImpl.get(this, this)
    }

    override fun uiModeChanged(isLight: Boolean) {

    }

    override fun onDestroy() {
        super.onDestroy()
        UIModeManager.getInstance().unRegisterUIModeListener(this)
    }
}