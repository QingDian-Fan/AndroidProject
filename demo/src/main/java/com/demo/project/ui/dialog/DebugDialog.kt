package com.demo.project.ui.dialog

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import com.common.utils.ResourcesUtil
import com.common.utils.ext.singleClick
import com.demo.project.utils.scheme.SchemeUtils
import com.demo.project.R
import com.demo.project.constants.ANDROID_ASSET_URI


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
        mEtInput.setText("demo://web?link_url=https://www.wanandroid.com")
        mTvConfirm.singleClick {
            if (!TextUtils.isEmpty(mEtInput.text.toString().trim())) {
                SchemeUtils.toOpenActivity(requireContext(), Uri.parse(mEtInput.text.toString().trim()) )
            }
            dismissAllowingStateLoss()
        }
        mTvCancel.singleClick { dismissAllowingStateLoss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            setCanceledOnTouchOutside(false)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }
}