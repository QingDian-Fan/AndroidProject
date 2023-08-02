package com.dian.demo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.dian.demo.databinding.ItemAddressPageBinding
import com.dian.demo.di.model.CityData
import com.dian.demo.ui.adapter.AddressPageAdapter.*
import org.json.JSONObject

class AddressPageAdapter(val mContext: Context) :
    RecyclerView.Adapter<ItemViewHolder>() {

    var onPageItemClick: ((name:String,type: AddressType, value: JSONObject?) -> Unit)? = null

    private val adapterMap = hashMapOf<AddressType,ItemAddressPageAdapter>()


    inner class ItemViewHolder(val binding: ItemAddressPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val mView =
            ItemAddressPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder.binding) {
            rvData.layoutManager = LinearLayoutManager(mContext)
            val mAdapter =
                ItemAddressPageAdapter(if (position == 0) AddressType.PROVINCE else if (position == 1) AddressType.CITY else AddressType.AREA)
            adapterMap[if (position == 0) AddressType.PROVINCE else if (position == 1) AddressType.CITY else AddressType.AREA] = mAdapter
            rvData.adapter = mAdapter
            mAdapter.onItemClick = { name,type, value ->
                onPageItemClick?.invoke(name,type, value)
            }
        }
    }

    override fun getItemCount(): Int = 3

    fun setItemData(type: AddressType, datList: ArrayList<CityData>) {
        val itemAddressPageAdapter = adapterMap[type]
        itemAddressPageAdapter?.setData(datList)
       // notifyDataSetChanged()
    }


}