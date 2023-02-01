package com.dian.demo.ui.img

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityImageSelectBinding
import com.dian.demo.ui.activity.DemoActivity
import com.dian.demo.ui.titlebar.CommonTitleBar.ACTION_RIGHT_TEXT
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.ext.dpToPx
import com.dian.demo.utils.ext.showAllowStateLoss
import java.io.File
import java.util.ArrayList
import java.util.HashMap

class ImageSelectActivity : BaseAppBindActivity<ActivityImageSelectBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, ImageSelectActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    private val allImage = ArrayList<String>()

    private val allAlbum = HashMap<String, ArrayList<String>>()

    private val selectImage = ArrayList<String>()

    private lateinit var mAdapter: ImageSelectAdapter


    override fun getLayoutId(): Int = R.layout.activity_image_select

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().setCenterText(getString(R.string.text_image_select))
        getTitleBarView().setRightText(
            getString(R.string.text_all_image),
            ResourcesUtil.getColor(R.color.text_light_color),
            14.dpToPx
        )
        getTitleBarView().setListener { _, action, _ ->
            if (action == ACTION_RIGHT_TEXT) {
                initAlbum()
            }
        }

        getAllImage()

        mAdapter = ImageSelectAdapter(this@ImageSelectActivity, allImage, selectImage)
        binding.rvData.layoutManager = GridLayoutManager(this@ImageSelectActivity, 3)
        binding.rvData.adapter = mAdapter
        with(mAdapter) {
            onSelectListener = { _, url ->
                if (!selectImage.contains(url))
                    selectImage.add(url)
            }
            unSelectListener = { _, url ->
                if (selectImage.contains(url))
                    selectImage.remove(url)
            }
            onClickListener = { position, url ->
                ImagePreviewActivity.start(this@ImageSelectActivity, getDataList(), position)
            }
        }

    }

    private fun initAlbum() {
        val albumList = arrayListOf<AlbumInfo>()
        albumList.add(
            AlbumInfo(
                allImage[0],
                ResourcesUtil.getString(R.string.text_all_image),
                ResourcesUtil.getString(R.string.image_select_total, allImage.size),
                mAdapter.getData() === allImage
            )
        )
        val keys: MutableSet<String> = allAlbum.keys
        for (key: String in keys) {
            val list: MutableList<String>? = allAlbum[key]
            if (list == null || list.isEmpty()) {
                continue
            }
            albumList.add(
                AlbumInfo(
                    list[0],
                    key,
                    String.format(getString(R.string.image_select_total), list.size),
                    mAdapter.getData() === list
                )
            )
        }
        val bundle = Bundle()
        bundle.putParcelableArrayList("dataList", albumList)
        val dialog = AlbumDialogFragment()
        dialog.arguments = bundle
        dialog.showAllowStateLoss(supportFragmentManager, "")

        dialog.onChooseAlbumListener = {
            if (it.getName() == ResourcesUtil.getString(R.string.text_all_image) && (mAdapter.getData() != allImage)) {
                mAdapter.setData(allImage)
                getTitleBarView().setRightText(
                    getString(R.string.text_all_image),
                    ResourcesUtil.getColor(R.color.text_light_color),
                    14.dpToPx
                )
            } else {
                for (key: String in keys) {
                    val dataList = allAlbum[key]
                    if (it.getName() == key && dataList != null && dataList.isNotEmpty() && (mAdapter.getData() != dataList)) {
                        mAdapter.setData(dataList)
                        getTitleBarView().setRightText(
                            key,
                            ResourcesUtil.getColor(R.color.text_light_color),
                            14.dpToPx
                        )
                    }
                }
            }
        }
    }

    private fun getAllImage() {
        allAlbum.clear()
        allImage.clear()
        val contentUri: Uri = MediaStore.Files.getContentUri("external")
        val sortOrder: String = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        val selection: String =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)" + " AND " + MediaStore.MediaColumns.SIZE + ">0"
        val contentResolver: ContentResolver = contentResolver
        val projections: Array<String?> = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE
        )
        val cursor = contentResolver.query(
            contentUri, projections, selection,
            arrayOf<String?>(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()), sortOrder
        )
        if (cursor != null && cursor.moveToFirst()) {
            val pathIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val mimeTypeIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            do {
                val size: Long = cursor.getLong(sizeIndex)
                // 图片大小不得小于 1 KB
                if (size < 1024) {
                    continue
                }
                val type: String = cursor.getString(mimeTypeIndex)
                val path: String = cursor.getString(pathIndex)
                if (TextUtils.isEmpty(path) || TextUtils.isEmpty(type)) {
                    continue
                }
                val file = File(path)
                if (!file.exists() || !file.isFile) {
                    continue
                }
                val parentFile: File = file.parentFile ?: continue

                // 获取目录名作为专辑名称
                val albumName: String = parentFile.name
                var data: MutableList<String>? = allAlbum[albumName]
                if (data == null) {
                    data = ArrayList()
                    allAlbum[albumName] = data
                }
                data.add(path)
                allImage.add(path)
            } while (cursor.moveToNext())
            cursor.close()
        }

    }


}