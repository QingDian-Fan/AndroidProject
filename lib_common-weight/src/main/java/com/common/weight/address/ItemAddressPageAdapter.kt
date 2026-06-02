package com.common.weight.address

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.common.utils.ext.singleClick
import com.common.weight.databinding.ItemAddressPageChildBinding
import org.json.JSONObject

/**
 * 单页（省 / 市 / 区其中一页）的列表适配器
 */
class ItemAddressPageAdapter(private val type: AddressType) :
    RecyclerView.Adapter<ItemAddressPageAdapter.ItemViewHolder>() {

    var onItemClick: ((name: String, type: AddressType, value: JSONObject?) -> Unit)? = null

    private val dataList = ArrayList<CityData>()

    inner class ItemViewHolder(val binding: ItemAddressPageChildBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemAddressPageChildBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val data = dataList[position]
        holder.binding.tvContent.text = data.name
        holder.itemView.singleClick {
            onItemClick?.invoke(data.name, type, data.value)
        }
    }

    override fun getItemCount(): Int = dataList.size

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: ArrayList<CityData>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }
}