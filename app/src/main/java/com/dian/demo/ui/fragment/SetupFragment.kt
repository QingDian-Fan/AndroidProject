package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSetupBinding
import com.dian.demo.di.vm.SetupViewModel


class SetupFragment : BaseAppVMFragment<FragmentSetupBinding, SetupViewModel>() {


    override fun getLayoutId(): Int = R.layout.fragment_setup

    override fun initialize(savedInstanceState: Bundle?) {

    }

    override fun createViewModel(): SetupViewModel = SetupViewModel()

    companion object {
        @JvmStatic
        fun getFragment() = SetupFragment()
    }
}