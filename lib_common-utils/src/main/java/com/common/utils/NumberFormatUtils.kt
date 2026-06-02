package com.common.utils

import android.text.TextUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

object NumberFormatUtils {

    private val NUM_TWO_FORMAT = DecimalFormat("0.00")
    private val NUM_FOUR_FORMAT = DecimalFormat("0.0000")
    private val NUMBER_TWO_FORMAT = DecimalFormat("0.##")
    private val NUMBER_FOUR_FORMAT = DecimalFormat("0.####")

    @JvmStatic
    fun keep2Num(numberString: String): String = NUM_TWO_FORMAT.format(numberString.toDouble())

    @JvmStatic
    fun keep2Number(numberString: String): String = NUMBER_TWO_FORMAT.format(numberString.toDouble())

    @JvmStatic
    fun keep4Num(numberString: String): String = NUM_FOUR_FORMAT.format(numberString.toDouble())

    @JvmStatic
    fun keep4Number(numberString: String): String = NUMBER_FOUR_FORMAT.format(numberString.toDouble())

    @JvmStatic
    fun fmtMicrometer(text: String): String {
        if (text.toDouble() == 0.0) {
            return "0"
        }

        if (text.indexOf(".") in 1..3) {
            return text
        }

        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'

        val df: DecimalFormat = if (text.indexOf(".") > 0) {
            val subLength = text.length - text.indexOf(".") - 1
            when (subLength) {
                0 -> DecimalFormat("###,##0.", dfs)
                1 -> DecimalFormat("###,##0.#", dfs)
                else -> DecimalFormat("###,##0.##", dfs)
            }
        } else {
            DecimalFormat("###,##0", dfs)
        }

        val number = try {
            text.toDouble()
        } catch (e: Exception) {
            0.0
        }
        return df.format(number)
    }

    @JvmStatic
    fun getLargeNumber(largeNumber: String?): String {
        if (TextUtils.isEmpty(largeNumber) || TextUtils.isEmpty(largeNumber?.trim())) {
            return "--"
        }
        val number = try {
            largeNumber!!.toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
        if (number == 0.0) {
            return "0"
        }
        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'
        var decimalFormat = DecimalFormat("#0.00", dfs)
        if (number < 10000) {
            if (number < 100) {
                decimalFormat = DecimalFormat("#0.0000", dfs)
            }
            if (number < 1) {
                decimalFormat = DecimalFormat("#0.000000##", dfs)
            }
            if (number <= 0.00000001) {
                decimalFormat = DecimalFormat("#0.000000####", dfs)
            }
            return decimalFormat.format(number)
        }
        if (number >= 10000 && number < 100000000) {
            return decimalFormat.format(number / 10000) + "万"
        }
        return decimalFormat.format(number / 100000000) + "亿"
    }
}