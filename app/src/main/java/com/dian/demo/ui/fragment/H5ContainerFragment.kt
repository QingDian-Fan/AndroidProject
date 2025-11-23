package com.dian.demo.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.net.toUri
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentH5ContainerBinding
import com.dian.demo.ui.dialog.WebMenuDialog
import com.dian.demo.utils.InputMethodUtils
import com.dian.demo.utils.ext.showAllowStateLoss
import com.dian.demo.utils.webview.callback.LoadProgressCallBack
import com.dian.demo.utils.webview.callback.WebViewCallBack

class H5ContainerFragment : BaseAppBindFragment<FragmentH5ContainerBinding>(),WebViewCallBack {
    private var currentTitleString: String? = null
    private var currentUrlString: String? = null

    override fun getLayoutId(): Int  = R.layout.fragment_h5_container

    override fun initialize(savedInstanceState: Bundle?) {
        initView()
        initData()

    }

    private fun initData() {
        binding.webView.loadUrl("https://www.wanAndroid.com")
        binding.webView.initWebClient(this)
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

    private fun initView() {
        binding.ivMenu.setOnClickListener {
            WebMenuDialog.getDialog().showAllowStateLoss(childFragmentManager,"")
        }
        binding.ivBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                activity?.finish()
            }
        }
        binding.ivForward.setOnClickListener{
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }
        binding.ivInto.setOnClickListener {
            val url = binding.etTitle.text.toString()
            if (!TextUtils.isEmpty(url)) {
                val uri = url.toUri()
                if (TextUtils.equals(
                        uri.scheme,
                        "http"
                    ) || TextUtils.equals(uri.scheme, "https")
                ) {
                    binding.webView.loadUrl(url)
                }
            }
            binding.etTitle.clearFocus()
        }
        binding.etTitle.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    binding.ivInto.performClick()
                    return true
                }
                return false
            }
        })
        binding.etTitle.setOnFocusChangeListener { v, hasFocus ->
            updateTitle()
            if (hasFocus) {
                InputMethodUtils.show(binding.etTitle)
            } else {
                InputMethodUtils.hide(binding.etTitle)
            }
        }
    }

    private fun updateTitle() {
        binding.etTitle.tag = binding.webView.url
        if (binding.etTitle.hasFocus()) {
            binding.etTitle.setText(binding.webView.url)
        } else {
            if (!TextUtils.isEmpty(binding.webView.title)) {
                binding.etTitle.setText(binding.webView.title)
            } else {
                binding.etTitle.setText(binding.webView.url)
            }
        }
    }

    override fun pageStarted(url: String?) {

    }

    override fun pageFinished(url: String?) {
        if (binding.webView.canGoBack()) {
            binding.ivBack.setImageResource(R.mipmap.ic_back)
        } else {
            binding.ivBack.setImageResource(R.mipmap.ic_close)
        }
        switchIconEnable(binding.ivForward, binding.webView.canGoForward())
    }
    private fun switchIconEnable(view: View, enable: Boolean) {
        if (enable) {
            view.isEnabled = true
            view.alpha = 1.0f
        } else {
            view.isEnabled = false
            view.alpha = 0.382f
        }
    }

    override fun pageError() {

    }

    override fun updateTitle(title: String?) {
        if (title != null && !TextUtils.isEmpty(title)) {
            updateTitle()
            currentTitleString = title
        }
    }

    override fun overrideUrlLoading(view: WebView?, url: WebResourceRequest?): Boolean {
        val uri = url?.url.toString().toUri()
        return if ("http" == uri.scheme || "https" == uri.scheme) {
            view?.loadUrl(url?.url.toString())
            currentUrlString = url?.url.toString()
            false
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW, url?.url.toString().toUri())
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        }
    }
    companion object {

        @JvmStatic
        fun getFragment() = H5ContainerFragment()
    }
}