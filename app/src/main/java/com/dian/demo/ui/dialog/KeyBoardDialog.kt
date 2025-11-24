package com.dian.demo.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import com.dian.demo.R
import com.dian.demo.databinding.DialogKeyboardBinding
import com.dian.demo.ui.titlebar.ScreenUtils
import com.dian.demo.utils.InputMethodUtils
import com.dian.demo.utils.keyboard.KeyboardHelper
import kotlin.compareTo

class KeyBoardDialog : AppCompatDialogFragment() {

    companion object {
        fun getDialog(): AppCompatDialogFragment {
            val dialog = KeyBoardDialog()
            val bundle = Bundle()
            dialog.arguments = bundle
            return dialog
        }
    }

    private lateinit var binding: DialogKeyboardBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DialogKeyboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
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

    private fun initView() {
        activity?.let {
            KeyboardHelper.getInstance().attach(it)
            KeyboardHelper.getInstance()
                .init(binding.llHeader, binding.llContainer, binding.etContent)
            KeyboardHelper.getInstance().listener(object : KeyboardHelper.OnSoftInputListener {
                override fun onOpen() {
                    Log.e("TAG--->", "keyboard::onOpen")
                }

                override fun onClose() {
                    Log.e("TAG--->", "keyboard::onClose")
                }

            })
        }
        binding.rgLayout.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_message -> {
                    InputMethodUtils.hide(binding.llContainer)
                    context?.let {
                        binding.llContainer.postDelayed({
                            val layoutParams = binding.llContainer.layoutParams
                            layoutParams.height = ScreenUtils.getScreenHeight(it) / 5 * 2
                            binding.llContainer.layoutParams = layoutParams
                        }, 200)
                    }
                }

                R.id.rb_voice -> {
                    InputMethodUtils.hide(binding.llContainer)
                    context?.let {
                        binding.llContainer.postDelayed({
                            val layoutParams = binding.llContainer.layoutParams
                            layoutParams.height = ScreenUtils.getScreenHeight(it) / 5 * 1
                            binding.llContainer.layoutParams = layoutParams
                        }, 200)

                    }

                }

                R.id.rb_camera -> {
                    InputMethodUtils.show(binding.etContent)
                    context?.let {
                        val layoutParams = binding.llContainer.layoutParams
                        layoutParams.height = 0
                        binding.llContainer.layoutParams = layoutParams
                    }
                }

                R.id.rb_image -> {
                    InputMethodUtils.hide(binding.llContainer)
                    context?.let {
                        binding.llContainer.postDelayed({
                            val layoutParams = binding.llContainer.layoutParams
                            layoutParams.height = ScreenUtils.getScreenHeight(it) / 5 * 2
                            binding.llContainer.layoutParams = layoutParams
                        }, 200)
                    }
                }
            }
        }
    }

    private fun initData() {

    }


}