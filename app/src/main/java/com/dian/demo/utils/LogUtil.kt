package com.dian.demo.utils

import android.util.Log


object LogUtil {
    @JvmField
    var tag = "TAG--->"

    @JvmField
    var isDebug = true

    var isWriteFile = true

    private lateinit var className: String
    private lateinit var methodName: String
    private var lineNumber: Int = 0

    private fun getMethodNames(sElements: Array<StackTraceElement>) {
        className = sElements[1].fileName ?: "null"
        methodName = sElements[1].methodName ?: "null"
        lineNumber = sElements[1].lineNumber
    }

    @JvmStatic
    fun i(message: String) {
        i(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun d(message: String) {
        d(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun v(message: String) {
        v(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun w(message: String) {
        w(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun e(message: String) {
        e(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        i(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        d(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun v(tag: String, message: String) {
        v(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        w(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun e(tag: String, message: String) {
        e(Throwable().stackTrace, tag, message)
    }

    @JvmStatic
    fun i(sElements: Array<StackTraceElement>, tag: String, message: String) {
        if (isDebug || isWriteFile) {
            getMethodNames(sElements)
            if (isDebug) {
                Log.i(tag, "($className#$methodName()#$lineNumber)  $message")
            }
            if (isWriteFile) {
                LogFileUtil.writeMessage("($className#$methodName()#$lineNumber)  $message")
            }
        }
    }

    @JvmStatic
    fun d(sElements: Array<StackTraceElement>, tag: String, message: String) {
        if (isDebug || isWriteFile) {
            getMethodNames(sElements)
            if (isDebug) {
                Log.d(tag, "($className#$methodName()#$lineNumber)  $message")
            }
            if (isWriteFile) {
                LogFileUtil.writeMessage("($className#$methodName()#$lineNumber)  $message")
            }
        }
    }

    @JvmStatic
    fun v(sElements: Array<StackTraceElement>, tag: String, message: String) {
        if (isDebug || isWriteFile) {
            getMethodNames(sElements)
            if (isDebug) {
                Log.v(tag, "($className#$methodName()#$lineNumber)  $message")
            }
            if (isWriteFile) {
                LogFileUtil.writeMessage("($className#$methodName()#$lineNumber)  $message")
            }
        }
    }

    @JvmStatic
    fun w(sElements: Array<StackTraceElement>, tag: String, message: String) {
        if (isDebug || isWriteFile) {
            getMethodNames(sElements)
            if (isDebug) {
                Log.w(tag, "($className#$methodName()#$lineNumber)  $message")
            }
            if (isWriteFile) {
                LogFileUtil.writeMessage("($className#$methodName()#$lineNumber)  $message")
            }
        }
    }

    @JvmStatic
    fun e(sElements: Array<StackTraceElement>, tag: String, message: String) {
        if (isDebug || isWriteFile) {
            getMethodNames(sElements)
            if (isDebug) {
                Log.e(tag, "($className#$methodName()#$lineNumber)  $message")
            }
            if (isWriteFile) {
                LogFileUtil.writeMessage("($className#$methodName()#$lineNumber)  $message")
            }
        }
    }


    @JvmStatic
    fun e(className: String, methodName: String, message: String?) {
        e("$className#$methodName()", message ?: "")
    }

    @JvmStatic
    fun printStackTrace(throwable: Throwable) {
        if (isDebug) throwable.printStackTrace()
    }

    @JvmStatic
    fun printStackTrace(tag: String) {
        if (!isDebug) return

        for (stackTraceElement in Thread.currentThread().stackTrace) {
            d(tag, stackTraceElement.toString())
        }
    }
}