package com.dian.demo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.dian.demo.databinding.ItemAddressPageBinding
import com.dian.demo.di.model.ProvinceData
import com.dian.demo.ui.adapter.AddressPageAdapter.*

class AddressPageAdapter(val datList:ArrayList<ProvinceData>, val mContext: Context) :RecyclerView.Adapter<ItemViewHolder>(){
    private  val provinceList:ArrayList<String> = ArrayList()

    inner class ItemViewHolder(val binding:ItemAddressPageBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val mView =
            ItemAddressPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder.binding){
            provinceList.clear()
            datList.forEach {
               provinceList.add(it.name)
            }
            rvData.layoutManager = LinearLayoutManager(mContext)
            rvData.adapter=ItemAddressPageAdapter(provinceList)
        }
    }

    override fun getItemCount(): Int = 3

}