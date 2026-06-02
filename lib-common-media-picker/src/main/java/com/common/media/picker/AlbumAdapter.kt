package com.common.media.picker

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.common.media.picker.databinding.ItemAlbumBinding
import com.common.media.picker.AlbumAdapter.*
import com.common.utils.ext.singleClick

class AlbumAdapter(private val mContext: Context, private val dataList: ArrayList<AlbumInfo>) :
    RecyclerView.Adapter<ItemViewHolder>() {

    inner class ItemViewHolder(var binding: ItemAlbumBinding) :
        RecyclerView.ViewHolder(binding.root)

    lateinit var onItemClickListener: (pos: Int, info: AlbumInfo) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemAlbumBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder.binding) {
            Glide.with(mContext)
                .asBitmap()
                .load(dataList[position].getIcon())
                .into(ivAlbumLogo)
            tvAlbumName.text = dataList[position].getName()
            tvAlbumRemark.text = dataList[position].getRemark()
            cbAlbumCheck.isChecked = dataList[position].isSelect()
        }
        holder.itemView.singleClick {
            // 仅刷新上一个选中项和当前项，避免整列表重绘
            val previousPosition = dataList.indexOfFirst { it.isSelect() }
            dataList.forEachIndexed { index, info ->
                info.setSelect(index == position)
            }
            if (previousPosition != -1 && previousPosition != position) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(position)
            onItemClickListener.invoke(position, dataList[position])
        }
    }

    override fun getItemCount(): Int = dataList.size
}