package com.dian.demo.utils.permissions

import android.Manifest
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.dian.demo.utils.ext.showAllowStateLoss

class DefaultPermissionInterceptor(var mPermissionDescription: String = "") :
    IPermissionInterceptor {
    private var dialog: DefaultPermissionDialog? = null
    override fun launchPermissionRequest(
        activity: FragmentActivity,
        allPermissions: Array<out String>
    ) {
        if (mPermissionDescription.isEmpty()) {
            mPermissionDescription = getPermissionDescription(activity, allPermissions)
        }
        dialog = DefaultPermissionDialog.getDialog(mPermissionDescription)
        dialog?.showAllowStateLoss(activity.supportFragmentManager, "")
    }

    override fun finishPermissionRequest(
        activity: FragmentActivity,
        allPermissions: Array<out String>
    ) {
        dialog?.dismissAllowingStateLoss()
    }

    private fun getPermissionDescription(
        activity: FragmentActivity,
        allPermissions: Array<out String>
    ): String {
        val stringBuilder = StringBuilder()
        allPermissions.forEach { it ->
            stringBuilder
                .append(permissionsToDescription(activity, it))
                .append("\n")
        }
        return stringBuilder.toString().trim { it <= ' ' }
    }

    private fun permissionsToDescription(mContext: Context, it: String): String {
        when (it) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE -> {
                return "存储权限:用于添加、上传图片和音频等场景中读取和写入相册和文件内容"
            }

            Manifest.permission.CAMERA -> {
                return "相机权限：用于拍照、录制视频、扫描二维码等场景"
            }

            Manifest.permission.SCHEDULE_EXACT_ALARM -> {
                return "相机权限：用于拍照、录制视频、扫描二维码等场景"
            }
        }
        return ""

    }
}