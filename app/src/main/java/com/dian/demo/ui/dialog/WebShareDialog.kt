package com.dian.demo.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.dian.demo.databinding.DialogWebShareBinding

class WebShareDialog:AppCompatDialogFragment() {
    companion object {
        fun getDialog(): WebShareDialog {
            val dialog = WebShareDialog()
            val bundle = Bundle()
            dialog.arguments = bundle
            return dialog
        }
    }

    private lateinit var binding: DialogWebShareBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        binding = DialogWebShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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