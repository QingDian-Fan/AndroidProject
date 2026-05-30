package com.common.utils.permissions

import android.Manifest
import android.os.Build

/**
 * 运行时权限适配：根据系统版本在「外置存储权限」与「细分媒体权限」之间互相转换。
 *
 * - Android 13（API 33, TIRAMISU）及以上：使用 READ_MEDIA_IMAGES / READ_MEDIA_VIDEO / READ_MEDIA_AUDIO。
 *   调用方声明的 READ_EXTERNAL_STORAGE 会自动映射为 READ_MEDIA_IMAGES + READ_MEDIA_VIDEO；
 *   WRITE_EXTERNAL_STORAGE 在该版本已无意义，直接移除。
 * - Android 12（API 32）及以下：统一使用 READ_EXTERNAL_STORAGE；
 *   细分媒体权限会被映射回 READ_EXTERNAL_STORAGE；
 *   WRITE_EXTERNAL_STORAGE 仅在 Android 9（API 28）及以下保留（更高版本运行时申请已无效）。
 *
 * 其余非存储/媒体类权限（相机、录音等）原样透传。
 */
object PermissionConversionUtil {

    private val READ_MEDIA_IMAGES = Manifest.permission.READ_MEDIA_IMAGES
    private val READ_MEDIA_VIDEO = Manifest.permission.READ_MEDIA_VIDEO
    private val READ_MEDIA_AUDIO = Manifest.permission.READ_MEDIA_AUDIO
    private val READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
    private val WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE

    /** Android 13（API 33）及以上：使用细分媒体权限 */
    private val isAtLeastAndroid13: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Android 9（API 28）及以下：WRITE_EXTERNAL_STORAGE 仍有意义 */
    private val isAtMostAndroid9: Boolean
        get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    fun conversionPermission(permissions: Array<out String>): Array<out String> {
        // LinkedHashSet：保持声明顺序并自动去重
        val result = LinkedHashSet<String>()
        var needVisualMedia = false   // 是否需要读取图片 / 视频
        var needAudio = false         // 是否需要读取音频

        for (permission in permissions) {
            when (permission) {
                READ_EXTERNAL_STORAGE -> needVisualMedia = true
                WRITE_EXTERNAL_STORAGE -> {
                    needVisualMedia = true
                    if (isAtMostAndroid9) result.add(WRITE_EXTERNAL_STORAGE)
                }
                READ_MEDIA_IMAGES, READ_MEDIA_VIDEO -> needVisualMedia = true
                READ_MEDIA_AUDIO -> needAudio = true
                else -> result.add(permission)   // 其它权限原样保留
            }
        }

        if (needVisualMedia || needAudio) {
            if (isAtLeastAndroid13) {
                if (needVisualMedia) {
                    result.add(READ_MEDIA_IMAGES)
                    result.add(READ_MEDIA_VIDEO)
                }
                if (needAudio) result.add(READ_MEDIA_AUDIO)
            } else {
                // 低版本统一使用外置存储读权限（音频/图片/视频读取都依赖它）
                result.add(READ_EXTERNAL_STORAGE)
            }
        }

        return result.toTypedArray()
    }
}
