package com.dian.demo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.databinding.ItemSerachFowlayoutBinding
import com.dian.demo.di.model.SearchRecord
import com.dian.demo.ui.view.FlowLayoutAdapter

class SearchRecordAdapter @JvmOverloads constructor(
    val mContext: Context,
    val dataList: MutableList<SearchRecord>
) : FlowLayoutAdapter() {

    private var listener:((data: SearchRecord?)->Unit)?=null
    fun setClickListener(listener:(data: SearchRecord?)->Unit){
        this.listener = listener
    }
    override fun getItemCount(): Int = dataList.size

    override fun getItemView(
        position: Int,
        parent: ViewGroup?
    ): View? {
        val binding =
            ItemSerachFowlayoutBinding.inflate(LayoutInflater.from(mContext), parent, false)
        binding.tvContent.text = dataList.getOrNull(position)?.name
        binding.root.setOnClickListener {
            listener?.invoke(dataList.getOrNull(position))
        }
        return binding.root
    }
}