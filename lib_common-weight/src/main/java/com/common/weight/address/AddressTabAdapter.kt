package com.common.weight.address

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.common.utils.ResourcesUtil
import com.common.utils.ext.gone
import com.common.utils.ext.singleClick
import com.common.utils.ext.visible
import com.common.weight.databinding.ItemAddressTabBinding
import com.common.theme.R as ThemeR

class AddressTabAdapter(private val dataList: List<String>) :
    RecyclerView.Adapter<AddressTabAdapter.ItemViewHolder>() {

    var onItemClick: ((position: Int) -> Unit)? = null

    inner class ItemViewHolder(val binding: ItemAddressTabBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemAddressTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            if (position == dataList.size) {
                binding.tvTabText.setText(com.common.weight.R.string.address_tab_select)
                binding.cvTabIndicator.visible()
                binding.tvTabText.setTextColor(ResourcesUtil.getColor(ThemeR.color.text_light_color))
            } else {
                binding.tvTabText.text = dataList[position]
                binding.cvTabIndicator.gone()
                binding.tvTabText.setTextColor(ResourcesUtil.getColor(ThemeR.color.text_color))
                itemView.singleClick {
                    onItemClick?.invoke(position)
                }
            }
        }
    }

    override fun getItemCount(): Int = minOf(dataList.size + 1, 3)
}
