package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.common.theme.AppLanguage
import com.common.theme.AppLanguageManager
import com.common.theme.NightMode
import com.common.theme.NightModeManager
import com.demo.project.R

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
        setContentView(R.layout.activity_theme_settings)
        selectedNightMode = NightModeManager.currentMode
        selectedLanguage = AppLanguageManager.currentLanguage

        val themeGroup = findViewById<RadioGroup>(R.id.rg_theme_mode)
        val languageGroup = findViewById<RadioGroup>(R.id.rg_language_mode)
        val confirmButton = findViewById<Button>(R.id.btn_confirm)

        themeGroup.check(selectedNightMode.toThemeRadioId())
        languageGroup.check(selectedLanguage.toLanguageRadioId())

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedNightMode = checkedId.toNightMode()
        }
        languageGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedLanguage = checkedId.toAppLanguage()
        }
        confirmButton.setOnClickListener {
            AppLanguageManager.setLanguage(this, selectedLanguage)
            NightModeManager.setMode(this, selectedNightMode)
            finish()
        }
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
