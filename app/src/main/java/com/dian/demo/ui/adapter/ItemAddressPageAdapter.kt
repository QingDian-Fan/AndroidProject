package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.R
import com.dian.demo.databinding.ItemAddressPageChildBinding
import com.dian.demo.di.model.CityData
import com.dian.demo.ui.adapter.ItemAddressPageAdapter.*
import com.dian.demo.utils.ext.singleClick
import org.json.JSONObject

class ItemAddressPageAdapter(val type: AddressType) :
    RecyclerView.Adapter<ItemViewHolder>() {
    private val dataList = arrayListOf<CityData>()

    inner class ItemViewHolder(val binding: ItemAddressPageChildBinding) :
        RecyclerView.ViewHolder(binding.root)

    var onItemClick: ((name:String,type: AddressType, JSONObject?) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val mView =
            ItemAddressPageChildBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            binding.tvContent.text = dataList[position].getName()
            itemView.singleClick {
                onItemClick?.invoke(dataList[position].getName(),type, dataList[position].getNext())
            }
        }

    }

    override fun getItemCount(): Int = dataList.size

    fun setData(dataList: ArrayList<CityData>){
        this.dataList.clear()
        this.dataList.addAll(dataList)
        notifyDataSetChanged()
    }
}