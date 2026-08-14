package com.demo.project.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import com.common.weight.webview.callback.IWebMenuListener
import com.demo.project.R
import com.demo.project.databinding.DialogWebMenuBinding


class WebMenuDialog : AppCompatDialogFragment() {
    companion object {
        fun getDialog(): WebMenuDialog {
            val dialog = WebMenuDialog()
            val bundle = Bundle()
            dialog.arguments = bundle
            return dialog
        }
    }

    private var listener: IWebMenuListener? = null
    fun setListener(listener: IWebMenuListener) {
        this.listener = listener
    }

    private lateinit var binding: DialogWebMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        binding = DialogWebMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.dialogWebMenuIvGoTop.setOnClickListener {
            listener?.onTop()
        }

        binding.dialogWebMenuIvHome.setOnClickListener {
            listener?.onHome()
        }
        binding.dialogWebMenuIvCloseActivity.setOnClickListener {
            listener?.onClose()
        }
        binding.dialogWebMenuIvRefresh.setOnClickListener {
            listener?.onRefresh()
        }
        binding.dialogWebMenuIvDismiss.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.dialogWebMenuIvCollect.setOnClickListener {
            listener?.onCollect()
        }
        binding.dialogWebMenuIvReadLater.setOnClickListener {
            listener?.onMark()
        }
        binding.dialogWebMenuIvShare.setOnClickListener {
            listener?.onShare()
        }
        binding.dialogWebMenuIvSetting.setOnClickListener {
            listener?.onSetting()
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.setCanceledOnTouchOutside(true)
            it.window?.run {
                // 与弹窗面板同色，随日夜间模式切换，避免夜间模式下底部导航栏仍为白色
                // bg_common 定义在 lib_common-theme，非传递 R 类下需全限定引用
                navigationBarColor =
                    ContextCompat.getColor(context, com.common.theme.R.color.bg_common)

                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                setBackgroundDrawableResource(R.color.transparent)
                val params = attributes
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                attributes = params
            }
        }
    }
}