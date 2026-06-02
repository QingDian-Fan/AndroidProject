package com.common.ui.skin

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.common.ui.BaseAppBindActivity

abstract class BaseSkinBindActivity<B : ViewBinding> :
    BaseAppBindActivity<B>(),
    SkinManager.Listener {

    protected var skin: Skin = Skin.BLUE
        private set

    protected var language: Language = Language.SYSTEM
        private set

    protected var nightMode: SkinNightMode = SkinNightMode.FOLLOW_SYSTEM
        private set

    private var refreshCount = 0

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SkinManager.applyConfiguration(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        skin = SkinManager.currentSkin
        language = SkinManager.currentLanguage
        nightMode = SkinManager.currentNightMode
        setTheme(skin.themeRes)
        super.onCreate(savedInstanceState)
        SkinManager.register(this)
        refreshSkinViews()
    }

    override fun onDestroy() {
        SkinManager.unregister(this)
        super.onDestroy()
    }

    override fun onSkinChanged(newSkin: Skin) {
        if (skin == newSkin) return
        skin = newSkin
        setTheme(newSkin.themeRes)
        refreshSkinViews()
    }

    override fun onLanguageChanged(newLanguage: Language) {
        if (language == newLanguage) return
        language = newLanguage
        recreate()
    }

    override fun onNightModeChanged(newMode: SkinNightMode) {
        if (nightMode == newMode) return
        nightMode = newMode
        refreshSkinViews()
    }

    protected fun switchSkin(newSkin: Skin) {
        SkinManager.switchSkin(newSkin)
    }

    protected fun switchLanguage(newLanguage: Language) {
        SkinManager.switchLanguage(newLanguage)
    }

    protected fun switchNightMode(newMode: SkinNightMode) {
        SkinManager.switchNightMode(newMode)
    }

    protected fun refreshSkinViews() {
        refreshCount++
        SkinApplier.applyWindow(window, this, skin)
        SkinApplier.applyToView(window.decorView, this, skin, language, nightMode, refreshCount)
    }

    fun applySkinToView(view: View) {
        refreshCount++
        SkinApplier.applyToView(view, this, skin, language, nightMode, refreshCount)
    }
}