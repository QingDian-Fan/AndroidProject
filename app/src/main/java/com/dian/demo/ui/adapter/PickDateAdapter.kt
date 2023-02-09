package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.databinding.ItemPickerDateBinding
import com.dian.demo.ui.adapter.PickDateAdapter.*
import com.dian.demo.utils.ext.singleClick

class PickDateAdapter(val datalist:ArrayList<String>) :RecyclerView.Adapter<ItemViewHolder>(){

    var onItemClick:((position:Int,value:String)->Unit) ?=null

    inner class ItemViewHolder(val binding:ItemPickerDateBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemPickerDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
       with(holder){
           binding.tvPickerName.text = datalist[position]
           itemView.singleClick {
               onItemClick?.invoke(position,datalist[position])
           }
       }
    }

    override fun getItemCount(): Int = datalist.size
}