package com.common.weight.address

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.common.weight.databinding.ItemAddressPageBinding
import org.json.JSONObject

class AddressPageAdapter(val mContext: Context) :
    RecyclerView.Adapter<AddressPageAdapter.ItemViewHolder>() {

    var onPageItemClick: ((name: String, type: AddressType, value: JSONObject?) -> Unit)? = null

    private val adapterMap = hashMapOf<AddressType, ItemAddressPageAdapter>()

    inner class ItemViewHolder(val binding: ItemAddressPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemAddressPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder.binding) {
            rvData.layoutManager = LinearLayoutManager(mContext)
            val type = when (position) {
                0 -> AddressType.PROVINCE
                1 -> AddressType.CITY
                else -> AddressType.AREA
            }
            val mAdapter = ItemAddressPageAdapter(type)
            adapterMap[type] = mAdapter
            rvData.adapter = mAdapter
            mAdapter.onItemClick = { name, t, value ->
                onPageItemClick?.invoke(name, t, value)
            }
        }
    }

    override fun getItemCount(): Int = 3

    fun setItemData(type: AddressType, datList: ArrayList<CityData>) {
        adapterMap[type]?.setData(datList)
    }
}