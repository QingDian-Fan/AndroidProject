package com.dian.demo.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.dian.demo.utils.webview.callback.LoadProgressCallBack
import com.dian.demo.utils.webview.callback.WebViewCallBack
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentWebBinding


//https://github.com/liyihuanx/NewWebView
class WebFragment : BaseAppBindFragment<FragmentWebBinding>(), WebViewCallBack {

    private lateinit var urlString: String
    private var currentUrlString: String? = null
    private var currentTitleString: String? = null

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"
        fun getFragment(urlString: String): WebFragment {
            val bundle = Bundle()
            bundle.putString(EXTRA_URL, urlString)
            val fragment = WebFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            urlString = it.getString(EXTRA_URL).toString()
        }
    }

    override fun getLayoutId(): Int = R.layout.fragment_web


    override fun initialize(savedInstanceState: Bundle?) {}

    override fun lazyInit() {
        binding.webView.initWebClient(this)
        binding.webView.loadUrl(urlString)
        currentUrlString = urlString
        binding.webView.getChromeClient()?.setLoadProgressCallBack(object : LoadProgressCallBack {
            override fun onCurrentProgress(currentProgress: Int) {
                if (currentProgress == 100) {
                    binding.progressBar.visibility = gone
                } else {
                    binding.progressBar.visibility = visible
                    binding.progressBar.progress = currentProgress
                }
            }
        })
    }

    /*override fun onResume() {
        super.onResume()
       if (getLazyState()) binding.webView.reload()
    }*/
    override fun pageStarted(url: String?) {

    }

    override fun pageFinished(url: String?) {

    }

    override fun pageError() {

    }

    override fun updateTitle(title: String?) {
        if (title != null && !TextUtils.isEmpty(title)) {
            setPageTitle(title)
            currentTitleString = title
        }
    }

    override fun overrideUrlLoading(view: WebView?, url: WebResourceRequest?): Boolean {
        val uri = Uri.parse(url?.url.toString())
        return if ("http" == uri.scheme || "https" == uri.scheme) {
            view?.loadUrl(url?.url.toString())
            currentUrlString = url?.url.toString()
            false
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url?.url.toString()))
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        }
    }

    fun getCurrentUrlString(): String = currentUrlString!!

    fun getCurrentTitleString(): String = currentTitleString!!

    fun canGoBack(): Boolean = binding.webView.canGoBack()

    fun doActionBack() = binding.webView.goBack()


}