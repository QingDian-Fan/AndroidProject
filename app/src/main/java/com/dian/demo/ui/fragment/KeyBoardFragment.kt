package com.dian.demo.ui.fragment

import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentKeyBoardBinding

class KeyBoardFragment : BaseAppBindFragment<FragmentKeyBoardBinding>() {
    override fun getLayoutId(): Int = R.layout.fragment_key_board

    override fun initialize(savedInstanceState: Bundle?) {
        binding.rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.rootLayout.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.rootLayout.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            if (keyboardHeight > screenHeight * 0.15) {
                // 键盘弹出，上移布局
                binding.contentLayout.animate()
                    .translationY(-keyboardHeight.toFloat() + 100)
                    .setDuration(100)
                    .start()
            } else {
                // 键盘收回，恢复布局
                binding.contentLayout.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        }
    }


    companion object {

        @JvmStatic
        fun newInstance() = KeyBoardFragment()
    }
}