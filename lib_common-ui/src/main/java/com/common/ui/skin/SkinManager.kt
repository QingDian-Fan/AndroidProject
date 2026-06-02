package com.common.ui.skin

import android.content.Context
import android.content.res.Configuration
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
        ThemePreferenceStore.putString(KEY_SKIN, newSkin.key)
        dispatch { it.onSkinChanged(newSkin) }
        return true
    }

    fun switchLanguage(newLanguage: Language): Boolean {
        if (currentLanguage == newLanguage) return false
        cachedLanguage = newLanguage
        ThemePreferenceStore.putString(KEY_LANGUAGE, newLanguage.key)
        dispatch { it.onLanguageChanged(newLanguage) }
        return true
    }

    fun switchNightMode(newMode: SkinNightMode): Boolean {
        if (currentNightMode == newMode) return false
        cachedNightMode = newMode
        ThemePreferenceStore.putString(KEY_NIGHT_MODE, newMode.key)
        dispatch { it.onNightModeChanged(newMode) }
        return true
    }

    fun applyConfiguration(context: Context): Context {
        return createConfiguredContext(context, includeLanguage = true, includeNightMode = true)
    }

    fun applyNightMode(context: Context): Context {
        return createConfiguredContext(context, includeLanguage = false, includeNightMode = true)
    }

    private fun createConfiguredContext(
        context: Context,
        includeLanguage: Boolean,
        includeNightMode: Boolean
    ): Context {
        val configuration = Configuration(context.resources.configuration)
        var changed = false

        if (includeLanguage) {
            currentLanguage.locale?.let { locale ->
                Locale.setDefault(locale)
                configuration.setLocale(locale)
                configuration.setLocales(LocaleList(locale))
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

    private fun readSkin(): Skin =
        Skin.fromName(ThemePreferenceStore.getString(KEY_SKIN, Skin.BLUE.key))

    private fun readLanguage(): Language =
        Language.fromName(ThemePreferenceStore.getString(KEY_LANGUAGE, Language.SYSTEM.key))

    private fun readNightMode(): SkinNightMode =
        SkinNightMode.fromName(
            ThemePreferenceStore.getString(KEY_NIGHT_MODE, SkinNightMode.FOLLOW_SYSTEM.key)
        )

    private inline fun dispatch(block: (Listener) -> Unit) {
        val snapshot = synchronized(listeners) { listeners.toList() }
        snapshot.forEach(block)
    }
}