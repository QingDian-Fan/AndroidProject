package com.demo.project.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.common.ui.BaseAppVMActivity
import com.demo.project.R
import com.demo.project.databinding.ActivityMainBinding
import com.demo.project.vm.MainViewModel

class HomeActivity : BaseAppVMActivity<ActivityMainBinding, MainViewModel>() {
     companion object{
         fun start(mContext: Context){
             val intent = Intent()
             intent.setClass(mContext, HomeActivity::class.java)
             mContext.startActivity(intent)
         }
     }

    override fun getLayoutId(): Int  = R.layout.activity_main

    override fun initialize(savedInstanceState: Bundle?) {

    }

    override fun createViewModel(): MainViewModel  = MainViewModel()

}