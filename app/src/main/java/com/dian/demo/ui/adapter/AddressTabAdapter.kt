package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.textColor
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.databinding.ItemAddressTabBinding
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.ext.singleClick

class AddressTabAdapter(private val dataList: List<String>) :
    RecyclerView.Adapter<AddressTabAdapter.ItemViewHolder>() {

    var onItemClick: ((position: Int) -> Unit)? = null

    inner class ItemViewHolder(val binding: ItemAddressTabBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val mView =
            ItemAddressTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(mView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            if (position == dataList.size) {
                binding.tvTabText.text = "请选择"
                binding.cvTabIndicator.visibility = visible
                binding.tvTabText.setTextColor(ResourcesUtil.getColor(R.color.text_light_color))
            } else {
                binding.tvTabText.text = dataList[position]
                binding.cvTabIndicator.visibility = gone
                binding.tvTabText.setTextColor(ResourcesUtil.getColor(R.color.text_color))
                itemView.singleClick {
                    onItemClick?.invoke(position)
                }
            }
        }

    }

    override fun getItemCount(): Int = Math.min(dataList.size + 1, 3)
}