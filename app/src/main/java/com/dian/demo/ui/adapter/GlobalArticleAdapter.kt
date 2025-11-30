package com.dian.demo.ui.adapter


import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dian.demo.databinding.ItemGlobalArticleBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.test.proxy.User
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.utils.StringUtils

class GlobalArticleAdapter : RecyclerView.Adapter<GlobalArticleAdapter.ItemViewHolder>() {

    var onItemClickListener: ((link: String) -> Unit)? = null

    fun setListener(listener: (link: String?) -> Unit) { this.onItemClickListener = listener }
    private val diffCallback = object : DiffUtil.ItemCallback<ArticleBean>() {
        override fun areItemsTheSame(oldItem: ArticleBean, newItem: ArticleBean): Boolean {
            return oldItem.id == newItem.id  // 推荐用 id，对 title 不保险
        }

        override fun areContentsTheSame(oldItem: ArticleBean, newItem: ArticleBean): Boolean {
            return oldItem == newItem
        }
    }

    private val mDiffer = AsyncListDiffer(this, diffCallback)
    val currentList: List<ArticleBean>
        get() = mDiffer.currentList

    fun submitList(data: List<ArticleBean>?) {
        mDiffer.submitList(data)
    }

    fun getItem(position: Int): ArticleBean? = mDiffer.currentList.getOrNull(position)

    override fun getItemCount(): Int = mDiffer.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemGlobalArticleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = mDiffer.currentList[position]     // ✔ 使用 Diff 数据
        with(holder.binding) {

            tvTop.visibility = if (item.top) View.VISIBLE else View.GONE
            tvNew.visibility = if (item.fresh) View.VISIBLE else View.GONE
            tvAuthor.text = item.author

            // tag
            if (!item.tags.isNullOrEmpty()) {
                tvTag.text = item.tags?.get(0)?.name
                tvTag.visibility = View.VISIBLE
            } else {
                tvTag.visibility = View.GONE
            }

            tvTime.text = item.niceDate

            // 图片
            if (!item.envelopePic.isNullOrEmpty()) {
                ivImg.visibility = View.VISIBLE
                Glide.with(root.context).load(item.envelopePic).into(ivImg)
            } else {
                ivImg.visibility = View.GONE
            }

            // 标题
            tvTitle.text = Html.fromHtml(item.title)

            // 描述
            if (item.desc.isNullOrEmpty()) {
                tvDesc.visibility = View.GONE
                tvTitle.isSingleLine = false
            } else {
                tvDesc.visibility = View.VISIBLE
                tvTitle.isSingleLine = true
                var desc = Html.fromHtml(item.desc).toString()
                desc = StringUtils.removeAllBank(desc, 2)
                tvDesc.text = desc
            }

            // 分类
            tvChapterName.text = Html.fromHtml(formatChapterName(item.superChapterName, item.chapterName))

            // 收藏状态
            cvCollect.setChecked(item.collect == true, false)

            // 点击
            root.setOnClickListener {
                onItemClickListener?.invoke(item.link ?: "")
            }
        }
    }

    inner class ItemViewHolder(val binding: ItemGlobalArticleBinding) :
        RecyclerView.ViewHolder(binding.root)

    private fun formatChapterName(vararg names: String?): String {
        return names.filterNotNull()
            .filter { it.isNotEmpty() }
            .joinToString("·")
    }
}
