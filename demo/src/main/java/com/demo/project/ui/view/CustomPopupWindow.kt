package com.demo.project.ui.view

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.demo.project.databinding.CustomPopupWindowLayoutBinding
import com.demo.project.utils.BaseBindingAdapter

class CustomPopupWindow(
    private val context: Context,
    width: Int,
    private val adapter: BaseBindingAdapter<*, *>
) : PopupWindow(context) {

    private val binding = CustomPopupWindowLayoutBinding.inflate(LayoutInflater.from(context))

    private var onItemClickListener: OnItemClickListener? = null

    fun interface OnItemClickListener {
        fun onItemClickListener(position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    init {
        contentView = binding.root
        setWidth(width)
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT)

        // 设置背景只有设置了这个才可以点击外边和BACK消失
        setBackgroundDrawable(ColorDrawable())
        // 设置可以获取焦点
        isFocusable = true
        // 设置点击外边可以消失
        isOutsideTouchable = true
        // 设置可以触摸
        isTouchable = true

        // 设置点击外部可以消失
        setTouchInterceptor { _, event ->
            // 判断是不是点击了外部
            event.action == MotionEvent.ACTION_OUTSIDE
        }

        initView()
    }

    private fun initView() {
        binding.rvSpinner.layoutManager = LinearLayoutManager(context)
        binding.rvSpinner.adapter = adapter
        adapter.setItemOnClickListener { position ->
            onItemClickListener?.onItemClickListener(position)
            dismiss()
        }
    }
}