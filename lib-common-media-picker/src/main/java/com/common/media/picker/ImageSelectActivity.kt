package com.common.media.picker

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.common.aop.CheckPermissions
import com.common.media.picker.databinding.ActivityImageSelectBinding
import com.common.ui.BaseAppBindActivity
import com.common.utils.ResourcesUtil
import com.common.utils.ext.dpToPx
import com.common.utils.ext.showAllowStateLoss
import com.common.utils.ext.singleClick
import com.common.weight.titlebar.CommonTitleBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImageSelectActivity : BaseAppBindActivity<ActivityImageSelectBinding>() {

    companion object {
        private const val SELECT_IMAGE_MAX_SELECT: String = "maxSelect"
        private const val SELECT_IMAGE_IMAGE_LIST: String = "selectImage"
        private const val SELECT_IMAGE_COLUM: String = "column"
        private const val SELECT_MEDIA_TYPE: String = "mediaType"
        private var listener: ImageSelectListener? = null
        private var cancelListener:ImageCancelListener?=null

        private fun setSelectLister(listener: ImageSelectListener) {
            this.listener = listener
        }
        private fun setCancelLister(cancelListener: ImageCancelListener) {
            this.cancelListener = cancelListener
        }
        @CheckPermissions(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, isMust = true)
        fun start(mContext: Context, maxSelect: Int, column:Int, mediaType: MediaType = MediaType.IMAGE, selectList: ArrayList<String>? = null, listener: ImageSelectListener? = null, cancelListener:ImageCancelListener?=null) {
            val intent = Intent()
            intent.setClass(mContext, ImageSelectActivity::class.java)
            intent.putExtra(SELECT_IMAGE_MAX_SELECT, maxSelect)
            intent.putExtra(SELECT_IMAGE_COLUM,column)
            intent.putExtra(SELECT_MEDIA_TYPE, mediaType)
            intent.putExtra(SELECT_IMAGE_IMAGE_LIST, selectList)
            if (listener != null) {
                setSelectLister(listener)
            }
            if (cancelListener!=null){
                setCancelLister(cancelListener)
            }
            mContext.startActivity(intent)
        }
    }


    private val allImage = ArrayList<String>()

    private val allAlbum = HashMap<String, ArrayList<String>>()

    private val selectImage = ArrayList<String>()

    /** 视频文件路径集合，用于在列表中区分图片与视频 */
    private val videoPaths = HashSet<String>()

    /** 当前选取的媒体类型 */
    private var mediaType: MediaType = MediaType.IMAGE

    private lateinit var mAdapter: ImageSelectAdapter

    /** 标题栏中心文案 */
    private val titleLabel: String
        get() = when (mediaType) {
            MediaType.IMAGE -> getString(R.string.text_image_select)
            MediaType.VIDEO -> getString(R.string.text_video_select)
            MediaType.ALL -> getString(R.string.text_media_select)
        }

    /** “全部”相册文案 */
    private val allLabel: String
        get() = when (mediaType) {
            MediaType.IMAGE -> getString(R.string.text_all_image)
            MediaType.VIDEO -> getString(R.string.text_all_video)
            MediaType.ALL -> getString(R.string.text_all_media)
        }


    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup): ActivityImageSelectBinding =
        ActivityImageSelectBinding.inflate(inflater, container, false)

    override fun initialize(savedInstanceState: Bundle?) {
        mediaType = (intent.getSerializableExtra(SELECT_MEDIA_TYPE) as? MediaType) ?: MediaType.IMAGE
        getTitleBarView()?.setCenterText(titleLabel)
        getTitleBarView()?.setRightText(
            allLabel,
            ResourcesUtil.getColor(R.color.text_light_color),
            14.dpToPx
        )
        getTitleBarView()?.setListener { _, action, _ ->
            if (action == CommonTitleBar.ACTION_RIGHT_TEXT) {
                initAlbum()
            }
        }
        getTitleBarView()?.getLeftImageButton()?.setOnClickListener {
            onBackPressed()
        }
        val maxSelect = intent.getIntExtra(SELECT_IMAGE_MAX_SELECT, 9)
        val selectList = intent.getStringArrayListExtra(SELECT_IMAGE_IMAGE_LIST)
        val column = intent.getIntExtra(SELECT_IMAGE_COLUM,3)
        if (selectList != null && selectList.isNotEmpty()) {
            selectImage.addAll(selectList)
        }

        binding.btnConfirm.singleClick {
            listener?.selectListener(selectImage)
            finish()
        }

        mAdapter = ImageSelectAdapter(
            this@ImageSelectActivity,
            allImage,
            selectImage,
            videoPaths,
            maxSelect != 1,
            maxSelect
        )
        binding.rvData.layoutManager = GridLayoutManager(this@ImageSelectActivity, column)
        binding.rvData.adapter = mAdapter
        with(mAdapter) {
            onSelectListener = { _, url ->
                if (!selectImage.contains(url))
                    selectImage.add(url)
            }
            unSelectListener = { _, url ->
                if (selectImage.contains(url)) {
                    selectImage.remove(url)
                    notifyDataSetChanged()
                }

            }
            onClickListener = { position, url ->
                ImagePreviewActivity.start(this@ImageSelectActivity, getDataList(), position)
            }
        }

        // 图库扫描包含 MediaStore 查询与逐条 File.exists() 磁盘 IO，放到后台线程避免阻塞主线程导致 ANR
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { getAllMedia() }
            mAdapter.notifyDataSetChanged()
        }

    }

    private var dialog: AlbumDialogFragment? = null


    private fun initAlbum() {
        if (allImage.isEmpty()) {
            return
        }
        val albumList = arrayListOf<AlbumInfo>()
        albumList.add(
            AlbumInfo(
                allImage[0],
                allLabel,
                ResourcesUtil.getString(R.string.image_select_total, allImage.size),
                mAdapter.getDataList() === allImage
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
                    ResourcesUtil.getString(R.string.image_select_total, list.size),
                    mAdapter.getDataList() === list
                )
            )
        }
        val bundle = Bundle()
        bundle.putParcelableArrayList("dataList", albumList)
        dialog = AlbumDialogFragment()
        dialog?.arguments = bundle
        dialog?.showAllowStateLoss(supportFragmentManager, "")

        dialog?.onChooseAlbumListener = {
            if (it.getName() == allLabel && (mAdapter.getDataList() !== allImage)) {
                mAdapter.setData(allImage)
                getTitleBarView()?.setRightText(
                    allLabel,
                    ResourcesUtil.getColor(R.color.text_light_color),
                    14.dpToPx
                )
            } else {
                for (key: String in keys) {
                    val dataList = allAlbum[key]
                    if (it.getName() == key && dataList != null && dataList.isNotEmpty() && (mAdapter.getDataList() !== dataList)) {
                        mAdapter.setData(dataList)
                        getTitleBarView()?.setRightText(
                            key,
                            ResourcesUtil.getColor(R.color.text_light_color),
                            14.dpToPx
                        )
                    }
                }
            }
        }
    }

    private fun getAllMedia() {
        allAlbum.clear()
        allImage.clear()
        videoPaths.clear()
        val contentUri: Uri = MediaStore.Files.getContentUri("external")
        val sortOrder: String = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
        val mediaTypeColumn: String = MediaStore.Files.FileColumns.MEDIA_TYPE
        val sizeColumn: String = MediaStore.MediaColumns.SIZE
        val imageType: String = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString()
        val videoType: String = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        // 根据选取类型构造查询条件
        val selection: String
        val selectionArgs: Array<String>
        when (mediaType) {
            MediaType.IMAGE -> {
                selection = "($mediaTypeColumn=?) AND $sizeColumn>0"
                selectionArgs = arrayOf(imageType)
            }
            MediaType.VIDEO -> {
                selection = "($mediaTypeColumn=?) AND $sizeColumn>0"
                selectionArgs = arrayOf(videoType)
            }
            MediaType.ALL -> {
                selection = "($mediaTypeColumn=? OR $mediaTypeColumn=?) AND $sizeColumn>0"
                selectionArgs = arrayOf(imageType, videoType)
            }
        }
        val contentResolver: ContentResolver = contentResolver
        val projections: Array<String?> = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            mediaTypeColumn
        )
        contentResolver.query(
            contentUri, projections, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use
            }
            val pathIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val mimeTypeIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIndex: Int = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val typeIndex: Int = cursor.getColumnIndex(mediaTypeColumn)
            do {
                val size: Long = cursor.getLong(sizeIndex)
                // 文件大小不得小于 1 KB
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
                // 记录视频路径，用于列表区分展示
                if (cursor.getInt(typeIndex) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    videoPaths.add(path)
                }
            } while (cursor.moveToNext())
        }
    }

    override fun onBackPressed() {
        if (dialog?.dialog?.isShowing == true) {
            dialog?.dismissAllowingStateLoss()
        } else {
            cancelListener?.cancel()
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 静态监听持有调用方（通常是 Activity）的引用，结束时清除避免内存泄漏；
        // 配置变更（isChangingConfigurations）时保留，以便重建后仍能回调
        if (isFinishing) {
            listener = null
            cancelListener = null
        }
    }


}