package com.dian.demo.ui.img


import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dian.demo.databinding.ItemAlbumBinding
import com.dian.demo.databinding.ItemImagePreviewBinding
import java.util.ArrayList
import com.dian.demo.ui.img.ImagePreviewAdapter.*


class ImagePreviewAdapter(val mContext: Context, private val dataList: ArrayList<String>) :
    RecyclerView.Adapter<ItemViewHolder>() {


    inner class ItemViewHolder(var binding: ItemImagePreviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemImagePreviewBinding.inflate(
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
                .load(dataList[position])
                .into(photoView)
        }
    }

    override fun getItemCount(): Int = dataList.size
}