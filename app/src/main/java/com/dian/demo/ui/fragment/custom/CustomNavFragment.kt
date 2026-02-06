package com.dian.demo.ui.fragment.custom

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentCustomNavBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior


class CustomNavFragment : BaseAppBindFragment<FragmentCustomNavBinding>() {

    override fun getLayoutId(): Int = R.layout.fragment_custom_nav

    override fun initialize(savedInstanceState: Bundle?) {

        val behavior = BottomSheetBehavior.from(binding.bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED


    }

    companion object {
        @JvmStatic
        fun getFragment() = CustomNavFragment()
    }


}