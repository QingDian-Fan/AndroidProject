package com.common.ui.skin

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.databinding.ViewDataBinding
import com.common.ui.BaseAppBindActivity
import com.common.ui.R
import com.common.ui.skin.SkinConfig.KEY_LANGUAGE
import com.common.ui.skin.SkinConfig.KEY_SKIN
import com.common.ui.skin.SkinConfig.SP_NAME
import com.common.ui.skin.SkinConfig.TAG_SKIN_PREFIX
import com.google.android.material.tabs.TabLayout
import java.util.Locale

abstract class BaseSkinBindActivity<B : ViewDataBinding> : BaseAppBindActivity<B>()  {

    protected var skin = Skin.BLUE
        private set
    protected var language = Language.SYSTEM
        private set
    private var refreshCount = 0

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        skin = loadSkin()
        language = loadLanguage(this)
        setTheme(skin.themeRes)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val savedLanguage = loadLanguage(this)
        if (savedLanguage != language) {
            language = savedLanguage
            recreate()
            return
        }

        val savedSkin = loadSkin()
        if (savedSkin != skin) {
            skin = savedSkin
            setTheme(savedSkin.themeRes)
            refreshSkinViews()
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        refreshSkinViews()
    }

    protected fun switchLanguage(newLanguage: Language) {
        if (language == newLanguage) return
        language = newLanguage
        ThemePreferenceUtil
            .putString(KEY_LANGUAGE, newLanguage.key)

        recreate()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        refreshSkinViews()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        refreshSkinViews()
    }

    protected fun switchSkin(newSkin: Skin) {
        if (skin == newSkin) return
        skin = newSkin
        ThemePreferenceUtil
            .putString(KEY_SKIN, newSkin.key)

        setTheme(newSkin.themeRes)
        refreshSkinViews()
    }

    protected fun refreshSkinViews() {
        refreshCount++
        applyStatusBarColor(themeColor(R.attr.skinColorMain))
        val listViews = mutableListOf<ListView>()
        applySkinToView(window.decorView, listViews)
        listViews.forEach {
            (it.adapter as? BaseAdapter)?.notifyDataSetChanged()
        }
    }

    fun applySkinToView(view: View) {
        val listViews = mutableListOf<ListView>()
        applySkinToView(view, listViews)
        listViews.forEach {
            (it.adapter as? BaseAdapter)?.notifyDataSetChanged()
        }
    }

