package com.dian.demo.utils


import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Log
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object DateFormatUtil {
    private const val DATE_FORMAT_TIME_SHARE = "yyyy.MM.dd HH:mm"
    private const val DATE_FORMAT_TIME = "yyyy-MM-dd"
    private const val DATE_FORMAT_TIME_24H = "HH:mm"
    private const val MM_DD_HH_MM = "MM-dd HH:mm"

    @JvmStatic
    fun getShareDate(): String {
        val simpleDateFormat = SimpleDateFormat(DATE_FORMAT_TIME_SHARE, Locale.CHINA)
        return simpleDateFormat.format(Calendar.getInstance().time)
    }

    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    fun getCurrentDate(time: Long): String {
        val simpleDateFormat = SimpleDateFormat(DATE_FORMAT_TIME, Locale.CHINA)
        return simpleDateFormat.format(time)
    }

    fun getNewsTime(time: Long): String {
        val simpleDateFormat = SimpleDateFormat(DATE_FORMAT_TIME_24H, Locale.CHINA)
        return simpleDateFormat.format(time)
    }

    fun getMarketFluctuationTime(time: Long): String {
        val simpleDateFormat = SimpleDateFormat(MM_DD_HH_MM, Locale.CHINA)
        return simpleDateFormat.format(time)
    }

    /**
     * one day millisecond count
     */
    const val ONE_DAY_MILLISECONDS = (1000 * 3600 * 24).toLong()

    const val ONE_HOUR_MILLISECONDS = (1000 * 3600).toLong()

    const val ONE_MIN_MILLISECONDS = (1000 * 60).toLong()

    /**
     * 时间日期格式化到年月日时分秒.
     */
    var dateFormatYMDHMS = "yyyy-MM-dd HH:mm:ss"
    var dateFormatYMDHMS_f = "yyyyMMddHHmmss"
    var dateFormatMDHM = "MM-dd HH:mm"
    var dateFormat = "yyyy-MM-dd HH:mm"

    /**
     * 时间日期格式化到年月日.
     */
    var dateFormatYMD = "yyyy-MM-dd"

    /**
     * 时间日期格式化到年月日时分.中文显示
     */
    var dateFormatYMDHMofChinese = "yyyy年MM月dd日 HH:mm"

    /**
     * 时间日期格式化到年月日.中文显示
     */
    var dateFormatYMDofChinese = "yyyy年MM月dd日"

    /**
     * 时间日期格式化到月日.中文显示
     */
    var dateFormatMDofChinese = "MM月dd日"

    /**
     * 时间日期格式化到月.中文显示
     */
    var dateFormatMofChinese = "MM月"

    /**
     * 时间日期格式化到年月.
     */
    var dateFormatYM = "yyyy-MM"

    /**
     * 时间日期格式化到年月日时分.
     */
    var dateFormatYMDHM = "yyyy-MM-dd HH:mm"

    /**
     * 时间日期格式化到月日.
     */
    var dateFormatMD = "MM/dd"
    var dateFormatM_D = "MM-dd"

    var dateFormatM = "MM月"
    var dateFormatD = "dd"
    var dateFormatM2 = "MM"

    var dateFormatMDHMofChinese = "MM月dd日HH时mm分"
    var dateFormatHMofChinese = "HH时mm分"

    /**
     * 时分秒.
     */
    var dateFormatHMS = "HH:mm:ss"

    /**
     * 时分.
     */
    var dateFormatHM = "HH:mm"

    /**
     * 上午/下午时分
     */
    var dateFormatAHM = "aHH:mm"

    var dateFormatYMDE = "yyyy/MM/dd E"
    var dateFormatYMD2 = "yyyy/MM/dd"

    private val dateFormater: ThreadLocal<SimpleDateFormat> =
        object : ThreadLocal<SimpleDateFormat>() {
            @SuppressLint("SimpleDateFormat")
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            }
        }

    @SuppressLint("SimpleDateFormat")
    private val dateFormater2: ThreadLocal<SimpleDateFormat> =
        object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd")
            }
        }

    /**
     * 时间戳转特定格式时间
     * @param dataFormat
     * @param timeStamp
     * @return
     */
    fun formatData(dataFormat: String?, timeStamp: Long): String? {
        var timeStamp = timeStamp
        if (timeStamp == 0L) {
            return ""
        }
        timeStamp = timeStamp * 1000
        val format = SimpleDateFormat(dataFormat)
        return format.format(Date(timeStamp))
    }

    /**
     * 将毫秒转换成秒
     *
     * @param time
     * @return
     */
    fun convertToSecond(time: Long?): Int {
        val date = Date()
        date.time = time!!
        return date.seconds
    }

    /**
     * 描述：String类型的日期时间转化为Date类型.
     *
     * @param strDate String形式的日期时间
     * @param format  格式化字符串，如："yyyy-MM-dd HH:mm:ss"
     * @return Date Date类型日期时间
     */
    fun getDateByFormat(strDate: String?, format: String?): Date? {
        val mSimpleDateFormat = SimpleDateFormat(format)
        var date: Date? = null
        try {
            date = mSimpleDateFormat.parse(strDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date
    }

    /**
     * 描述：获取偏移之后的Date.
     *
     * @param date          日期时间
     * @param calendarField Calendar属性，对应offset的值， 如(Calendar.DATE,表示+offset天,Calendar.HOUR_OF_DAY,表示＋offset小时)
     * @param offset        偏移(值大于0,表示+,值小于0,表示－)
     * @return Date 偏移之后的日期时间
     */
    fun getDateByOffset(date: Date?, calendarField: Int, offset: Int): Date? {
        val c: Calendar = GregorianCalendar()
        try {
            c.time = date
            c.add(calendarField, offset)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return c.time
    }

    /**
     * 描述：获取指定日期时间的字符串(可偏移).
     *
     * @param strDate       String形式的日期时间
     * @param format        格式化字符串，如："yyyy-MM-dd HH:mm:ss"
     * @param calendarField Calendar属性，对应offset的值， 如(Calendar.DATE,表示+offset天,Calendar.HOUR_OF_DAY,表示＋offset小时)
     * @param offset        偏移(值大于0,表示+,值小于0,表示－)
     * @return String String类型的日期时间
     */
    fun getStringByOffset(
        strDate: String?,
        format: String?,
        calendarField: Int,
        offset: Int
    ): String? {
        var mDateTime: String? = null
        try {
            val c: Calendar = GregorianCalendar()
            val mSimpleDateFormat = SimpleDateFormat(format)
            c.time = mSimpleDateFormat.parse(strDate)
            c.add(calendarField, offset)
            mDateTime = mSimpleDateFormat.format(c.time)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return mDateTime
    }

    /**
     * 描述：Date类型转化为String类型(可偏移).
     *
     * @param date          the date
     * @param format        the format
     * @param calendarField the calendar field
     * @param offset        the offset
     * @return String String类型日期时间
     */
    fun getStringByOffset(date: Date?, format: String?, calendarField: Int, offset: Int): String? {
        var strDate: String? = null
        try {
            val c: Calendar = GregorianCalendar()
            val mSimpleDateFormat = SimpleDateFormat(format)
            c.time = date
            c.add(calendarField, offset)
            strDate = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strDate
    }

    /**
     * from yyyy-MM-dd HH:mm:ss to MM-dd HH:mm
     */
    fun formatDate(before: String?): String? {
        val after: String
        after = try {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .parse(before)
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
        } catch (e: ParseException) {
            return before
        }
        return after
    }

    /**
     * 描述：Date类型转化为String类型.
     *
     * @param date   the date
     * @param format the format
     * @return String String类型日期时间
     */
    fun getStringByFormat(date: Date?, format: String?): String? {
        val mSimpleDateFormat = SimpleDateFormat(format)
        var strDate: String? = null
        try {
            strDate = mSimpleDateFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strDate
    }

    /**
     * 描述：获取指定日期时间的字符串,用于导出想要的格式.
     *
     * @param strDate String形式的日期时间，必须为yyyy-MM-dd HH:mm:ss格式
     * @param format  输出格式化字符串，如："yyyy-MM-dd HH:mm:ss"
     * @return String 转换后的String类型的日期时间
     */
    fun getStringByFormat(strDate: String?, format: String?): String? {
        var mDateTime: String? = null
        try {
            val c: Calendar = GregorianCalendar()
            val mSimpleDateFormat = SimpleDateFormat(format)
            c.time = mSimpleDateFormat.parse(strDate)
            val mSimpleDateFormat2 = SimpleDateFormat(format)
            mDateTime = mSimpleDateFormat2.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mDateTime
    }

    /**
     * 描述：获取milliseconds表示的日期时间的字符串.
     *
     * @param format 格式化字符串，如："yyyy-MM-dd HH:mm:ss"
     * @return String 日期时间字符串
     */
    fun getStringByFormat(milliseconds: Long, format: String?): String? {
        var thisDateTime: String? = null
        try {
            val mSimpleDateFormat = SimpleDateFormat(format)
            thisDateTime = mSimpleDateFormat.format(milliseconds)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return thisDateTime
    }

    /**
     * 描述：获取表示当前日期时间的字符串.
     *
     * @param format 格式化字符串，如："yyyy-MM-dd HH:mm:ss"
     * @return String String类型的当前日期时间
     */
    fun getCurrentDate(format: String?): String? {
        var curDateTime: String? = null
        try {
            val mSimpleDateFormat = SimpleDateFormat(format)
            val c: Calendar = GregorianCalendar()
            curDateTime = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return curDateTime
    }


    //获取当前系统当天日期
    fun getCurrentDay(): String? {
        var curDateTime: String? = null
        try {
            val mSimpleDateFormat = SimpleDateFormat(dateFormat)
            val c: Calendar = GregorianCalendar()
            c.add(Calendar.DAY_OF_MONTH, 0)
            curDateTime = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return curDateTime
    }

    //获取当前系统当天日期
    fun getCurrentDay2(): String? {
        var curDateTime: String? = null
        try {
            val mSimpleDateFormat = SimpleDateFormat(dateFormatYMDHMS)
            val c: Calendar = GregorianCalendar()
            c.add(Calendar.DAY_OF_MONTH, 0)
            curDateTime = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return curDateTime
    }

    //获取当前系统前后第几天
    fun getNextDay(i: Int): String? {
        var curDateTime: String? = null
        try {
            val mSimpleDateFormat = SimpleDateFormat(dateFormat)
            val c: Calendar = GregorianCalendar()
            c.add(Calendar.DAY_OF_MONTH, i)
            curDateTime = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return curDateTime
    }

    //获取当前系统前后第几小时
    fun getNextHour(i: Int): String? {
        var curDateTime: String? = null
        try {
            val mSimpleDateFormat = SimpleDateFormat(dateFormat)
            val c: Calendar = GregorianCalendar()
            c.add(Calendar.HOUR_OF_DAY, i)
            curDateTime = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return curDateTime
    }

    /**
     * 描述：获取表示当前日期时间的字符串(可偏移).
     *
     * @param format        格式化字符串，如："yyyy-MM-dd HH:mm:ss"
     * @param calendarField Calendar属性，对应offset的值， 如(Calendar.DATE,表示+offset天,Calendar.HOUR_OF_DAY,表示＋offset小时)
     * @param offset        偏移(值大于0,表示+,值小于0,表示－)
     * @return String String类型的日期时间
     */
    fun getCurrentDateByOffset(format: String?, calendarField: Int, offset: Int): String? {
        var mDateTime: String? = null
        try {
            val mSimpleDateFormat = SimpleDateFormat(format)
            val c: Calendar = GregorianCalendar()
            c.add(calendarField, offset)
            mDateTime = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mDateTime
    }

    /**
     * 描述：计算两个日期所差的天数.
     *
     * @param date1 第一个时间的毫秒表示
     * @param date2 第二个时间的毫秒表示
     * @return int 所差的天数
     */
    fun getOffectDay(date1: Long, date2: Long): Int {
        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = date1
        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = date2
        //先判断是否同年
        val y1 = calendar1[Calendar.YEAR]
        val y2 = calendar2[Calendar.YEAR]
        val d1 = calendar1[Calendar.DAY_OF_YEAR]
        val d2 = calendar2[Calendar.DAY_OF_YEAR]
        var maxDays = 0
        var day = 0
        if (y1 - y2 > 0) {
            maxDays = calendar2.getActualMaximum(Calendar.DAY_OF_YEAR)
            day = d1 - d2 + maxDays
        } else if (y1 - y2 < 0) {
            maxDays = calendar1.getActualMaximum(Calendar.DAY_OF_YEAR)
            day = d1 - d2 - maxDays
        } else {
            day = d1 - d2
        }
        return day
    }

    /**
     * 描述：计算两个日期所差的小时数.
     *
     * @param date1 第一个时间的毫秒表示
     * @param date2 第二个时间的毫秒表示
     * @return int 所差的小时数
     */
    fun getOffectHour(date1: Long, date2: Long): Int {
        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = date1
        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = date2
        val h1 = calendar1[Calendar.HOUR_OF_DAY]
        val h2 = calendar2[Calendar.HOUR_OF_DAY]
        var h = 0
        val day = getOffectDay(date1, date2)
        h = h1 - h2 + day * 24
        return h
    }

    /**
     * 描述：计算两个日期所差的分钟数.
     *
     * @param date1 第一个时间的毫秒表示
     * @param date2 第二个时间的毫秒表示
     * @return int 所差的分钟数
     */
    fun getOffectMinutes(date1: Long, date2: Long): Int {
        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = date1
        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = date2
        val m1 = calendar1[Calendar.MINUTE]
        val m2 = calendar2[Calendar.MINUTE]
        val h = getOffectHour(date1, date2)
        var m = 0
        m = m1 - m2 + h * 60
        return m
    }

    /**
     * 描述：获取本周一.
     *
     * @param format the format
     * @return String String类型日期时间
     */
    fun getFirstDayOfWeek(format: String): String? {
        return getDayOfWeek(format, Calendar.MONDAY)
    }

    /**
     * 描述：获取本周日.
     *
     * @param format the format
     * @return String String类型日期时间
     */
    fun getLastDayOfWeek(format: String): String? {
        return getDayOfWeek(format, Calendar.SUNDAY)
    }

    /**
     * 描述：获取本周的某一天.
     *
     * @param format        the format
     * @param calendarField the calendar field
     * @return String String类型日期时间
     */
    private fun getDayOfWeek(format: String, calendarField: Int): String? {
        var strDate: String? = null
        try {
            val c: Calendar = GregorianCalendar()
            val mSimpleDateFormat = SimpleDateFormat(format)
            val week = c[Calendar.DAY_OF_WEEK]
            if (week == calendarField) {
                strDate = mSimpleDateFormat.format(c.time)
            } else {
                var offectDay = calendarField - week
                if (calendarField == Calendar.SUNDAY) {
                    offectDay = 7 - Math.abs(offectDay)
                }
                c.add(Calendar.DATE, offectDay)
                strDate = mSimpleDateFormat.format(c.time)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strDate
    }

    /**
     * 描述：获取本月第一天.
     *
     * @param format the format
     * @return String String类型日期时间
     */
    fun getFirstDayOfMonth(format: String?): String? {
        var strDate: String? = null
        try {
            val c: Calendar = GregorianCalendar()
            val mSimpleDateFormat = SimpleDateFormat(format)
            //当前月的第一天
            c[GregorianCalendar.DAY_OF_MONTH] = 1
            strDate = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strDate
    }

    /**
     * 描述：获取本月最后一天.
     *
     * @param format the format
     * @return String String类型日期时间
     */
    fun getLastDayOfMonth(format: String?): String? {
        var strDate: String? = null
        try {
            val c: Calendar = GregorianCalendar()
            val mSimpleDateFormat = SimpleDateFormat(format)
            // 当前月的最后一天
            c[Calendar.DATE] = 1
            c.roll(Calendar.DATE, -1)
            strDate = mSimpleDateFormat.format(c.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strDate
    }


    /**
     * 描述：获取表示当前日期的0点时间毫秒数.
     *
     * @return the first time of day
     */
    fun getFirstTimeOfDay(): Long {
        var date: Date? = null
        try {
            val currentDate = getCurrentDate(dateFormatYMD)
            date = getDateByFormat("$currentDate 00:00:00", dateFormatYMDHMS)
            return date!!.time
        } catch (e: Exception) {
        }
        return -1
    }

    /**
     * 描述：获取表示当前日期24点时间毫秒数.
     *
     * @return the last time of day
     */
    fun getLastTimeOfDay(): Long {
        var date: Date? = null
        try {
            val currentDate = getCurrentDate(dateFormatYMD)
            date = getDateByFormat("$currentDate 24:00:00", dateFormatYMDHMS)
            return date!!.time
        } catch (e: Exception) {
        }
        return -1
    }

    /**
     * 描述：判断是否是闰年()
     *
     * (year能被4整除 并且 不能被100整除) 或者 year能被400整除,则该年为闰年.
     *
     * @param year 年代（如2012）
     * @return boolean 是否为闰年
     */
    fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && year % 400 != 0 || year % 400 == 0
    }

    /**
     * 描述：根据时间返回几天前或几分钟的描述.
     *
     * @param strDate the str date
     * @return the string
     */
    fun formatDateStr2Desc(strDate: String?, outFormat: String?): String? {
        val df: DateFormat = SimpleDateFormat(dateFormatYMDHM)
        val c1 = Calendar.getInstance()
        val c2 = Calendar.getInstance()
        try {
            c2.time = df.parse(strDate)
            c1.time = Date()
            val d = getOffectDay(c1.timeInMillis, c2.timeInMillis)
            if (d == 0) {
                val h = getOffectHour(c1.timeInMillis, c2.timeInMillis)
                if (h > 0) {
                    return h.toString() + "小时前"
                } else if (h < 0) {
                    return Math.abs(h).toString() + "小时后"
                } else if (h == 0) {
                    val m = getOffectMinutes(c1.timeInMillis, c2.timeInMillis)
                    return if (m > 0) {
                        m.toString() + "分钟前"
                    } else if (m < 0) {
                        Math.abs(m).toString() + "分钟后"
                    } else {
                        "刚刚"
                    }
                }
            } else if (d > 0) {
                if (d == 1) {
                    return "昨天"
                } else if (d == 2) {
                    return "前天"
                }
            } else if (d < 0) {
                if (d == -1) {
                    return "明天"
                } else if (d == -2) {
                    return "后天"
                }
                return Math.abs(d).toString() + "天后"
            }
            val out = getStringByFormat(strDate, outFormat)
            if (!TextUtils.isEmpty(out)) {
                return out
            }
        } catch (e: Exception) {
        }
        return strDate
    }


    /**
     * 取指定日期为星期几
     *
     * @param strDate  指定日期
     * @param inFormat 指定日期格式
     * @return String   星期几
     */
    fun getWeekNumber(strDate: String?, inFormat: String?): String? {
        var week = "星期日"
        val calendar: Calendar = GregorianCalendar()
        val df: DateFormat = SimpleDateFormat(inFormat)
        try {
            calendar.time = df.parse(strDate)
        } catch (e: Exception) {
            return "错误"
        }
        val intTemp = calendar[Calendar.DAY_OF_WEEK] - 1
        when (intTemp) {
            0 -> week = "星期日"
            1 -> week = "星期一"
            2 -> week = "星期二"
            3 -> week = "星期三"
            4 -> week = "星期四"
            5 -> week = "星期五"
            6 -> week = "星期六"
        }
        return week
    }

    /**
     * 将字符串转位日期类型
     *
     * @param sdate
     * @return
     */
    private fun toDate(sdate: String): Date? {
        return try {
            dateFormater.get().parse(sdate)
        } catch (e: ParseException) {
            null
        }
    }

    /**
     * 以友好的方式显示时间
     *
     * @param ms
     * @return
     */
    fun getfriendlyTime(ms: Long?): String? {
        if (ms == null) return ""
        //      Date time = toDate(sdate);
        val time = Date()
        time.time = ms
        if (time == null) {
            return "Unknown"
        }
        var ftime: String? = ""
        val cal = Calendar.getInstance()

        // 判断是否是同一天
        val curDate = dateFormater2.get().format(cal.time)
        val paramDate = dateFormater2.get().format(time)
        if (curDate == paramDate) {
            val hour = ((cal.timeInMillis - time.time) / 3600000).toInt()
            ftime = if (hour == 0) {
                if ((cal.timeInMillis - time.time) / 60000 < 1) {
                    "刚刚"
                } else {
                    Math.max((cal.timeInMillis - time.time) / 60000, 1).toString() + "分钟前"
                }
            } else {
                hour.toString() + "小时前"
            }
            return ftime
        }
        val lt = time.time / 86400000
        val ct = cal.timeInMillis / 86400000
        val days = (ct - lt).toInt()
        if (days == 0) {
            val hour = ((cal.timeInMillis - time.time) / 3600000).toInt()
            ftime = if (hour == 0) Math.max(
                (cal.timeInMillis - time.time) / 60000, 1
            )
                .toString() + "分钟前" else hour.toString() + "小时前"
        } else if (days == 1) {
            ftime = "昨天"
        } else if (days == 2) {
            ftime = "前天"
        } else if (days > 2 && days <= 10) {
            ftime = days.toString() + "天前"
        } else if (days > 10) {
            ftime = dateFormater2.get().format(time)
        }
        return ftime
    }

    /**
     * 距离当前多少个小时
     *
     * @param dateStr
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    fun getExpiredHour(dateStr: String?): Int {
        var ret = -1
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date: Date
        try {
            date = sdf.parse(dateStr)
            val dateNow = Date()
            val times = date.time - dateNow.time
            ret = if (times > 0) {
                (times / ONE_HOUR_MILLISECONDS).toInt()
            } else {
                -1
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return ret
    }

    /**
     * 过了多少个小时
     * @param dateStr
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    fun getExpiredHour2(dateStr: String?): Int {
        var ret = -1
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val sendDate: Date
        try {
            sendDate = sdf.parse(dateStr)
            val dateNow = Date(System.currentTimeMillis())
            Log.e("JPush", "date=$sendDate")
            val times = dateNow.time - sendDate.time
            Log.e("JPush", "date.getTime()=" + sendDate.time)
            if (times > 0) {
                ret = (times / ONE_HOUR_MILLISECONDS).toInt()
                val sdqf = Math.floor((times / ONE_HOUR_MILLISECONDS).toDouble()).toInt()
            } else {
                ret = -1
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        Log.e("JPush", "ret=$ret")
        return ret
    }


    /**
     * 判断给定字符串时间是否为今日
     *
     * @param sdate
     * @return boolean
     */
    fun isToday(sdate: String): Boolean {
        var b = false
        val time = toDate(sdate)
        val today = Date()
        if (time != null) {
            val nowDate = dateFormater2.get().format(today)
            val timeDate = dateFormater2.get().format(time)
            if (nowDate == timeDate) {
                b = true
            }
        }
        return b
    }

    /**
     * 判断给定字符串时间是否为今日
     *
     * @param sdate
     * @return boolean
     */
    fun isToday(sdate: Long): Boolean {
        var b = false
        val time = Date(sdate)
        val today = Date()
        if (time != null) {
            val nowDate = dateFormater2.get().format(today)
            val timeDate = dateFormater2.get().format(time)
            if (nowDate == timeDate) {
                b = true
            }
        }
        return b
    }

    /**
     * 根据用户生日计算年龄
     */
    fun getAgeByBirthday(birthday: Date?): Int {
        val cal = Calendar.getInstance()
        require(!cal.before(birthday)) { "The birthDay is before Now.It's unbelievable!" }
        val yearNow = cal[Calendar.YEAR]
        val monthNow = cal[Calendar.MONTH] + 1
        val dayOfMonthNow = cal[Calendar.DAY_OF_MONTH]
        cal.time = birthday
        val yearBirth = cal[Calendar.YEAR]
        val monthBirth = cal[Calendar.MONTH] + 1
        val dayOfMonthBirth = cal[Calendar.DAY_OF_MONTH]
        var age = yearNow - yearBirth
        if (monthNow <= monthBirth) {
            if (monthNow == monthBirth) {
                // monthNow==monthBirth
                if (dayOfMonthNow < dayOfMonthBirth) {
                    age--
                }
            } else {
                // monthNow>monthBirth
                age--
            }
        }
        return age
    }

    /**
     * 友好显示时间差
     *
     * @param diff 毫秒
     * @return
     */
    fun getFriendTimeOffer(diff: Long): String? {
        val day = (diff / (24 * 60 * 60 * 1000)).toInt()
        if (day > 0) return day.toString() + "天"
        val time = (diff / (60 * 60 * 1000)).toInt()
        if (time > 0) return time.toString() + "小时"
        val min = (diff / (60 * 1000)).toInt()
        if (min > 0) return min.toString() + "分钟"
        val sec = diff.toInt() / 1000
        return if (sec > 0) sec.toString() + "秒" else "1秒"
    }

    /**
     * 友好的时间间隔
     *
     * @param duration 秒
     * @return
     */
    fun getFriendlyDuration(duration: Long): String? {
        var str = ""
        var tmpDuration = duration
        str += (if (tmpDuration / 60 > 10) tmpDuration / 60 else "0" + tmpDuration / 60).toString() + ":"
        tmpDuration %= 60
        str += if (tmpDuration / 1 >= 10) tmpDuration / 1 else "0" + tmpDuration / 1
        tmpDuration %= 1
        return str
    }

    /**
     * 友好的时间间隔2
     *
     * @param duration 秒
     * @return
     */
    fun getFriendlyDuration2(duration: Long): String {
        var str = ""
        var tmpDuration = duration
        str += if (tmpDuration / 60 > 0) (tmpDuration / 60).toString() + "'" else ""
        tmpDuration %= 60
        str += if (tmpDuration / 1 >= 10) (tmpDuration / 1).toString() + "''" else "0" + tmpDuration / 1 + "''"
        tmpDuration %= 1
        return str
    }

    fun getFriendlyMusicDuration(duration: Long): String? {
        var str = "-"
        var tmpDuration = (duration / 1000).toInt() //秒
        str += (if (tmpDuration / 3600 > 10) tmpDuration / 3600 else "0" + tmpDuration / 3600).toString() + ":"
        tmpDuration %= 3600
        str += (if (tmpDuration / 60 > 10) tmpDuration / 60 else "0" + tmpDuration / 60).toString() + ":"
        tmpDuration %= 60
        str += if (tmpDuration / 1 >= 10) tmpDuration / 1 else "0" + tmpDuration / 1
        tmpDuration %= 1
        return str
    }

    /**
     * 通过日期来确定星座
     *
     * @param mouth
     * @param day
     * @return
     */
    fun getStarSeat(mouth: Int, day: Int): String? {
        var starSeat: String? = null
        starSeat = if (mouth == 3 && day >= 21 || mouth == 4 && day <= 19) {
            "白羊座"
        } else if (mouth == 4 && day >= 20 || mouth == 5 && day <= 20) {
            "金牛座"
        } else if (mouth == 5 && day >= 21 || mouth == 6 && day <= 21) {
            "双子座"
        } else if (mouth == 6 && day >= 22 || mouth == 7 && day <= 22) {
            "巨蟹座"
        } else if (mouth == 7 && day >= 23 || mouth == 8 && day <= 22) {
            "狮子座"
        } else if (mouth == 8 && day >= 23 || mouth == 9 && day <= 22) {
            "处女座"
        } else if (mouth == 9 && day >= 23 || mouth == 10 && day <= 23) {
            "天秤座"
        } else if (mouth == 10 && day >= 24 || mouth == 11 && day <= 22) {
            "天蝎座"
        } else if (mouth == 11 && day >= 23 || mouth == 12 && day <= 21) {
            "射手座"
        } else if (mouth == 12 && day >= 22 || mouth == 1 && day <= 19) {
            "摩羯座"
        } else if (mouth == 1 && day >= 20 || mouth == 2 && day <= 18) {
            "水瓶座"
        } else {
            "双鱼座"
        }
        return starSeat
    }

    /**
     * 返回聊天时间
     * @return
     */
    fun getChatTimeForShow(time: Long): String? {
        return if (isToday(time)) {
            getStringByFormat(time, dateFormatHMofChinese)
        } else {
            getStringByFormat(time, dateFormatMDHMofChinese)
        }
    }

    /**
     * 获取指定时间的毫秒值
     */
    fun getDatelongMills(fomat: String?, dateStr: String?): Long {
        val sdf = SimpleDateFormat(fomat)
        var date: Date? = null
        try {
            date = sdf.parse(dateStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return date!!.time
    }

    /**
     * 两个日期比较
     * @param DATE1
     * @param DATE2
     * @return
     */
    fun compare_date(DATE1: String?, DATE2: String?): Int {
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        try {
            val dt1: Date = df.parse(DATE1)
            val dt2: Date = df.parse(DATE2)
            return if (dt1.time - dt2.time > 0) { //date1>date2
                1
            } else {
                -1
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return 0
    }

    /**
     *
     * 给时间加几个小时
     */
    fun addDateMinut(day: String?, hour: Int): String? {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        var date: Date? = null
        try {
            date = format.parse(day)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        if (date == null) return ""
        println("front:" + format.format(date)) //显示输入的日期
        var cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.HOUR, hour) // 24小时制
        date = cal.time
        println("after:" + format.format(date)) //显示更新后的日期
        cal = null
        return format.format(date)
    }


}