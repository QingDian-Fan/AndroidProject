package com.dian.demo.ui.img

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class AlbumInfo(

    /** 封面 */
    private val icon: String,
    /** 名称 */
    private var name: String,
    /** 备注 */
    private val remark: String,
    /** 选中 */
    private var select: Boolean
) : Parcelable {

    fun setName(name: String) {
        this.name = name
    }

    fun setSelect(select: Boolean) {
        this.select = select
    }

    fun getIcon(): String {
        return icon
    }

    fun getName(): String {
        return name
    }

    fun getRemark(): String {
        return remark
    }

    fun isSelect(): Boolean {
        return select
    }


}