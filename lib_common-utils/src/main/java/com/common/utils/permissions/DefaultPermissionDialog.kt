package com.common.utils.permissions

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.os.bundleOf
import com.common.utils.R

class DefaultPermissionDialog : AppCompatDialogFragment() {
    companion object {
        fun getDialog(description: String = ""): DefaultPermissionDialog =
            DefaultPermissionDialog().apply {
                arguments = bundleOf("description" to description)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Animation_Dialog)
    }

    private var mView: View? = null
    private var mTvDescription: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        mView = inflater.inflate(R.layout.permission_description_popup, container)
        return mView

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mDescriptionString = arguments?.getString("description")
        mTvDescription = mView?.findViewById(R.id.tv_permission_description_message)
        mTvDescription?.text = mDescriptionString ?: ""
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.setCanceledOnTouchOutside(true)
            it.window?.run {
                setBackgroundDrawableResource(android.R.color.transparent)
                val params = attributes
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                attributes = params
            }
        }

    }
}