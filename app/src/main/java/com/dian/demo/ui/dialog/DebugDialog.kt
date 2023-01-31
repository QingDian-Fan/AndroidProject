package com.dian.demo.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import com.dian.demo.R
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SchemaUtil
import com.dian.demo.utils.ext.singleClick


class DebugDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val mView: View = LayoutInflater.from(context).inflate(R.layout.dialog_debug, null, false)
        dialog.setContentView(mView)
        initView(mView)
        return dialog
    }


    private fun initView(mView: View) {
        val mTvCancel = mView.findViewById<AppCompatTextView>(R.id.tv_cancel)
        val mTvConfirm = mView.findViewById<AppCompatTextView>(R.id.tv_confirm)
        val mTvTitle = mView.findViewById<AppCompatTextView>(R.id.tv_title)
        val mEtInput = mView.findViewById<AppCompatEditText>(R.id.et_input)
        mTvTitle.text = ResourcesUtil.getString(R.string.text_schema)
        mEtInput.setText("dian://webview?link_url=https://wanandroid.com/")
        mTvConfirm.singleClick {
            if (!TextUtils.isEmpty(mEtInput.text.toString().trim())) {
                SchemaUtil.schemaToPage(requireContext(),mEtInput.text.toString().trim())
            }
        }
        mTvCancel.singleClick { dismissAllowingStateLoss() }
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