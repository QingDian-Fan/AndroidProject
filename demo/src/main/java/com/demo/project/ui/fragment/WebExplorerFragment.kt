package com.demo.project.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.common.theme.NightMode
import com.common.theme.NightModeManager
import com.common.ui.BaseAppBindFragment
import com.common.utils.InputMethodUtils
import com.common.utils.ext.showAllowStateLoss
import com.common.weight.webview.bean.WebDataEntry
import com.common.weight.webview.callback.IWebMenuListener
import com.common.weight.webview.callback.LoadProgressCallBack
import com.common.weight.webview.callback.WebViewCallBack
import com.common.weight.webview.storage.CollectWebPageUtil
import com.common.weight.webview.storage.WebBookMarkUtil
import com.common.weight.webview.storage.WebHistoryUtil
import com.demo.project.R
import com.demo.project.databinding.FragmentH5ContainerBinding
import com.demo.project.ui.dialog.WebMenuDialog
import com.demo.project.ui.dialog.WebShareDialog
import com.demo.project.web.WebViewDarkModeHelper
import com.demo.project.web.WebViewDarkModeHelper.Mode
import androidx.core.net.toUri

class WebExplorerFragment : BaseAppBindFragment<FragmentH5ContainerBinding>(), WebViewCallBack {

    companion object {
        const val KEY_URL_DATA = "KEY_URL_DATA"
        private const val EXTRA_URL = "urlString"
        private const val DEFAULT_URL = "https://www.wanandroid.com"

        @JvmStatic
        fun getFragment() = WebExplorerFragment()
    }

    private var currentTitleString: String = ""
    private var currentUrlString: String = ""
    private var isCollectChangeFromPage = false

    override fun getLayoutId(): Int = R.layout.fragment_h5_container

    override fun initialize(savedInstanceState: Bundle?) {
        when(NightModeManager.currentMode){
            NightMode.FOLLOW_SYSTEM->{
                activity?.let {
                    WebViewDarkModeHelper.apply(it,binding.webView,Mode.FOLLOW_SYSTEM)
                }
            }
            NightMode.LIGHT->{
                activity?.let {
                    WebViewDarkModeHelper.apply(it,binding.webView,Mode.LIGHT)
                }
            }
            NightMode.NIGHT->{
                activity?.let {
                    WebViewDarkModeHelper.apply(it,binding.webView,Mode.NIGHT)
                }
            }
        }
        parentFragmentManager.setFragmentResultListener(KEY_URL_DATA, viewLifecycleOwner) { _, bundle ->
            initView()
            initData(bundle.getString(EXTRA_URL).orEmpty().ifBlank { DEFAULT_URL })
        }
    }

