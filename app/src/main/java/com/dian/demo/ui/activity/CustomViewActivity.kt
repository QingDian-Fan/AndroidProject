package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityCustomViewBinding

class CustomViewActivity : BaseAppBindActivity<ActivityCustomViewBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, CustomViewActivity::class.java)
            mContext.startActivity(intent)
        }
    }
    override fun getLayoutId(): Int = R.layout.activity_custom_view

    override fun initialize(savedInstanceState: Bundle?) {
        setPageTitle("自定义View")
    }
}