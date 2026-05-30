package com.demo.project.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.common.ui.BaseAppBindActivity
import com.demo.project.R
import com.demo.project.databinding.ActivityCameraBinding
import com.demo.project.utils.ext.gone

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
