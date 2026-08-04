package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.common.theme.AppLanguage
import com.common.theme.AppLanguageManager
import com.common.theme.NightMode
import com.common.theme.NightModeManager
import com.common.weight.titlebar.StatusBarUtils
import com.demo.project.R
import com.demo.project.databinding.ActivityThemeSettingsBinding

class ThemeSettingsActivity : AppCompatActivity() {

    private var selectedNightMode = NightMode.FOLLOW_SYSTEM
    private var selectedLanguage = AppLanguage.FOLLOW_SYSTEM

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, ThemeSettingsActivity::class.java).apply {
                if (context !is Activity) {
                    flags = FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityThemeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpImmersionStatusBar(binding.root)
        selectedNightMode = NightModeManager.currentMode
        selectedLanguage = AppLanguageManager.currentLanguage

        binding.rgThemeMode.check(selectedNightMode.toThemeRadioId())
        binding.rgLanguageMode.check(selectedLanguage.toLanguageRadioId())

        binding.ivBack.setOnClickListener { finish() }
        binding.rgThemeMode.setOnCheckedChangeListener { _, checkedId ->
            selectedNightMode = checkedId.toNightMode()
        }
        binding.rgLanguageMode.setOnCheckedChangeListener { _, checkedId ->
            selectedLanguage = checkedId.toAppLanguage()
        }
        binding.btnConfirm.setOnClickListener {
            AppLanguageManager.setLanguage(this, selectedLanguage)
            NightModeManager.setMode(this, selectedNightMode)
            finish()
        }
    }

    /**
     * 沉浸式状态栏。本页自绘标题栏，未使用 CommonTitleBar，这里沿用其 autoStatusBarMode 的处理方式：
     * 状态栏透明并让根布局延伸到状态栏下方，由根布局的 bg_default 背景填充状态栏区域保证同色，
     * 再按日夜间模式切换状态栏图标明暗。
     */
    private fun setUpImmersionStatusBar(rootView: View) {
        StatusBarUtils.transparentStatusBar(window)
        if (isNightMode()) {
            StatusBarUtils.setLightMode(window)
        } else {
            StatusBarUtils.setDarkMode(window)
        }
        // 用状态栏高度顶开内容，避免标题栏被状态栏遮挡；直接赋值而非累加，重复调用也不会叠加
        rootView.setPadding(
            rootView.paddingLeft,
            StatusBarUtils.getStatusBarHeight(this),
            rootView.paddingRight,
            rootView.paddingBottom
        )
    }

    private fun isNightMode(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun NightMode.toThemeRadioId(): Int {
        return when (this) {
            NightMode.FOLLOW_SYSTEM -> R.id.rb_theme_follow_system
            NightMode.DAY -> R.id.rb_theme_day
            NightMode.NIGHT -> R.id.rb_theme_night
        }
    }

    private fun Int.toNightMode(): NightMode {
        return when (this) {
            R.id.rb_theme_day -> NightMode.DAY
            R.id.rb_theme_night -> NightMode.NIGHT
            else -> NightMode.FOLLOW_SYSTEM
        }
    }

    private fun AppLanguage.toLanguageRadioId(): Int {
        return when (this) {
            AppLanguage.FOLLOW_SYSTEM -> R.id.rb_language_follow_system
            AppLanguage.CHINESE -> R.id.rb_language_chinese
            AppLanguage.ENGLISH -> R.id.rb_language_english
        }
    }

    private fun Int.toAppLanguage(): AppLanguage {
        return when (this) {
            R.id.rb_language_chinese -> AppLanguage.CHINESE
            R.id.rb_language_english -> AppLanguage.ENGLISH
            else -> AppLanguage.FOLLOW_SYSTEM
        }
    }
}
