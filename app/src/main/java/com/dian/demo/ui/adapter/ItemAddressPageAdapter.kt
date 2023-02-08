package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.R
import com.dian.demo.databinding.ItemAddressPageChildBinding
import com.dian.demo.di.model.CityData
import com.dian.demo.ui.adapter.ItemAddressPageAdapter.*

class ItemAddressPageAdapter(val dataList: List<String>) : RecyclerView.Adapter<ItemViewHolder>() {

    inner class ItemViewHolder(val binding: ItemAddressPageChildBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val mView =
            ItemAddressPageChildBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder.binding) {
            tvContent.text = dataList[position]
        }
    }

    override fun getItemCount(): Int = dataList.size
}