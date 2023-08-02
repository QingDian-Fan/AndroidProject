package com.dian.demo.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.AppCompatTextView
import com.dian.demo.R
import com.dian.demo.utils.SchemaUtil
import com.dian.demo.utils.ext.singleClick

class ConfirmDialog : AppCompatDialogFragment() {

    companion object{
        fun getDialog(titleString: String, contentString: String, urlString: String): AppCompatDialogFragment {
            val dialog = ConfirmDialog()
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
        val mView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm, null, false)
        initView(mView)
        return mView
    }

    private fun initView(mView: View) {
        val tvTitle: AppCompatTextView by lazy { mView.findViewById(R.id.tv_title) }
        val tvContent: AppCompatTextView by lazy { mView.findViewById(R.id.tv_content) }
        val tvCancel: AppCompatTextView by lazy { mView.findViewById(R.id.tv_cancel) }
        val tvConfirm: AppCompatTextView by lazy { mView.findViewById(R.id.tv_confirm) }
        val title = arguments?.getString("title")
        val content = arguments?.getString("content")
        val url = arguments?.getString("url")
        tvTitle.text = title
        tvContent.text = content
        tvCancel.singleClick {
            dismissAllowingStateLoss()
        }
        tvConfirm.singleClick {
            SchemaUtil.schemaToPage(requireContext(), url!!)
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