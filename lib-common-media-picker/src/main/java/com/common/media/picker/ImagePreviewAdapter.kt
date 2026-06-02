package com.common.media.picker


import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.common.media.picker.databinding.ItemImagePreviewBinding
import com.common.media.picker.databinding.ItemVideoPreviewBinding
import java.util.ArrayList


class ImagePreviewAdapter(val mContext: Context, private val dataList: ArrayList<String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_VIDEO = 1

        /** 常见视频文件后缀，用于区分图片与视频 */
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "3gp", "webm", "m4v", "flv", "ts", "wmv", "mpeg", "mpg"
        )
    }

    class ImageViewHolder(val binding: ItemImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    class VideoViewHolder(val binding: ItemVideoPreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    /** 指定位置是否为视频 */
    fun isVideo(position: Int): Boolean = isVideoPath(dataList[position])

    /** 获取指定位置的文件路径 */
    fun getPath(position: Int): String = dataList[position]

    private fun isVideoPath(path: String): Boolean =
        path.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    override fun getItemViewType(position: Int): Int =
        if (isVideo(position)) TYPE_VIDEO else TYPE_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_VIDEO) {
            VideoViewHolder(ItemVideoPreviewBinding.inflate(inflater, parent, false))
        } else {
            ImageViewHolder(ItemImagePreviewBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> {
                Glide.with(mContext)
                    .asBitmap()
                    .load(dataList[position])
                    .into(holder.binding.photoView)
            }

            is VideoViewHolder -> {
                // 复位为“播放”图标与进度；实际播放/暂停由 Activity 根据当前页驱动
                holder.binding.ivPlayPause.setImageResource(R.drawable.icon_video_play)
                holder.binding.seekBar.progress = 0
                holder.binding.tvPosition.text = "00:00"
                holder.binding.tvDuration.text = "00:00"
            }
        }
    }

    override fun getItemCount(): Int = dataList.size
}
