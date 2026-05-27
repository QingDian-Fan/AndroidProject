package com.common.ui.skin

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.common.ui.skin.SkinConfig.KEY_LANGUAGE
import com.common.ui.skin.SkinConfig.KEY_NIGHT_MODE
import com.common.ui.skin.SkinConfig.KEY_SKIN
import java.util.Collections
import java.util.Locale
import java.util.WeakHashMap

object SkinManager {

    interface Listener {
        fun onSkinChanged(newSkin: Skin) = Unit
        fun onLanguageChanged(newLanguage: Language) = Unit
        fun onNightModeChanged(newMode: SkinNightMode) = Unit
    }

    private val listeners = Collections.synchronizedSet(
        Collections.newSetFromMap(WeakHashMap<Listener, Boolean>())
    )

    private var cachedSkin: Skin? = null
    private var cachedLanguage: Language? = null
    private var cachedNightMode: SkinNightMode? = null

    val currentSkin: Skin
        get() = cachedSkin ?: readSkin().also { cachedSkin = it }

    val currentLanguage: Language
        get() = cachedLanguage ?: readLanguage().also { cachedLanguage = it }

    val currentNightMode: SkinNightMode
        get() = cachedNightMode ?: readNightMode().also { cachedNightMode = it }

    fun register(listener: Listener) {
        listeners.add(listener)
    }

    fun unregister(listener: Listener) {
        listeners.remove(listener)
    }

    fun switchSkin(newSkin: Skin): Boolean {
        if (currentSkin == newSkin) return false
        cachedSkin = newSkin
        ThemePreferenceUtil.putString(KEY_SKIN, newSkin.key)
        notifySkinChanged(newSkin)
        return true
    }

    fun switchLanguage(newLanguage: Language): Boolean {
        if (currentLanguage == newLanguage) return false
        cachedLanguage = newLanguage
        ThemePreferenceUtil.putString(KEY_LANGUAGE, newLanguage.key)
        notifyLanguageChanged(newLanguage)
        return true
    }

    fun switchNightMode(newMode: SkinNightMode): Boolean {
        if (currentNightMode == newMode) return false
        cachedNightMode = newMode
        ThemePreferenceUtil.putString(KEY_NIGHT_MODE, newMode.key)
        notifyNightModeChanged(newMode)
        return true
    }

    fun syncFromStorage(notify: Boolean = false) {
        val oldSkin = cachedSkin
        val oldLanguage = cachedLanguage
        val oldNightMode = cachedNightMode
        val savedSkin = readSkin()
        val savedLanguage = readLanguage()
        val savedNightMode = readNightMode()

        cachedSkin = savedSkin
        cachedLanguage = savedLanguage
        cachedNightMode = savedNightMode

        if (!notify) return
        if (oldSkin != null && oldSkin != savedSkin) {
            notifySkinChanged(savedSkin)
        }
        if (oldLanguage != null && oldLanguage != savedLanguage) {
            notifyLanguageChanged(savedLanguage)
        }
        if (oldNightMode != null && oldNightMode != savedNightMode) {
            notifyNightModeChanged(savedNightMode)
        }
    }

    fun applyConfiguration(context: Context): Context {
        return createConfiguredContext(context, includeLanguage = true, includeNightMode = true)
    }

    fun applyNightMode(context: Context): Context {
        return createConfiguredContext(context, includeLanguage = false, includeNightMode = true)
    }

    @Suppress("DEPRECATION")
    private fun createConfiguredContext(
        context: Context,
        includeLanguage: Boolean,
        includeNightMode: Boolean
    ): Context {
        val configuration = Configuration(context.resources.configuration)
        var changed = false

        if (includeLanguage) {
            val locale = currentLanguage.locale
            if (locale != null) {
                Locale.setDefault(locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    configuration.setLocale(locale)
                    configuration.setLocales(LocaleList(locale))
                } else {
                    configuration.locale = locale
                }
                changed = true
            }
        }

        if (includeNightMode) {
            val targetNightMode = when (currentNightMode) {
                SkinNightMode.DAY -> Configuration.UI_MODE_NIGHT_NO
                SkinNightMode.NIGHT -> Configuration.UI_MODE_NIGHT_YES
                SkinNightMode.FOLLOW_SYSTEM -> null
            }

            if (targetNightMode != null) {
                configuration.uiMode =
                    (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or targetNightMode
                changed = true
            }
        }

        return if (changed) context.createConfigurationContext(configuration) else context
    }

    private fun readSkin(): Skin {
        return Skin.fromName(ThemePreferenceUtil.getString(KEY_SKIN, Skin.BLUE.key))
    }

    private fun readLanguage(): Language {
        return Language.fromName(ThemePreferenceUtil.getString(KEY_LANGUAGE, Language.SYSTEM.key))
    }

    private fun readNightMode(): SkinNightMode {
        return SkinNightMode.fromName(
            ThemePreferenceUtil.getString(KEY_NIGHT_MODE, SkinNightMode.FOLLOW_SYSTEM.key)
        )
    }

    private fun notifySkinChanged(newSkin: Skin) {
        listenersSnapshot().forEach { it.onSkinChanged(newSkin) }
    }

    private fun notifyLanguageChanged(newLanguage: Language) {
        listenersSnapshot().forEach { it.onLanguageChanged(newLanguage) }
    }

    private fun notifyNightModeChanged(newMode: SkinNightMode) {
        listenersSnapshot().forEach { it.onNightModeChanged(newMode) }
    }

    private fun listenersSnapshot(): List<Listener> {
        return synchronized(listeners) {
            listeners.toList()
        }
    }
}
