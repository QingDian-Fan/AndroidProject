package com.dian.demo.ui.img

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.databinding.ItemImageSelectBinding
import com.dian.demo.ui.img.ImageSelectAdapter.*
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.ext.singleClick
import java.util.ArrayList

class ImageSelectAdapter(
    private val mContext: Context,
    private var dataList: ArrayList<String>,
    private val selectList: ArrayList<String>,
    private val isMulti: Boolean = false,
    private val maxSelect: Int = 9
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


            if (isMulti) {
                binding.cbSingleImageSelectCheck.visibility = gone
                binding.cbMultiImageSelectCheck.visibility = visible
                val index: Int = selectList.indexOf(dataList[position])
                if (index != -1) {
                    binding.cbMultiImageSelectCheck.text = (index + 1).toString()
                    binding.cbMultiImageSelectCheck.background =
                        ResourcesUtil.getDrawable(R.drawable.icon_multi_checkbox_checked)
                } else {
                    binding.cbMultiImageSelectCheck.text = ""
                    binding.cbMultiImageSelectCheck.background =
                        ResourcesUtil.getDrawable(R.drawable.icon_checkbox_uncheck)
                }
            } else {
                binding.cbSingleImageSelectCheck.visibility = visible
                binding.cbMultiImageSelectCheck.visibility = gone
                binding.cbSingleImageSelectCheck.isChecked = selectList.contains(dataList[position])
            }


            Glide.with(mContext)
                .asBitmap()
                .load(dataList[position])
                .into(binding.imgView)
            itemView.singleClick {
                onClickListener(position, dataList[position])
            }
            binding.flImageSelectCheck.singleClick {
                if (!isMulti) {
                    binding.cbSingleImageSelectCheck.isChecked =
                        !selectList.contains(dataList[position])
                }
                if (selectList.contains(dataList[position])) {
                    unSelectListener.invoke(position, dataList[position])
                } else {
                    if (isMulti) {
                        if (selectList.size >= maxSelect){
                            ToastUtil.showToast(mContext,ResourcesUtil.getString(R.string.text_max_select,maxSelect))
                            return@singleClick
                        }
                        onSelectListener.invoke(position, dataList[position])
                        binding.cbMultiImageSelectCheck.text = (selectList.size).toString()
                        binding.cbMultiImageSelectCheck.background =
                            ResourcesUtil.getDrawable(R.drawable.icon_multi_checkbox_checked)
                    }else{
                        selectList.clear()
                        onSelectListener.invoke(position, dataList[position])
                        notifyDataSetChanged()
                    }

                }
            }
        }

    }

    override fun getItemCount(): Int = dataList.size

    fun getDataList(): ArrayList<String> {
        return dataList
    }

    fun getData(): Any = dataList

    fun setData(dataList: ArrayList<String>) {
        this.dataList = dataList
        notifyDataSetChanged()
    }

    inner class ItemViewHolder(var binding: ItemImageSelectBinding) :
        RecyclerView.ViewHolder(binding.root)
}