    @ColorInt
    protected fun themeColor(@AttrRes attr: Int): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, Color.TRANSPARENT)
        typedArray.recycle()
        return color
    }

    protected fun roundRect(
        @ColorInt fill: Int,
        radius: Float,
        @ColorInt stroke: Int = Color.TRANSPARENT,
        strokeWidth: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius
            if (strokeWidth > 0) {
                setStroke(strokeWidth, stroke)
            }
        }
    }

    protected fun applyStatusBarColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = darker(color)
        }
    }

    @ColorInt
    protected fun darker(@ColorInt color: Int): Int {
        return Color.rgb(
            (Color.red(color) * 0.82F).toInt(),
            (Color.green(color) * 0.82F).toInt(),
            (Color.blue(color) * 0.82F).toInt()
        )
    }

    protected val Int.dp: Float
        get() = this * resources.displayMetrics.density

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density

    private fun applySkinToView(view: View, listViews: MutableList<ListView>) {
        val rules = parseSkinRules(view)
        applySkinRules(view, rules)

        if (view is ListView) {
            rules["divider"]?.let {
                view.divider = roundRect(colorFor(it), 0F)
                view.dividerHeight = 1.dp.toInt()
            }
            listViews.add(view)
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applySkinToView(view.getChildAt(i), listViews)
            }
        }
    }

    private fun applySkinRules(view: View, rules: Map<String, String>) {
        rules["bg"]?.let {
            applyBackground(view, it, rules)
        }

        if (view is TextView) {
            rules["text"]?.let {
                view.setTextColor(colorFor(it))
            }
            rules["state"]?.let {
                applyStateText(view, it)
            }
        }

        if (view is Button) {
            rules["button"]?.let {
                applyButton(view, it, rules)
            }
            rules["lang"]?.let {
                applyLanguageButton(view, it, rules)
            }
        }

        if (view is TabLayout) {
            rules["tab"]?.let {
                applyTabLayout(view)
            }
        }
    }

    private fun applyBackground(view: View, role: String, rules: Map<String, String>) {
        val color = colorFor(role)
        val radius = rules["radius"]?.toFloatOrNull()?.dp ?: 0F
        val strokeColor = rules["stroke"]?.let { colorFor(it) } ?: Color.TRANSPARENT
        val strokeWidth = rules["strokeWidth"]?.toFloatOrNull()?.dp?.toInt() ?: 0

        if (radius > 0F || strokeWidth > 0) {
            view.background = roundRect(color, radius, strokeColor, strokeWidth)
        } else {
            view.setBackgroundColor(color)
        }
    }

    private fun applyButton(button: Button, role: String, rules: Map<String, String>) {
        val radius = rules["radius"]?.toFloatOrNull()?.dp ?: 18.dp
        val main = themeColor(R.attr.skinColorMain)
        val surface = themeColor(R.attr.skinColorSurface)
        val onMain = themeColor(R.attr.skinColorOnMain)
        val textMain = themeColor(R.attr.skinColorTextMain)

        if (role == "action") {
            button.setTextColor(if (skin == Skin.BLACK) onMain else main)
            button.background = roundRect(surface, radius)
            return
        }

        val buttonSkin = Skin.fromName(role)
        if (skin == buttonSkin) {
            button.setTextColor(onMain)
            button.background = roundRect(main, radius)
        } else {
            button.setTextColor(textMain)
            button.background = roundRect(surface, radius, main, 1.dp.toInt())
        }
    }

    private fun applyLanguageButton(button: Button, role: String, rules: Map<String, String>) {
        val radius = rules["radius"]?.toFloatOrNull()?.dp ?: 18.dp
        val main = themeColor(R.attr.skinColorMain)
        val surface = themeColor(R.attr.skinColorSurface)
        val onMain = themeColor(R.attr.skinColorOnMain)
        val textMain = themeColor(R.attr.skinColorTextMain)
        val buttonLanguage = Language.fromName(role)

        if (language == buttonLanguage) {
            button.setTextColor(onMain)
            button.background = roundRect(main, radius)
        } else {
            button.setTextColor(textMain)
            button.background = roundRect(surface, radius, main, 1.dp.toInt())
        }
    }

    private fun applyStateText(textView: TextView, role: String) {
        textView.text = when (role) {
            "home" -> "Home #${System.identityHashCode(this)}  |  ${skin.label}"
            "demo" -> "Demo #${System.identityHashCode(this)}  |  ${skin.label}"
            else -> "Activity #${System.identityHashCode(this)}  |  refresh $refreshCount  |  ${skin.label}"
        }
    }

    private fun applyTabLayout(tabLayout: TabLayout) {
        val main = themeColor(R.attr.skinColorMain)
        val onMain = themeColor(R.attr.skinColorOnMain)
        val textSecond = themeColor(R.attr.skinColorTextSecond)

        tabLayout.setBackgroundColor(main)
        tabLayout.setSelectedTabIndicatorColor(onMain)
        tabLayout.setTabTextColors(textSecond, onMain)
    }

    @ColorInt
    private fun colorFor(role: String): Int {
        return when (role) {
            "main" -> themeColor(R.attr.skinColorMain)
            "background" -> themeColor(R.attr.skinColorBackground)
            "surface" -> themeColor(R.attr.skinColorSurface)
            "onMain" -> themeColor(R.attr.skinColorOnMain)
            "textMain" -> themeColor(R.attr.skinColorTextMain)
            "textSecond" -> themeColor(R.attr.skinColorTextSecond)
            else -> Color.TRANSPARENT
        }
    }

    private fun parseSkinRules(view: View): Map<String, String> {
        val tag = view.tag as? String ?: return emptyMap()
        if (!tag.startsWith(TAG_SKIN_PREFIX)) return emptyMap()
        return tag.removePrefix(TAG_SKIN_PREFIX)
            .split(";")
            .mapNotNull {
                val item = it.trim()
                if (item.isEmpty()) {
                    null
                } else {
                    val index = item.indexOf("=")
                    if (index > 0) {
                        item.substring(0, index).trim() to item.substring(index + 1).trim()
                    } else {
                        item to "true"
                    }
                }
            }
            .toMap()
    }

    private fun loadSkin(): Skin {
        val name = ThemePreferenceUtil
            .getString(KEY_SKIN, Skin.BLUE.key)
        return Skin.fromName(name)
    }

    @Suppress("DEPRECATION")
    private fun applyLanguage(context: Context): Context {
        val savedLanguage = loadLanguage(context)
        val locale = savedLanguage.locale ?: return context
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLocales(LocaleList(locale))
        } else {
            configuration.locale = locale
        }
        return context.createConfigurationContext(configuration)
    }

    private fun loadLanguage(context: Context): Language {
        val name = ThemePreferenceUtil
            .getString(KEY_LANGUAGE, Language.SYSTEM.key)
        return Language.fromName(name)
    }
}
