package com.dian.demo.ui.fragment

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.dian.demo.ProjectApplication
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentAboutBinding
import com.dian.demo.ui.activity.H5ContainerActivity
import java.lang.String
import kotlin.Int

class AboutFragment : BaseAppBindFragment<FragmentAboutBinding>() {



    override fun getLayoutId(): Int = R.layout.fragment_about

    override fun initialize(savedInstanceState: Bundle?) {
        binding.tvVersionName.text = String.format("%s(%d)", getVersionName(), getVersionCode())
        binding.llCheckUpdate.setRightText("已是最新版")
        binding.llCheckUpdate.setOnClickListener {
            showToast("已是最新版")
        }
        binding.llWebsite.setOnClickListener {
            H5ContainerActivity.start(requireActivity(),"https://www.wanandroid.com/")
        }
        binding.llWebContent.setOnClickListener {
            H5ContainerActivity.start(requireActivity(),"https://www.wanandroid.com/about")
        }
        binding.llProject.setOnClickListener {
            H5ContainerActivity.start(requireActivity()," https://github.com/QingDian-Fan/AndroidProject")
        }
        binding.llInnerTest.setOnClickListener {
           showToast("暂未开放")
        }
    }

    /**
     * 获取本地软件版本号code
     */
    fun getVersionCode(): Int {
        var localVersion = 0
        try {
            val packageInfo: PackageInfo = ProjectApplication.getAppContext().packageManager
                .getPackageInfo(ProjectApplication.getAppContext().packageName, 0)
            localVersion = packageInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return localVersion
    }

    /**
     * 获取本地软件版本号name
     */
    fun getVersionName(): kotlin.String? {
        var versionName: kotlin.String? = ""
        try {
            val packageInfo: PackageInfo =
                ProjectApplication.getAppContext().applicationContext.packageManager
                    .getPackageInfo(ProjectApplication.getAppContext().packageName, 0)
            versionName = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionName
    }

}