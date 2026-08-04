package com.demo.project.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.common.image.ext.loadImage
import com.demo.project.R
import com.demo.project.databinding.ItemShareWebBinding


class WebShareLogoAdapter(
    val dataList: MutableList<String?>,
    val onItemClickListener: (position: Int) -> Unit
) : RecyclerView.Adapter<WebShareLogoAdapter.ItemViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        return ItemViewHolder(
            ItemShareWebBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {
        with(holder) {
            if (dataList.getOrNull(position) == null) {
                binding.ivLogo.setImageResource(R.mipmap.icon_none)
            } else {
                binding.ivLogo.loadImage(dataList.getOrNull(position))
            }
            binding.root.setOnClickListener {
                onItemClickListener.invoke(position)
            }
        }

    }

    override fun getItemCount(): Int = dataList.size

    inner class ItemViewHolder(val binding: ItemShareWebBinding) :
        RecyclerView.ViewHolder(binding.root)
}