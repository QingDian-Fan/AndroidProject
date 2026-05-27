package com.common.ui.skin

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.common.ui.R
import com.common.ui.skin.SkinConfig.TAG_SKIN_PREFIX
import com.google.android.material.tabs.TabLayout

internal object SkinApplier {

    @Suppress("DEPRECATION")
    fun applyWindow(window: Window, context: Context, skin: Skin) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val main = themeColor(context, skin, R.attr.skinColorMain)
            window.statusBarColor = darker(main)
            window.navigationBarColor = darker(main)
        }
    }

    fun applyToView(
        root: View,
        context: Context,
        skin: Skin,
        language: Language,
        nightMode: SkinNightMode,
        refreshCount: Int
    ) {
        val palette = SkinPalette.from(context, skin)
        val listViews = mutableListOf<ListView>()
        applyRecursive(root, palette, SkinState(skin, language, nightMode, refreshCount), listViews)
        listViews.forEach {
            (it.adapter as? BaseAdapter)?.notifyDataSetChanged()
        }
    }

    @ColorInt
    fun themeColor(context: Context, skin: Skin, @AttrRes attr: Int): Int {
        val wrapper = ContextThemeWrapper(SkinManager.applyNightMode(context), skin.themeRes)
        val typedArray = wrapper.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, Color.TRANSPARENT)
        typedArray.recycle()
        return color
    }

    fun roundRect(
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

    @ColorInt
    fun darker(@ColorInt color: Int): Int {
        return Color.rgb(
            (Color.red(color) * 0.82F).toInt(),
            (Color.green(color) * 0.82F).toInt(),
            (Color.blue(color) * 0.82F).toInt()
        )
    }

    private fun applyRecursive(
        view: View,
        palette: SkinPalette,
        state: SkinState,
        listViews: MutableList<ListView>
    ) {
        val rules = parseSkinRules(view)
        applyRules(view, rules, palette, state)

        if (view is ListView) {
            rules["divider"]?.let {
                view.divider = roundRect(palette.colorFor(it), 0F)
                view.dividerHeight = view.dp(1F).toInt()
            }
            listViews.add(view)
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                applyRecursive(view.getChildAt(index), palette, state, listViews)
            }
        }
    }

    private fun applyRules(
        view: View,
        rules: Map<String, String>,
        palette: SkinPalette,
        state: SkinState
    ) {
        rules["bg"]?.let {
            applyBackground(view, it, rules, palette)
        }
        (rules["bgDrawable"] ?: rules["backgroundDrawable"])?.let {
            applyBackgroundDrawable(view, it, state.skin)
        }
        if (view is ImageView) {
            (rules["src"] ?: rules["image"])?.let {
                drawableFor(view.context, state.skin, it)?.let { drawable ->
                    view.setImageDrawable(drawable)
                }
            }
        }

        rules["tint"]?.let {
            applyTint(view, palette.colorFor(it))
        }

        if (view is TextView) {
            applyCompoundDrawables(view, rules, state.skin)
            rules["text"]?.let {
                view.setTextColor(palette.colorFor(it))
            }
            rules["hint"]?.let {
                view.setHintTextColor(palette.colorFor(it))
            }
            rules["state"]?.let {
                applyStateText(view, it, state)
            }
        }

        if (view is Button) {
            rules["button"]?.let {
                applyButton(view, it, rules, palette, state.skin)
            }
            rules["lang"]?.let {
                applyLanguageButton(view, it, rules, palette, state.language)
            }
        }

        if (view is TabLayout && rules.containsKey("tab")) {
            applyTabLayout(view, palette)
        }

        view.invalidate()
    }

    private fun applyBackground(
        view: View,
        role: String,
        rules: Map<String, String>,
        palette: SkinPalette
    ) {
        val color = palette.colorFor(role)
        val radius = rules["radius"]?.toFloatOrNull()?.let { view.dp(it) } ?: 0F
        val strokeColor = rules["stroke"]?.let { palette.colorFor(it) } ?: Color.TRANSPARENT
        val strokeWidth = rules["strokeWidth"]?.toFloatOrNull()?.let { view.dp(it).toInt() } ?: 0

        if (radius > 0F || strokeWidth > 0) {
            view.background = roundRect(color, radius, strokeColor, strokeWidth)
        } else {
            view.setBackgroundColor(color)
        }
    }

    private fun applyBackgroundDrawable(view: View, name: String, skin: Skin) {
        drawableFor(view.context, skin, name)?.let {
            view.background = it
        }
    }

    private fun applyCompoundDrawables(textView: TextView, rules: Map<String, String>, skin: Skin) {
        val hasDrawableRule = rules.containsKey("drawableStart") ||
            rules.containsKey("drawableLeft") ||
            rules.containsKey("drawableTop") ||
            rules.containsKey("drawableEnd") ||
            rules.containsKey("drawableRight") ||
            rules.containsKey("drawableBottom")

        if (!hasDrawableRule) return

        val current = textView.compoundDrawablesRelative
        val start = (rules["drawableStart"] ?: rules["drawableLeft"])
            ?.let { drawableFor(textView.context, skin, it) } ?: current[0]
        val top = rules["drawableTop"]
            ?.let { drawableFor(textView.context, skin, it) } ?: current[1]
        val end = (rules["drawableEnd"] ?: rules["drawableRight"])
            ?.let { drawableFor(textView.context, skin, it) } ?: current[2]
        val bottom = rules["drawableBottom"]
            ?.let { drawableFor(textView.context, skin, it) } ?: current[3]

        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom)
    }

    private fun drawableFor(context: Context, skin: Skin, value: String): Drawable? {
        val name = drawableName(value) ?: return null
        val resourceContext = ContextThemeWrapper(SkinManager.applyNightMode(context), skin.themeRes)
        val id = resourceContext.resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return null
        return ContextCompat.getDrawable(resourceContext, id)?.mutate()
    }

    private fun drawableName(value: String): String? {
        val name = value.trim()
            .removePrefix("@drawable/")
            .removePrefix("@")
            .substringAfterLast("/")
        return name.takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun applyTint(view: View, @ColorInt color: Int) {
        when (view) {
            is ImageView -> view.imageTintList = ColorStateList.valueOf(color)
            is CompoundButton -> view.buttonTintList = ColorStateList.valueOf(color)
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.backgroundTintList = ColorStateList.valueOf(color)
            }
        }
    }

    private fun applyButton(
        button: Button,
        role: String,
        rules: Map<String, String>,
        palette: SkinPalette,
        skin: Skin
    ) {
        val radius = rules["radius"]?.toFloatOrNull()?.let { button.dp(it) } ?: button.dp(18F)

        if (role == "action") {
            button.setTextColor(palette.main)
            button.background = roundRect(palette.surface, radius)
            return
        }

        val buttonSkin = Skin.fromName(role)
        if (skin == buttonSkin) {
            button.setTextColor(palette.onMain)
            button.background = roundRect(palette.main, radius)
        } else {
            button.setTextColor(palette.textMain)
            button.background = roundRect(palette.surface, radius, palette.main, button.dp(1F).toInt())
        }
    }

    private fun applyLanguageButton(
        button: Button,
        role: String,
        rules: Map<String, String>,
        palette: SkinPalette,
        language: Language
    ) {
        val radius = rules["radius"]?.toFloatOrNull()?.let { button.dp(it) } ?: button.dp(18F)
        val buttonLanguage = Language.fromName(role)

        if (language == buttonLanguage) {
            button.setTextColor(palette.onMain)
            button.background = roundRect(palette.main, radius)
        } else {
            button.setTextColor(palette.textMain)
            button.background = roundRect(palette.surface, radius, palette.main, button.dp(1F).toInt())
        }
    }

    private fun applyStateText(textView: TextView, role: String, state: SkinState) {
        textView.text = when (role) {
            "skin" -> state.skin.label
            "language" -> state.language.key
            "night" -> state.nightMode.key
            "refresh" -> state.refreshCount.toString()
            "home" -> "Home #${System.identityHashCode(textView.context)}  |  ${state.skin.label}"
            "demo" -> "Demo #${System.identityHashCode(textView.context)}  |  ${state.skin.label}"
            else -> textView.text
        }
    }

    private fun applyTabLayout(tabLayout: TabLayout, palette: SkinPalette) {
        tabLayout.setBackgroundColor(palette.main)
        tabLayout.setSelectedTabIndicatorColor(palette.onMain)
        tabLayout.setTabTextColors(palette.textSecond, palette.onMain)
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

    private fun View.dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}

private data class SkinState(
    val skin: Skin,
    val language: Language,
    val nightMode: SkinNightMode,
    val refreshCount: Int
)

private data class SkinPalette(
    val main: Int,
    val background: Int,
    val surface: Int,
    val onMain: Int,
    val textMain: Int,
    val textSecond: Int
) {
    @ColorInt
    fun colorFor(role: String): Int {
        return when (role) {
            "main" -> main
            "background" -> background
            "surface" -> surface
            "onMain" -> onMain
            "textMain" -> textMain
            "textSecond" -> textSecond
            else -> Color.TRANSPARENT
        }
    }

    companion object {
        fun from(context: Context, skin: Skin): SkinPalette {
            return SkinPalette(
                main = SkinApplier.themeColor(context, skin, R.attr.skinColorMain),
                background = SkinApplier.themeColor(context, skin, R.attr.skinColorBackground),
                surface = SkinApplier.themeColor(context, skin, R.attr.skinColorSurface),
                onMain = SkinApplier.themeColor(context, skin, R.attr.skinColorOnMain),
                textMain = SkinApplier.themeColor(context, skin, R.attr.skinColorTextMain),
                textSecond = SkinApplier.themeColor(context, skin, R.attr.skinColorTextSecond)
            )
        }
    }
}
