package com.demo.project.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.common.utils.FastClickUtil

/*
 *
 *Created by : QingDian_Fan
 *Date:  2020/5/9   18:02
 *Email:qingdian_fan@163.com
 * */
abstract class BaseBindingAdapter<M, B : ViewBinding>(
    protected val context: Context,
    items: List<M>
) : RecyclerView.Adapter<BaseBindingAdapter<M, B>.BaseBindingViewHolder>() {

    protected var items: MutableList<M> = ArrayList()

    //接口回调点击事件
    private var itemOnClickListener: ItemOnClickListener? = null
    private var itemOnLongClickListener: ItemOnLongClickListener? = null

    fun interface ItemOnClickListener {
        fun callBack(position: Int)
    }

    fun interface ItemOnLongClickListener {
        fun callBack(position: Int)
    }

    fun setItemOnClickListener(itemOnClickListener: ItemOnClickListener) {
        this.itemOnClickListener = itemOnClickListener
    }

    fun setItemOnLongClickListener(itemOnLongClickListener: ItemOnLongClickListener) {
        this.itemOnLongClickListener = itemOnLongClickListener
    }

    inner class BaseBindingViewHolder(val binding: B) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseBindingViewHolder {
        val binding = getViewBinding(LayoutInflater.from(context), parent, viewType)
        return BaseBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseBindingViewHolder, position: Int) {
        onBindItem(holder.binding, items[position], position)
        holder.itemView.setOnClickListener {
            if (FastClickUtil.isFastClick()) return@setOnClickListener
            itemOnClickListener?.callBack(position)
        }
        holder.itemView.setOnLongClickListener {
            itemOnLongClickListener?.callBack(position)
            itemOnLongClickListener != null
        }
    }

    protected abstract fun getViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): B

    protected abstract fun onBindItem(binding: B, item: M, position: Int)

    @SuppressLint("NotifyDataSetChanged")
    fun setLoadData(list: List<M>) {
        items = list.toMutableList()
        notifyDataSetChanged()
    }
}