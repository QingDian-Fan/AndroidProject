package com.dian.demo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.databinding.ItemSetupNavigationBinding
import com.dian.demo.di.model.NavigationData
import com.dian.demo.ui.view.FlowLayoutAdapter

class KnowledgeFlowAdapter @JvmOverloads constructor(
    val mContext: Context,
    val dataList: MutableList<NavigationData>,
    val isSetUp: Boolean
) : FlowLayoutAdapter()  {

    private var listener:((data: NavigationData?)->Unit)?=null

    fun setClickListener(listener:(data: NavigationData?)->Unit){
        this.listener = listener
    }

    override fun getItemCount(): Int  = dataList.size

    override fun getItemView(
        position: Int,
        parent: ViewGroup?,
    ): View? {
        val binding =
            ItemSetupNavigationBinding.inflate(LayoutInflater.from(mContext), parent, false)
      //  binding.tvContent.text = dataList.getOrNull(position)?.name
        binding.tvContent.text = if (isSetUp) dataList.getOrNull(position)?.name else dataList.getOrNull(position)?.title
        binding.root.setOnClickListener {
            listener?.invoke(dataList.getOrNull(position))
        }
        return binding.root
    }

}