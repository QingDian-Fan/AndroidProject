package com.demo.project.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.common.ui.BaseAppBindFragment
import com.common.weight.webview.callback.LoadProgressCallBack
import com.common.weight.webview.callback.WebViewCallBack
import com.demo.project.R
import com.demo.project.databinding.FragmentWebBinding

class WebFragment : BaseAppBindFragment<FragmentWebBinding>(), WebViewCallBack {

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"

        @JvmStatic
        fun getFragment(urlString: String): WebFragment {
            return WebFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_URL, urlString)
                }
            }
        }
    }

    private var urlString: String = ""
    private var currentUrlString: String = ""
    private var currentTitleString: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        urlString = arguments?.getString(EXTRA_URL).orEmpty()
    }

    override fun getLayoutId(): Int = R.layout.fragment_web

    override fun initialize(savedInstanceState: Bundle?) = Unit

    override fun lazyInit() {
        binding.webView.initWebClient(this)
        binding.webView.getChromeClient()?.setLoadProgressCallBack(
            LoadProgressCallBack { currentProgress ->
                if (currentProgress >= 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = currentProgress
                }
            }
        )
        currentUrlString = urlString
        binding.webView.loadUrl(urlString)
    }

    override fun onDestroyView() {
        binding.webView.apply {
            stopLoading()
            webChromeClient = null
            destroy()
        }
        super.onDestroyView()
    }

    override fun pageStarted(url: String?) {
        if (!url.isNullOrEmpty()) {
            currentUrlString = url
        }
    }

    override fun pageFinished(url: String?) {
        if (!url.isNullOrEmpty()) {
            currentUrlString = url
        }
    }

    override fun pageError() = Unit

    override fun updateTitle(title: String?) {
        if (!TextUtils.isEmpty(title)) {
            currentTitleString = title.orEmpty()
            setPageTitle(currentTitleString)
        }
    }

    override fun overrideUrlLoading(view: WebView?, url: WebResourceRequest?): Boolean {
        val targetUrl = url?.url?.toString().orEmpty()
        val uri = Uri.parse(targetUrl)
        return if (uri.scheme == "http" || uri.scheme == "https") {
            currentUrlString = targetUrl
            view?.loadUrl(targetUrl)
            false
        } else {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            true
        }
    }

    fun getCurrentUrlString(): String = currentUrlString

    fun getCurrentTitleString(): String = currentTitleString

    fun canGoBack(): Boolean = binding.webView.canGoBack()

    fun doActionBack() = binding.webView.goBack()

    fun getShareData(callback: (url: String, covers: List<String>, title: String, desc: String) -> Unit) {
        binding.webView.getShareData(callback)
    }
}