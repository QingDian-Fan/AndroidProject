package com.common.utils

import java.util.regex.Pattern

object StringUtils {

    @JvmStatic
    fun removeAllBank(str: String?): String {
        if (str == null) {
            return ""
        }
        return Pattern.compile("\\s*|\t|\r|\n").matcher(str).replaceAll("")
    }

    @JvmStatic
    fun removeAllBank(str: String?, count: Int): String {
        if (str == null) {
            return ""
        }
        return Pattern.compile("\\s{$count,}|\t|\r|\n").matcher(str).replaceAll(" ")
    }
}