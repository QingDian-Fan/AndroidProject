package com.dian.demo.ui.img

import android.app.Activity

class ImageSelectUtil {


    private var mActivity: Activity? = null
    private var maxSelect: Int = 9
    private var selectList: ArrayList<String>? = null
    private var column: Int = 3
    private var mediaType: MediaType = MediaType.IMAGE
    private var listener: ImageSelectListener? = null
    private var cancelListener: ImageCancelListener? = null


    fun setActivity(mActivity: Activity): ImageSelectUtil {
        this.mActivity = mActivity
        return this
    }

    /**
     * 设置选取的媒体类型：图片、视频，或两者都包含
     * 默认为 [MediaType.IMAGE]
     */
    fun setMediaType(mediaType: MediaType): ImageSelectUtil {
        this.mediaType = mediaType
        return this
    }

    fun setMaxSelect(maxSelect: Int): ImageSelectUtil {
        this.maxSelect = maxSelect
        return this
    }

    fun setColumn(column: Int): ImageSelectUtil {
        this.column = if (column < 2 || column > 4) {
            3
        } else {
            column
        }
        return this
    }

    fun setSelectList(selectList: ArrayList<String>): ImageSelectUtil {
        this.selectList = selectList
        return this
    }

    fun setSelectListener(listener: ImageSelectListener): ImageSelectUtil {
        this.listener = listener
        return this
    }

    fun setCancelListener(cancelListener: ImageCancelListener): ImageSelectUtil {
        this.cancelListener = cancelListener
        return this
    }

    fun create() {
        if (mActivity == null) {
            throw RuntimeException("mActivity must be not null")
        }
        ImageSelectActivity.start(
            mActivity!!,
            maxSelect,
            column,
            mediaType,
            selectList,
            listener,
            cancelListener
        )
    }

}