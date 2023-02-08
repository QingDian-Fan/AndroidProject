package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.visible
import com.dian.demo.databinding.ItemAddressTabBinding

class AddressTabAdapter(private val dataList: List<String>) :
    RecyclerView.Adapter<AddressTabAdapter.ItemViewHolder>() {


    inner class ItemViewHolder(val binding: ItemAddressTabBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val mView =
            ItemAddressTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder.binding) {
            if (position == dataList.size) {
                tvTabText.text = "请选择"
                cvTabIndicator.visibility = visible
            } else {
                tvTabText.text = dataList[position]
                cvTabIndicator.visibility = gone
            }
        }

    }

    override fun getItemCount(): Int = Math.min(dataList.size + 1, 3)
}