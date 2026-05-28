package com.common.weight.titlebar

import android.os.Build
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

object OSUtils {
    private const val ROM_MIUI = "MIUI"
    private const val ROM_EMUI = "EMUI"
    private const val ROM_FLYME = "FLYME"
    private const val ROM_OPPO = "OPPO"
    private const val ROM_SMARTISAN = "SMARTISAN"
    private const val ROM_VIVO = "VIVO"
    private const val ROM_QIKU = "QIKU"

    private const val KEY_VERSION_MIUI = "ro.miui.ui.version.name"
    private const val KEY_VERSION_EMUI = "ro.build.version.emui"
    private const val KEY_VERSION_OPPO = "ro.build.version.opporom"
    private const val KEY_VERSION_SMARTISAN = "ro.smartisan.version"
    private const val KEY_VERSION_VIVO = "ro.vivo.os.version"

    @Volatile
    private var sName: String? = null

    @Volatile
    private var sVersion: String? = null

    fun isEmui(): Boolean = check(ROM_EMUI)
    fun isMiui(): Boolean = check(ROM_MIUI)
    fun isVivo(): Boolean = check(ROM_VIVO)
    fun isOppo(): Boolean = check(ROM_OPPO)
    fun isFlyme(): Boolean = check(ROM_FLYME)
    fun is360(): Boolean = check(ROM_QIKU) || check("360")
    fun isSmartisan(): Boolean = check(ROM_SMARTISAN)

    fun getName(): String? {
        if (sName == null) check("")
        return sName
    }

    fun getVersion(): String? {
        if (sVersion == null) check("")
        return sVersion
    }

    @Synchronized
    private fun check(rom: String): Boolean {
        sName?.let { return it == rom }

        val candidates = arrayOf(
            ROM_MIUI to KEY_VERSION_MIUI,
            ROM_EMUI to KEY_VERSION_EMUI,
            ROM_OPPO to KEY_VERSION_OPPO,
            ROM_VIVO to KEY_VERSION_VIVO,
            ROM_SMARTISAN to KEY_VERSION_SMARTISAN
        )
        for ((romName, key) in candidates) {
            val value = getProp(key)
            if (!value.isNullOrEmpty()) {
                sName = romName
                sVersion = value
                return romName == rom
            }
        }

        val display = Build.DISPLAY.orEmpty()
        if (display.uppercase(Locale.ROOT).contains(ROM_FLYME)) {
            sName = ROM_FLYME
            sVersion = display
        } else {
            sName = Build.MANUFACTURER.orEmpty().uppercase(Locale.ROOT)
            sVersion = Build.UNKNOWN
        }
        return sName == rom
    }

    private fun getProp(name: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", name))
            BufferedReader(InputStreamReader(process.inputStream), 1024).use { it.readLine() }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }
}