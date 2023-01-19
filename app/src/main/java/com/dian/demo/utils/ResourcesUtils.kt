package com.dian.demo.utils

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.*
import androidx.core.content.ContextCompat
import com.dian.demo.ProjectApplication
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object ResourcesUtils {

    private fun getResources(): Resources {
        return ProjectApplication.getAppContext().resources
    }

    @kotlin.jvm.JvmStatic
    fun getDrawable(@DrawableRes id: Int): Drawable? {
        return ContextCompat.getDrawable(ProjectApplication.getAppContext(), id)
    }

    @kotlin.jvm.JvmStatic
    fun getString(@StringRes id: Int): String {
        return getResources().getString(id)
    }

    fun getString(@StringRes id: Int, vararg params: Any?): String {
        return getResources().getString(id, *params)
    }

    @kotlin.jvm.JvmStatic
    fun getColor(@ColorRes id: Int): Int {
        return ContextCompat.getColor(ProjectApplication.getAppContext(), id)
    }

    fun getColor(context: Context, @ColorRes id: Int): Int {
        return context.resources.getColor(id)
    }

    fun getColor(view: View, @ColorRes id: Int): Int {
        return view.context.resources.getColor(id)
    }

    fun getDimens(@DimenRes id: Int): Float {
        return getResources().getDimension(id)
    }

    fun getStringArray(@ArrayRes id: Int): Array<String?> {
        return getResources().getStringArray(id)
    }

    fun getBoolean(@BoolRes id: Int): Boolean {
        return getResources().getBoolean(id)
    }

    fun getInteger(@IntegerRes id: Int): Int {
        return getResources().getInteger(id)
    }

    fun getAssets(fileName: String?): String {
        val stringBuilder = StringBuilder()
        try {
            val assetManager: AssetManager = ProjectApplication.getAppContext().assets
            val bf = BufferedReader(
                InputStreamReader(
                    assetManager.open(
                        fileName!!
                    )
                )
            )
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }
}