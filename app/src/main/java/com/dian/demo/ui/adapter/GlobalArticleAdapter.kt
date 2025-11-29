package com.dian.demo.ui.adapter


import android.text.Html
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dian.demo.databinding.ItemGlobalArticleBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.utils.StringUtils

class GlobalArticleAdapter(val dataList: List<ArticleBean>) : RecyclerView.Adapter<GlobalArticleAdapter.ItemViewHolder>() {

     var onItemClickListener: ((link: String) -> Unit)? = null

    fun setListener(listener: (link: String?) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        return ItemViewHolder(
            ItemGlobalArticleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder.binding){
            val item = dataList[position]
            if (item.top) {
                tvTop.visibility = View.VISIBLE
            } else {
                tvTop.visibility = View.GONE
            }
            if (item.fresh) {
                tvNew.visibility = View.VISIBLE
            } else {
                tvNew.visibility = View.GONE
            }
            tvAuthor.text = item.author
            if (item.tags != null && (item.tags?.isNotEmpty()?:false)) {
                tvTag.text = item.tags?.get(0)?.name
                tvTag.visibility = View.VISIBLE
              
            } else {
                tvTag.visibility = View.GONE
            }
            tvTime.text = item.niceDate
            if (!TextUtils.isEmpty(item.envelopePic)) {
                Glide.with(root.context).load(item.envelopePic)
                    .into(ivImg)
                ivImg.visibility = View.VISIBLE
            } else {
                ivImg.visibility = View.GONE
            }
            tvTitle.text = Html.fromHtml(item.title)
            if (TextUtils.isEmpty(item.desc)) {
                tvDesc.visibility = View.GONE
                tvTitle.isSingleLine = false
            } else {
                tvDesc.visibility = View.VISIBLE
                tvTitle.isSingleLine = true
                var desc: String? = Html.fromHtml(item.desc).toString()
                desc = StringUtils.removeAllBank(desc, 2)
                tvDesc.text = desc
            }
            tvChapterName.text = Html.fromHtml(formatChapterName(item.superChapterName, item.chapterName))
            if (item.collect?:false) {
                cvCollect.setChecked(true, false)
            } else {
                cvCollect.setChecked(false, false)
            }
          root.setOnClickListener {
              WebExplorerActivity.start(root.context,item.link?:"")
          }
          
        }
    }


    override fun getItemCount(): Int = dataList.size

    inner class ItemViewHolder(val binding: ItemGlobalArticleBinding) :
        RecyclerView.ViewHolder(binding.root)

    private fun formatChapterName(vararg names: String?): String {
        val format = StringBuilder()
        for (name in names) {
            if (!TextUtils.isEmpty(name)) {
                if (format.isNotEmpty()) {
                    format.append("·")
                }
                format.append(name)
            }
        }
        return format.toString()
    }
}