package com.common.ui.skin

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.common.ui.BaseAppBindFragment

abstract class BaseSkinBindFragment<B : ViewBinding> :
    BaseAppBindFragment<B>(),
    SkinManager.Listener {

    protected var skin: Skin = Skin.BLUE
        private set

    protected var language: Language = Language.SYSTEM
        private set

    protected var nightMode: SkinNightMode = SkinNightMode.FOLLOW_SYSTEM
        private set

    private var refreshCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        skin = SkinManager.currentSkin
        language = SkinManager.currentLanguage
        nightMode = SkinManager.currentNightMode
        SkinManager.register(this)
        refreshSkinViews()
    }

    override fun onDestroyView() {
        SkinManager.unregister(this)
        super.onDestroyView()
    }

    override fun onSkinChanged(newSkin: Skin) {
        if (skin == newSkin) return
        skin = newSkin
        refreshSkinViews()
    }

    override fun onLanguageChanged(newLanguage: Language) {
        if (language == newLanguage) return
        language = newLanguage
        if (activity !is BaseSkinBindActivity<*>) {
            activity?.recreate()
        }
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
        val root = view ?: return
        val safeContext = context ?: root.context
        refreshCount++
        SkinApplier.applyToView(root, safeContext, skin, language, nightMode, refreshCount)
    }

    fun applySkinToView(view: View) {
        val safeContext = context ?: view.context
        refreshCount++
        SkinApplier.applyToView(view, this.context ?: safeContext, skin, language, nightMode, refreshCount)
    }
}