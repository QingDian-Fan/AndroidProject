package com.dian.demo.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.databinding.ItemHomeArticleBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.ui.adapter.HomeArticleAdapter.*
import com.dian.demo.utils.DateFormatUtil

class HomeArticleAdapter(val dataList: List<ArticleBean>) : RecyclerView.Adapter<ItemViewHolder>() {

    private var onItemClickListener: ((link: String?) -> Unit)? = null

    fun setOnItemClickListener(listener: (link: String?) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemHomeArticleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {
            binding.tvTitle.text = dataList[position].title
            binding.tvUser.text = "作者：${dataList[position].shareUser}"
            binding.tvTime.text = "时间：${DateFormatUtil.getDate(dataList[position].publishTime)}"
            binding.root.setOnClickListener {
                Log.e("TAG--->", "to do it")
                onItemClickListener?.invoke(dataList[position].link)

            }
        }

    }

    override fun getItemCount(): Int = dataList.size

    inner class ItemViewHolder(val binding: ItemHomeArticleBinding) :
        RecyclerView.ViewHolder(binding.root)
}