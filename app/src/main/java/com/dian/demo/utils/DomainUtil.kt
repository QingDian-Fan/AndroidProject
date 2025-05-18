package com.dian.demo.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

object DomainUtil {
    //baidu.com
    fun getDomainShort(url: String): String {
        val pattern: Pattern = Pattern.compile("(?<=http://|\\.)[^.]*?\\.(com|cn|net|org|biz|info|cc|tv)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher: Matcher = pattern.matcher(url)
        matcher.find()
        return matcher.group()
    }

    //www.baidu.com
    fun getDomainMedium(url: String): String {
        val pattern: Pattern =
            Pattern.compile("[^//]*?\\.(com|cn|net|org|biz|info|cc|tv)", Pattern.CASE_INSENSITIVE)
        val matcher: Matcher = pattern.matcher(url)
        matcher.find()
        return matcher.group()
    }

    //http://www.baidu.com/
    fun getDomainLong(url: String): String {
        val pattern = "/(?!/)"
        val compile: Pattern = Pattern.compile(pattern)
        val matcher: Matcher = compile.matcher(url)
        val list: ArrayList<Int> = ArrayList()
        while (matcher.find()) {
            list.add(matcher.start())
        }
        return url.substring(0, list[1] + 1)
    }

}