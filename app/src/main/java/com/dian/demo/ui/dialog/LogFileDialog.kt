package com.dian.demo.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.dian.demo.databinding.DialogLogFileBinding
import com.project.common.utils.ExceptionHandlerUtil
import com.project.common.utils.LogFileUtil

class LogFileDialog: AppCompatDialogFragment() {
    companion object {
        fun getDialog(): AppCompatDialogFragment {
            val dialog = LogFileDialog()
            val bundle = Bundle()
            dialog.arguments = bundle
            return dialog
        }
    }

    private lateinit var binding: DialogLogFileBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DialogLogFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initData()
    }

    private fun initData() {
        binding.tvLog.setOnClickListener {
            LogFileUtil.doShareLogFile()
            dismissAllowingStateLoss()
        }
        binding.tvException.setOnClickListener {
            ExceptionHandlerUtil.doShareExceptionFile()
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.setCanceledOnTouchOutside(true)
            it.window?.run {
                navigationBarColor = Color.WHITE
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                setBackgroundDrawableResource(android.R.color.transparent)
                val params = attributes
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                attributes = params
            }
        }
    }


}