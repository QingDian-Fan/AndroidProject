package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityCameraBinding
import com.dian.demo.ui.fragment.CameraFragment

class CameraActivity : BaseAppBindActivity<ActivityCameraBinding>() {
    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, CameraActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_camera

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.visibility = gone
        val fragment = CameraFragment.getFragment()
        val mFragmentTransaction =
            supportFragmentManager.beginTransaction().add(R.id.fl_container, fragment)
        mFragmentTransaction.commitAllowingStateLoss()
        mFragmentTransaction.show(fragment)
    }
}