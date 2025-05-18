package com.dian.demo.ui.img

import android.app.Activity
import android.content.Context
import androidx.room.Room
import com.dian.demo.di.repository.local.DatabaseFactory

class ImageSelectUtil {


    private var mActivity: Activity? = null
    private var maxSelect: Int = 9
    private var selectList: ArrayList<String>? = null
    private var column: Int = 3
    private var listener: ImageSelectListener? = null
    private var cancelListener: ImageCancelListener? = null


    fun setActivity(mActivity: Activity): ImageSelectUtil {
        this.mActivity = mActivity
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
            selectList,
            listener,
            cancelListener
        )
    }

}