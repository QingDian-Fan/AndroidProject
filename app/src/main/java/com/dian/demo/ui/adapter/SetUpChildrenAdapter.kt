package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.databinding.ItemSerachFowlayoutBinding
import com.dian.demo.databinding.ItemSetupNavigationBinding
import com.dian.demo.di.model.NavigationData
class SetUpChildrenAdapter(
    private val isSetUp: Boolean
) : RecyclerView.Adapter<SetUpChildrenAdapter.ItemViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<NavigationData>() {
        override fun areItemsTheSame(oldItem: NavigationData, newItem: NavigationData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NavigationData, newItem: NavigationData): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<NavigationData>) {
        differ.submitList(list)
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemSetupNavigationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.tvContent.text = if (isSetUp) item.name else item.title
    }

    inner class ItemViewHolder(val binding: ItemSetupNavigationBinding) :
        RecyclerView.ViewHolder(binding.root)
}
