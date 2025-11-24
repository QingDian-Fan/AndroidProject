package com.dian.demo.utils.webview.webview

import android.content.Context
import android.util.AttributeSet

class BrowserWebView: BaseWebView{
    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet?): super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int): super(
        context,
        attributeSet,
        defStyleAttr
    )

    fun goTop() {
        val js = "javascript:(" +
                "function(){\n" +
                "  var timer = null;\n" +
                "  cancelAnimationFrame(timer);\n" +
                "  var startTime = +new Date();     \n" +
                "  var b = document.body.scrollTop || document.documentElement.scrollTop;\n" +
                "  var d = 500;\n" +
                "  var c = b;\n" +
                "  var timer = requestAnimationFrame(function func(){\n" +
                "    var t = d - Math.max(0,startTime - (+new Date()) + d);\n" +
                "    document.documentElement.scrollTop = document.body.scrollTop = t * (-c) / d + b;\n" +
                "    timer = requestAnimationFrame(func);\n" +
                "    if(t == d){\n" +
                "      cancelAnimationFrame(timer);\n" +
                "    }\n" +
                "  });\n" +
                "}" +
                ")()"
        this.evaluateJavascript(js, null)
    }
}