package com.common.weight.webview.webview

import android.content.Context
import android.util.AttributeSet
import com.common.utils.LogUtil
import com.common.weight.webview.callback.IShareCallBack
import org.json.JSONException
import org.json.JSONObject

/**
 * 在 [BaseWebView] 基础上额外提供浏览器场景的工具方法：
 *
 *  - [goTop]        平滑回到顶部；
 *  - [getShareData] 抽取标题/描述/封面图，回调给分享面板。
 */
class BrowserWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : BaseWebView(context, attrs, defStyleAttr) {

    /** 通过 `requestAnimationFrame` 在 500ms 内平滑回到顶部。 */
    fun goTop() {
        evaluateJavascript(JS_GO_TOP, null)
    }

    /**
     * 抽取页面分享信息。`title`/`desc` 取 `meta[name="description"]`，若缺失则回落到
     * `document.title` 与当前 url；`covers` 收集自然尺寸大于 200x100 的 `<img>`。
     */
    fun getShareData(callback: IShareCallBack) {
        evaluateJavascript(JS_SHARE_INFO) { raw ->
            var title = ""
            var desc = ""
            val covers = mutableListOf<String>()
            try {
                val json = JSONObject(raw ?: "{}")
                title = json.optString("title")
                desc = json.optString("desc")
                json.optJSONArray("imgs")?.let { array ->
                    for (i in 0 until array.length()) {
                        val img = array.optString(i)
                        if (img.isNotEmpty() && img !in covers) covers.add(img)
                    }
                }
            } catch (e: JSONException) {
                LogUtil.e(TAG, "parse share info failed: ${e.message}")
            }
            if (title.isEmpty()) title = getTitle().orEmpty()
            val pageUrl = getUrl().orEmpty()
            if (desc.isEmpty()) desc = pageUrl
            callback.onShareData(pageUrl, covers, title, desc)
        }
    }

    private companion object {
        const val TAG = "WebView"

        // 注意：JS 内对 description meta 与 querySelector 做了 null-safe，避免抛异常时
        // evaluateJavascript 拿到 "null" 导致 JSONObject 解析失败。
        const val JS_GO_TOP = """
            javascript:(function(){
              var timer = null;
              cancelAnimationFrame(timer);
              var startTime = +new Date();
              var b = document.body.scrollTop || document.documentElement.scrollTop;
              var d = 500;
              var c = b;
              timer = requestAnimationFrame(function func(){
                var t = d - Math.max(0, startTime - (+new Date()) + d);
                document.documentElement.scrollTop = document.body.scrollTop = t * (-c) / d + b;
                timer = requestAnimationFrame(func);
                if (t == d) cancelAnimationFrame(timer);
              });
            })()
        """

        const val JS_SHARE_INFO = """
            javascript:(function(){
              var map = {};
              map["title"] = document.title || "";
              var descMeta = document.querySelector('meta[name="description"]');
              map["desc"] = descMeta ? (descMeta.getAttribute('content') || "") : "";
              var imgElements = document.getElementsByTagName("img");
              var imgs = [];
              for (var i = 0; i < imgElements.length; i++) {
                var imgEle = imgElements[i];
                var w = imgEle.naturalWidth;
                var h = imgEle.naturalHeight;
                if (w > 200 && h > 100) imgs.push(imgEle.src);
              }
              map["imgs"] = imgs;
              return map;
            })()
        """
    }
}
