package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.databinding.ItemCoinRecordBinding
import com.dian.demo.di.model.CoinCount


class CoinRecordAdapter: RecyclerView.Adapter<CoinRecordAdapter.ItemViewHolder>() {


    private val diffCallback = object : DiffUtil.ItemCallback<CoinCount>() {
        override fun areItemsTheSame(oldItem: CoinCount, newItem: CoinCount): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CoinCount, newItem: CoinCount): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    val currentList: List<CoinCount>
        get() = differ.currentList

    fun submitList(list: List<CoinCount>?) {
        differ.submitList(list)
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemCoinRecordBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = differ.currentList[position]

        with(holder) {
            val desc: String = item.desc
            val firstSpace = desc.indexOf(" ")
            val secondSpace = desc.indexOf(" ", firstSpace + 1)
            val time = desc.substring(0, secondSpace)
            val title = desc.substring(secondSpace + 1)
                .replace(",", "")
                .replace("：", "")
                .replace(" ", "")
            binding.tvTitle.text = title
            binding.tvCoinCount.text = "+${item.coinCount}"
            binding.tvTime.text = time
        }
    }

    inner class ItemViewHolder(val binding: ItemCoinRecordBinding) :
        RecyclerView.ViewHolder(binding.root)
}