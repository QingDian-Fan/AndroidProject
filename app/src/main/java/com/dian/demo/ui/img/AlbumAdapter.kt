package com.dian.demo.ui.img

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dian.demo.databinding.ItemAlbumBinding
import com.dian.demo.ui.img.AlbumAdapter.*
import com.dian.demo.utils.ext.singleClick

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
            onItemClickListener.invoke(position, dataList[position])
            holder.binding.cbAlbumCheck.isChecked = !dataList[position].isSelect()
        }
    }

    override fun getItemCount(): Int = dataList.size
}