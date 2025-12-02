package com.dian.demo.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.databinding.ItemGlobalArticleBinding
import com.dian.demo.databinding.ItemNavigationLayoutBinding
import com.dian.demo.databinding.ItemSerachFowlayoutBinding
import com.dian.demo.di.model.NavigationData
import com.dian.demo.di.model.SetUpData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

class SetupAdapter(
    private val isSetUp: Boolean,
    sharedPool: RecyclerView.RecycledViewPool = RecyclerView.RecycledViewPool(),
) : RecyclerView.Adapter<SetupAdapter.ItemViewHolder>() {

    private val pool = sharedPool

    private var listener: ((isSetup: Boolean, titleList: List<NavigationData>?, data: NavigationData?) -> Unit)? =
        null

    fun setListener(listener: (isSetup: Boolean, titleList: List<NavigationData>?, data: NavigationData?) -> Unit) {
        this.listener = listener
    }

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
        return ItemViewHolder(
            ItemNavigationLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = differ.currentList[position]
        with(holder) {
            binding.tvTitle.text = item.name ?: ""
            val mList = if (isSetUp) item.children
            else item.articles
            mList?.toMutableList()?.let {
                val mFlowAdapter = KnowledgeFlowAdapter(binding.root.context, it, isSetUp)
                mFlowAdapter.setClickListener {
                    listener?.invoke(isSetUp,mList,it)
                }
                binding.flLayout.setAdapter(mFlowAdapter)
            }
        }
    }

    inner class ItemViewHolder(val binding: ItemNavigationLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)
}

