package com.dian.demo.ui.img

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dian.demo.databinding.ItemImageSelectBinding
import com.dian.demo.ui.img.ImageSelectAdapter.*
import com.dian.demo.utils.ext.singleClick
import java.util.ArrayList

class ImageSelectAdapter(
    private val mContext: Context,
    private var dataList: ArrayList<String>,
    private val selectList: ArrayList<String>
) : RecyclerView.Adapter<ItemViewHolder>() {

    lateinit var onSelectListener: (pos: Int, url: String) -> Unit
    lateinit var unSelectListener: (pos: Int, url: String) -> Unit
    lateinit var onClickListener: (pos: Int, url: String) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemImageSelectBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        with(holder) {

            binding.cbImageSelectCheck.isChecked = selectList.contains(dataList[position])

            Glide.with(mContext)
                .asBitmap()
                .load(dataList[position])
                .into(binding.imgView)
            itemView.singleClick {
                onClickListener(position, dataList[position])
            }
            binding.flImageSelectCheck.singleClick {
                binding.cbImageSelectCheck.isChecked = !selectList.contains(dataList[position])
                if (selectList.contains(dataList[position])) {
                    unSelectListener.invoke(position, dataList[position])
                } else {
                    onSelectListener.invoke(position, dataList[position])
                }
            }
        }

    }

    override fun getItemCount(): Int = dataList.size

    fun getDataList(): ArrayList<String> {
        return dataList
    }

    fun getData(): Any = dataList

    fun setData(dataList:ArrayList<String>){
        this.dataList=dataList
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(var binding: ItemImageSelectBinding) :
        RecyclerView.ViewHolder(binding.root)
}