package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityH5ContainerBinding
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.ext.gone

class H5ContainerActivity : BaseAppBindActivity<ActivityH5ContainerBinding>() {
    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, H5ContainerActivity::class.java)
            mContext.startActivity(intent)
        }
        fun start(mContext: Context,urlString: String) {
            val intent = Intent()
            intent.setClass(mContext, H5ContainerActivity::class.java)
            intent.putExtra("urlString",urlString)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int  =R.layout.activity_h5_container

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.mainView.gone()
        getTitleBarView()?.setOpenStatusBar(true)
        getTitleBarView()?.setStatusBarColor(ResourcesUtil.getColor(R.color.text_blue_color))
        getTitleBarView()?.bottomLine.gone()
        val urlString = intent.getStringExtra("urlString")
        val bundle = Bundle().apply {
            putString("urlString", urlString)
        }
        supportFragmentManager.setFragmentResult("KEY_URL_DATA", bundle)
    }
}
