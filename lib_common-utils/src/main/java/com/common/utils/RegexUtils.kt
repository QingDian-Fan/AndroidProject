package com.common.utils

import java.util.regex.Pattern

/**
 * 字符串正则匹配
 *
 * @author Cuizhen
 * @date 2018/7/18-下午1:52
 */
object RegexUtils {

    const val PASSWORD_MIN_LENGTH = 6
    const val PASSWORD_MAX_LENGTH = 18

    private const val REGEX_E_MAIL =
        "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)\$"
    private const val REGEX_PHONE = "^(1[3456789][0-9])\\d{8}"
    private val REGEX_PASSWORD_NUN_OR_EN = "^[a-z0-9A-Z]{$PASSWORD_MIN_LENGTH,$PASSWORD_MAX_LENGTH}$"
    private const val REGEX_ID_NUM = "^\\d{15}|\\d{18}|\\d{17}(\\d|X|x)"
    private val REGEX_PASSWORD_LENGTH = "^.{$PASSWORD_MIN_LENGTH,$PASSWORD_MAX_LENGTH}$"
    private const val REGEX_PHONE_LENGTH = "^.{11}\$"
    private const val REGEX_PHONE_HIDE = "(\\d{3})\\d{4}(\\d{4})"
    private const val REGEX_E_MAIL_HIDE = "(\\w?)(\\w+)(\\w)(@\\w+\\.[a-z]+(\\.[a-z]+)?)"

    /**
     * 邮箱格式是否正确
     */
    @JvmStatic
    fun matchEmail(email: String): Boolean = match(email, REGEX_E_MAIL)

    /**
     * 手机号格式是否正确
     */
    @JvmStatic
    fun matchPhone(phone: String): Boolean = match(phone, REGEX_PHONE)

    /**
     * 手机号长度是否正确
     */
    @JvmStatic
    fun matchPhoneLength(phone: String): Boolean = match(phone, REGEX_PHONE_LENGTH)

    /**
     * 手机号用****号隐藏中间数字
     */
    @JvmStatic
    fun hidePhone(phone: String): String = phone.replace(Regex(REGEX_PHONE_HIDE), "$1****$2")

    /**
     * 邮箱用****号隐藏前面的字母
     */
    @JvmStatic
    fun hideEmail(email: String): String = email.replace(Regex(REGEX_E_MAIL_HIDE), "$1****$3$4")

    /**
     * 密码格式是否正确
     */
    @JvmStatic
    fun matchPassword(psw: String): Boolean = match(psw, REGEX_PASSWORD_NUN_OR_EN)

    /**
     * 密码长度是否正确
     */
    @JvmStatic
    fun matchPasswordLength(psw: String): Boolean = match(psw, REGEX_PASSWORD_LENGTH)

    /**
     * 身份证号格式是否正确
     */
    @JvmStatic
    fun matchIdNum(id: String): Boolean = match(id, REGEX_ID_NUM)

    /**
     * 字符串正则匹配
     *
     * @param s     待匹配字符串
     * @param regex 正则表达式
     */
    @JvmStatic
    fun match(s: String?, regex: String): Boolean {
        if (s == null) {
            return false
        }
        return Pattern.compile(regex).matcher(s).matches()
    }
}