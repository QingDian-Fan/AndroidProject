package com.dian.demo.utils.webview.webview

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.webkit.ValueCallback
import com.dian.demo.utils.webview.callback.IShareCallBack
import org.json.JSONException
import org.json.JSONObject

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

    fun getShareData(callback: IShareCallBack) {
        val js = "javascript:(" +
                "function getShareInfo() {" +
                "  var map = {};" +
                "  map[\"title\"] = document.title;" +
                "  map[\"desc\"] = document.querySelector('meta[name=\"description\"]').getAttribute('content');" +
                "  var imgElements = document.getElementsByTagName(\"img\");" +
                "  var imgs = [];" +
                "  for(var i = 0 ; i < imgElements.length; i++){" +
                "    var imgEle = imgElements[i];" +
                "    var w = imgEle.naturalWidth;" +
                "    var h = imgEle.naturalHeight;" +
                "    if(w > 200 && h > 100){" +
                "      imgs.push(imgEle.src);" +
                "    }" +
                "  }" +
                "  map[\"imgs\"] = imgs;" +
                "  return map;" +
                "}" +
                ")()"
        this.evaluateJavascript(js) { s ->
            var title = ""
            var desc = ""
            val imgs: MutableList<String?> = ArrayList<String?>()
            try {
                val jsonObject = JSONObject(s)
                title = jsonObject.optString("title")
                desc = jsonObject.optString("desc")
                val imgArr = jsonObject.optJSONArray("imgs")
                if (imgArr != null) {
                    for (i in 0..<imgArr.length()) {
                        val img = imgArr.optString(i)
                        if (!TextUtils.isEmpty(img)) {
                            if (!imgs.contains(img)) {
                                imgs.add(img)
                            }
                        }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            if (TextUtils.isEmpty(title)) {
                title = getTitle()!!
            }
            if (TextUtils.isEmpty(desc)) {
                desc = getUrl()!!
            }
            callback.onShareData(getUrl()?:"", imgs, title, desc)
        }
    }
}