package com.demo.project.ui.activity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import com.common.ui.BaseAppBindActivity
import com.demo.project.R
import com.demo.project.databinding.ActivityCameraBinding
import com.demo.project.ui.fragment.CameraFragment
import com.demo.project.utils.ext.gone

class CameraActivity : BaseAppBindActivity<ActivityCameraBinding>() {
    companion object {
        @JvmStatic
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, CameraActivity::class.java).apply {
                if (mContext !is Activity) {
                    flags=FLAG_ACTIVITY_NEW_TASK
                }
            }
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
