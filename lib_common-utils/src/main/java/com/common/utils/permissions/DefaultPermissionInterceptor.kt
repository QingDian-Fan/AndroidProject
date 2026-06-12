package com.common.utils.permissions

import android.Manifest
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.common.utils.R
import com.common.utils.ext.showAllowStateLoss

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
            val mDescription = permissionsToDescription(activity, it)
            if (!stringBuilder.contains(mDescription)) {
                stringBuilder
                    .append(mDescription)
                    .append("\n")
            }
        }
        return stringBuilder.toString().trim { it <= ' ' }
    }

    private fun permissionsToDescription(mContext: Context, it: String): String {
        when (it) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO -> {
                return mContext.getString(R.string.permission_desc_storage)
            }

            Manifest.permission.CAMERA -> {
                return mContext.getString(R.string.permission_desc_camera)
            }

            Manifest.permission.SCHEDULE_EXACT_ALARM -> {
                return mContext.getString(R.string.permission_desc_alarm)
            }
        }
        return ""

    }
}