    private fun initData(urlString: String) {
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

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        activity?.finish()
                    }
                }
            }
        )
    }

    private fun initView() {
        binding.cvCollect.setOnCheckedChangeListener { _, isChecked ->
            if (isCollectChangeFromPage) return@setOnCheckedChangeListener
            val webEntry = currentWebEntry() ?: return@setOnCheckedChangeListener
            if (isChecked) {
                CollectWebPageUtil.collectWebPage(webEntry)
            } else {
                CollectWebPageUtil.removeCollectWebPage(webEntry)
            }
        }
        binding.ivMenu.setOnClickListener {
            val dialog = WebMenuDialog.getDialog()
            dialog.setListener(object : IWebMenuListener {
                override fun onHome() {
                    var step = 0
                    while (binding.webView.canGoBackOrForward(step - 1)) step--
                    binding.webView.goBackOrForward(step)
                    dialog.dismissAllowingStateLoss()
                }

                override fun onTop() {
                    binding.webView.goTop()
                    dialog.dismissAllowingStateLoss()
                }

                override fun onRefresh() {
                    binding.webView.reload()
                    dialog.dismissAllowingStateLoss()
                }

                override fun onClose() {
                    activity?.finish()
                    dialog.dismissAllowingStateLoss()
                }

                override fun onCollect() {
                    if (binding.webView.title?.isNotEmpty() == true && binding.webView.url?.isNotEmpty() == true) {
                        val webEntry = WebDataEntry(
                            binding.webView.title,
                            binding.webView.url,
                            System.currentTimeMillis()
                        )
                        CollectWebPageUtil.collectWebPage(webEntry)
                    }
                    dialog.dismissAllowingStateLoss()
                }

                override fun onMark() {
                    if (binding.webView.title?.isNotEmpty() == true && binding.webView.url?.isNotEmpty() == true) {
                        val webEntry = WebDataEntry(
                            binding.webView.title,
                            binding.webView.url,
                            System.currentTimeMillis()
                        )
                        WebBookMarkUtil.markWebPage(webEntry)
                    }
                    dialog.dismissAllowingStateLoss()
                }

                override fun onShare() {
                    binding.webView.getShareData { url, covers, title, desc ->
                        WebShareDialog.getDialog(
                            url,
                            covers.toMutableList(),
                            title,
                            desc
                        ).showAllowStateLoss(childFragmentManager, "")
                    }
                    dialog.dismissAllowingStateLoss()
                }

                override fun onSetting() {
                    showToast("功能开发中")
                    dialog.dismissAllowingStateLoss()
                }
            })
            dialog.showAllowStateLoss(childFragmentManager, "")
        }
        binding.ivBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                activity?.finish()
            }
        }
        binding.ivForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }
        binding.ivInto.setOnClickListener {
            val url = binding.etTitle.text.toString().trim()
            if (url.isNotEmpty()) {
                val uri = url.toUri()
                if (uri.scheme == "http" || uri.scheme == "https") {
                    binding.webView.loadUrl(url)
                }
            }
            binding.etTitle.clearFocus()
        }
        binding.etTitle.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    binding.ivInto.performClick()
                    return true
                }
                return false
            }
        })
        binding.etTitle.setOnFocusChangeListener { _, hasFocus ->
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
        } else if (!TextUtils.isEmpty(binding.webView.title)) {
            binding.etTitle.setText(binding.webView.title)
        } else {
            binding.etTitle.setText(binding.webView.url)
        }
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
        currentWebEntry()?.let { webEntry ->
            if (!binding.webView.canGoBack()) {
                WebHistoryUtil.putWebHistory(webEntry)
            }
            binding.cvCollect.post {
                isCollectChangeFromPage = true
                binding.cvCollect.isChecked = CollectWebPageUtil.isCollectedWebPage(webEntry)
                isCollectChangeFromPage = false
            }
        }
        binding.ivBack.setImageResource(
            if (binding.webView.canGoBack()) R.drawable.ic_web_back else R.drawable.ic_web_close
        )
        switchIconEnable(binding.ivForward, binding.webView.canGoForward())
        updateTitle()
    }

    private fun switchIconEnable(view: View, enable: Boolean) {
        view.isEnabled = enable
        view.alpha = if (enable) 1.0f else 0.382f
    }

    override fun pageError() = Unit

    override fun updateTitle(title: String?) {
        if (!TextUtils.isEmpty(title)) {
            currentTitleString = title.orEmpty()
            updateTitle()
        }
    }

    override fun overrideUrlLoading(view: WebView?, url: WebResourceRequest?): Boolean {
        val targetUrl = url?.url?.toString().orEmpty()
        val uri = targetUrl.toUri()
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

    override fun onDestroyView() {
        binding.webView.apply {
            stopLoading()
            webChromeClient = null
            destroy()
        }
        super.onDestroyView()
    }

    private fun currentWebEntry(): WebDataEntry? {
        val title = binding.webView.title.orEmpty()
        val url = binding.webView.url.orEmpty()
        if (title.isEmpty() || url.isEmpty()) return null
        return WebDataEntry(title, url, System.currentTimeMillis())
    }
}
