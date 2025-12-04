package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentPersonalBinding
import com.dian.demo.di.vm.SettingViewModel


class PersonalFragment : BaseAppVMFragment<FragmentPersonalBinding, SettingViewModel>() {
    override fun getViewModelClass(): Class<SettingViewModel> = SettingViewModel::class.java



    override fun getLayoutId(): Int =R.layout.fragment_personal

    override fun initialize(savedInstanceState: Bundle?) {

    }

    companion object {
        @JvmStatic
        fun getFragment() = PersonalFragment()
    }
}