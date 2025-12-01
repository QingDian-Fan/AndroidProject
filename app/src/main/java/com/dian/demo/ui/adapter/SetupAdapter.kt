package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.databinding.ItemGlobalArticleBinding
import com.dian.demo.databinding.ItemNavigationLayoutBinding
import com.dian.demo.databinding.ItemSerachFowlayoutBinding
import com.dian.demo.di.model.SetUpData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
class SetupAdapter(
    private val isSetUp: Boolean,
    sharedPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool()
) : RecyclerView.Adapter<SetupAdapter.ItemViewHolder>() {

    private val pool = sharedPool

    private val diffCallback = object : DiffUtil.ItemCallback<SetUpData>() {
        override fun areItemsTheSame(oldItem: SetUpData, newItem: SetUpData): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: SetUpData, newItem: SetUpData): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<SetUpData>) {
        differ.submitList(list)
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemNavigationLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding, isSetUp, pool)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    inner class ItemViewHolder(
        private val binding: ItemNavigationLayoutBinding,
        private val isSetUp: Boolean,
        private val sharedPool: RecyclerView.RecycledViewPool
    ) : RecyclerView.ViewHolder(binding.root) {

        private val childAdapter = SetUpChildrenAdapter(isSetUp)

        init {
            binding.rvData.apply {
                layoutManager = FlexboxLayoutManager(context).apply {
                    flexDirection = FlexDirection.ROW
                    flexWrap = FlexWrap.WRAP
                    justifyContent = JustifyContent.FLEX_START
                }
                adapter = childAdapter
                setRecycledViewPool(sharedPool)
                isNestedScrollingEnabled = false
                setHasFixedSize(false)
            }
        }

        fun bind(item: SetUpData) {
            binding.tvTitle.text = item.name

            val list =
                if (isSetUp) item.children
                else item.articles

            childAdapter.submitList(list ?: emptyList())
        }
    }
}

