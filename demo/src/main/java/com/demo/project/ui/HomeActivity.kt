package com.demo.project.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.common.ui.BaseAppVMActivity
import com.demo.project.databinding.ActivityMainBinding
import com.demo.project.vm.MainViewModel

class HomeActivity : BaseAppVMActivity<ActivityMainBinding, MainViewModel>() {
    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, HomeActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater, container, false)

    override fun initialize(savedInstanceState: Bundle?) {

    }

    override fun getViewModelClass(): Class<MainViewModel> = MainViewModel::class.java
}