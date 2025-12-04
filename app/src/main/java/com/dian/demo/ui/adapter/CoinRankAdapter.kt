package com.dian.demo.ui.adapter

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.R
import com.dian.demo.databinding.ItemCoinRankBinding
import com.dian.demo.di.model.CoinCount
import com.dian.demo.utils.ResourcesUtil
import androidx.core.graphics.toColorInt
import androidx.core.graphics.drawable.toDrawable

class CoinRankAdapter : RecyclerView.Adapter<CoinRankAdapter.ItemViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<CoinCount>() {
        override fun areItemsTheSame(oldItem: CoinCount, newItem: CoinCount): Boolean {
            return oldItem.id == newItem.id  // 推荐用 id，对 title 不保险
        }

        override fun areContentsTheSame(oldItem: CoinCount, newItem: CoinCount): Boolean {
            return oldItem == newItem
        }
    }

    private val mDiffer = AsyncListDiffer(this, diffCallback)
    val currentList: List<CoinCount>
        get() = mDiffer.currentList

    fun submitList(data: List<CoinCount>?) {
        mDiffer.submitList(data)
    }

    fun getItem(position: Int): CoinCount? = mDiffer.currentList.getOrNull(position)

    override fun getItemCount(): Int = mDiffer.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemCoinRankBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }
    private  val mScale = 1000
    private var mMax = 0
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = mDiffer.currentList[position]
        with(holder.binding) {
            pb.setMax(mMax * mScale)
            cancelProgressAnim(pb)
            if (!item.anim) {
                item.anim = true
                doProgressAnim(pb, item.coinCount.toInt())
            } else {
                pb.progress = item.coinCount.toInt() * mScale
            }
            val index: Int = position + 1
            tvIndex.text = " ${index}"
            tvUserName.text = item?.username
            tvCoinCount.text = " ${item.coinCount}"

            when (index) {
                1 -> {
                    ivIndex.setImageResource(R.mipmap.ic_rank_1)
                    tvIndex.setTextColor("#ffca28".toColorInt())
                    tvIndex.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        ResourcesUtil.getDimens(R.dimen.dp_13)
                    )
                }
                2 -> {
                    ivIndex.setImageResource(R.mipmap.ic_rank_2)
                    tvIndex.setTextColor("#cdcdcd".toColorInt())
                    tvIndex.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        ResourcesUtil.getDimens(R.dimen.dp_13)
                    )
                }
                3 -> {
                    ivIndex.setImageResource(R.mipmap.ic_rank_3)
                    tvIndex.setTextColor("#d49682".toColorInt())
                    tvIndex.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        ResourcesUtil.getDimens(R.dimen.dp_13)
                    )
                }
                else -> {
                    ivIndex.setImageDrawable(Color.TRANSPARENT.toDrawable())
                    tvIndex.setTextColor(
                        ResourcesUtil.getColor(R.color.text_light_color)
                    )
                    tvIndex.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        ResourcesUtil.getDimens(R.dimen.dp_15)
                    )
                }
            }
        }
    }
    private fun doProgressAnim(pb: ProgressBar, to: Int) {
        val animator = ValueAnimator.ofInt(0, to)
        animator.setDuration(1000)
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation -> pb.progress = animation.getAnimatedValue() as Int * mScale }
        pb.tag = animator
        animator.start()
    }
    private fun cancelProgressAnim(pb: ProgressBar) {
        val obj = pb.tag
        if (obj is Animator) {
            val animator = obj
            animator.cancel()
        }
        pb.setTag(null)
    }

    inner class ItemViewHolder(val binding: ItemCoinRankBinding) :
        RecyclerView.ViewHolder(binding.root)
}