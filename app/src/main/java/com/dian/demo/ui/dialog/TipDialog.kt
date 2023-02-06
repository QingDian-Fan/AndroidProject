package com.dian.demo.ui.dialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import com.dian.demo.R
import com.dian.demo.ui.activity.DemoActivity
import com.dian.demo.utils.SchemaUtil
import com.dian.demo.utils.ext.singleClick

class TipDialog : AppCompatDialogFragment() {

    companion object {
        fun getDialog(titleString: String,contentString: String,urlString: String?=null): AppCompatDialogFragment {
            val dialog = TipDialog()
            val bundle = Bundle()
            bundle.putString("title", titleString)
            bundle.putString("content", contentString)
            bundle.putString("url", urlString)
            dialog.arguments = bundle
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        val mView = LayoutInflater.from(context).inflate(R.layout.dialog_tip, null, false)
        initView(mView)
        return mView
    }

    private fun initView(mView: View) {
        val tvTitle: AppCompatTextView by lazy { mView.findViewById(R.id.tv_title) }
        val tvContent: AppCompatTextView by lazy { mView.findViewById(R.id.tv_content) }
        val tvConfirm: AppCompatTextView by lazy { mView.findViewById(R.id.tv_confirm) }

        val title = arguments?.getString("title")
        val content = arguments?.getString("content")
        val url = arguments?.getString("url")
        tvTitle.text = title
        tvContent.text = content
        tvConfirm.singleClick {
            if (url != null && !TextUtils.isEmpty(url)) {
                SchemaUtil.schemaToPage(requireContext(), url)
            }
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog!!.setCanceledOnTouchOutside(false)
            if (dialog!!.window != null) {
                dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }
}