package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.databinding.ItemWebRecordBinding
import com.dian.demo.ui.activity.H5ContainerActivity
import com.dian.demo.utils.DateFormatUtil
import com.dian.demo.utils.webview.bean.WebDataEntry

class WebRecordAdapter(val dataList: List<WebDataEntry>): RecyclerView.Adapter<WebRecordAdapter.ItemViewHolder>()  {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        return ItemViewHolder(
            ItemWebRecordBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ItemViewHolder,
        position: Int
    ) {
       with(holder){
           binding.tvTitle.text = dataList.getOrNull(position)?.title
           binding.tvTime.text = DateFormatUtil.getDate(dataList.getOrNull(position)?.timestamp?:0L)
           binding.root.setOnClickListener {
               H5ContainerActivity.start(binding.root.context,dataList.getOrNull(position)?.url?:"")
           }
       }
    }

    override fun getItemCount(): Int = dataList.size

    inner class ItemViewHolder(val binding: ItemWebRecordBinding) :
        RecyclerView.ViewHolder(binding.root)
}