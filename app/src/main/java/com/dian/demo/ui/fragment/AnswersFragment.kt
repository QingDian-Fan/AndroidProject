package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentAnswersBinding
import com.dian.demo.di.vm.AnswersViewModel


class AnswersFragment : BaseAppVMFragment<FragmentAnswersBinding, AnswersViewModel>() {
    override fun createViewModel(): AnswersViewModel  = AnswersViewModel()

    override fun getLayoutId(): Int = R.layout.fragment_answers

    override fun initialize(savedInstanceState: Bundle?) {

    }


    companion object {

        @JvmStatic
        fun getFragment() = AnswersFragment()
    }
}