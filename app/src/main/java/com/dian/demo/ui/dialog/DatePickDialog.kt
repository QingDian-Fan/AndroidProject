package com.dian.demo.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.R
import com.dian.demo.databinding.DialogDatePickBinding
import com.dian.demo.ui.adapter.PickDateAdapter
import com.dian.demo.ui.view.PickerLayoutManager
import com.dian.demo.ui.view.PickerLayoutManager.*
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.ext.singleClick
import java.util.*

class DatePickDialog : AppCompatDialogFragment(), Runnable, OnPickerListener {

    companion object {
        fun getDialog(): DatePickDialog {
            return DatePickDialog()
        }
    }


    var onSelected: ((year: Int, month: Int, day: Int,timeInMillis:Long) -> Unit)? = null
    var onCancel: (() -> Unit)? = null

    private lateinit var binding: DialogDatePickBinding
    private var startYear: Int = Calendar.getInstance(Locale.CHINA)[Calendar.YEAR] - 100
    private var endYear: Int = Calendar.getInstance(Locale.CHINA)[Calendar.YEAR]
    private lateinit var mYearAdapter: PickDateAdapter
    private lateinit var mMonthAdapter: PickDateAdapter
    private lateinit var mDateAdapter: PickDateAdapter
    private val yearManager by lazy {
        Builder(requireContext()).build()
    }
    private val monthManager by lazy {
        Builder(requireContext()).build()
    }
    private val dateManager by lazy {
        Builder(requireContext()).build()
    }
    private val yearData by lazy {
        ArrayList<String>(10)
    }
    private val monthData by lazy {
        ArrayList<String>(10)
    }
    private val dayData by lazy {
        ArrayList<String>(10)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DialogDatePickBinding.inflate(inflater, container, false)
        initData()
        return binding.root
    }

    private fun initData() {
        with(binding) {
            tvTitle.text = "请选择日期"
            binding.tvCancel.singleClick {
                dismissAllowingStateLoss()
                onCancel?.invoke()
            }
            binding.tvConfirm.singleClick {
                val calendar: Calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, startYear+yearManager.getPickedPosition())
                calendar.set(Calendar.MONTH, monthManager.getPickedPosition())
                calendar.set(Calendar.DAY_OF_MONTH, dateManager.getPickedPosition()+1)
                onSelected?.invoke(startYear+yearManager.getPickedPosition(),monthManager.getPickedPosition() + 1,dateManager.getPickedPosition() + 1,calendar.timeInMillis)
                dismissAllowingStateLoss()
            }


            rvYear.layoutManager = yearManager
            rvMonth.layoutManager = monthManager
            rvDate.layoutManager = dateManager

            // 生产年份
            for (i in startYear..endYear) {
                yearData.add("$i 年")
            }

            // 生产月份
            for (i in 1..12) {
                monthData.add("$i 月")
            }

            val calendar = Calendar.getInstance(Locale.CHINA)
            val day = calendar.getActualMaximum(Calendar.DATE)
            // 生产天数
            for (i in 1..day) {
                dayData.add("$i 日")
            }

            mYearAdapter = PickDateAdapter(yearData)
            rvYear.adapter = mYearAdapter
            mMonthAdapter = PickDateAdapter(monthData)
            rvMonth.adapter = mMonthAdapter
            mDateAdapter = PickDateAdapter(dayData)
            rvDate.adapter = mDateAdapter

            monthManager.setOnPickerListener(this@DatePickDialog)
            setYear(calendar[Calendar.YEAR])
            setMonth(calendar[Calendar.MONTH] + 1)
            setDay(calendar[Calendar.DAY_OF_MONTH])
        }

    }


    private fun setYear(year: Int) = apply {
        var index = year - startYear
        if (index < 0) {
            index = 0
        } else if (index > mYearAdapter.itemCount - 1) {
            index = mYearAdapter.itemCount - 1
        }
        binding.rvYear.scrollToPosition(index)

    }

    private fun setMonth(month: Int) = apply {
        var index = month - 1
        if (index < 0) {
            index = 0
        } else if (index > mMonthAdapter.itemCount - 1) {
            index = mMonthAdapter.itemCount - 1
        }
        binding.rvMonth.scrollToPosition(index)
        binding.rvMonth.removeCallbacks(this)
        binding.rvMonth.post(this)
    }

    private fun setDay(day: Int) = apply {
        var index = day - 1
        if (index < 0) {
            index = 0
        } else if (index > mDateAdapter.itemCount - 1) {
            index = mDateAdapter.itemCount - 1
        }
        binding.rvDate.scrollToPosition(index)

    }


    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog!!.setCanceledOnTouchOutside(false)
            if (dialog!!.window != null) {
                dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
                val params = dialog!!.window?.attributes
                params?.width = ViewGroup.LayoutParams.MATCH_PARENT
                params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                params?.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                dialog!!.window?.attributes = params
            }
        }
    }

    override fun run() {
        val calendar = Calendar.getInstance(Locale.CHINA)
        calendar[startYear + yearManager.getPickedPosition(), monthManager.getPickedPosition()] =
            1
        val day = calendar.getActualMaximum(Calendar.DATE)
        if (mDateAdapter.itemCount != day) {
            dayData.clear()
            for (i in 1..day) {
                dayData.add("$i 日")
            }
            mDateAdapter.notifyDataSetChanged()
        }
    }

    override fun onPicked(recyclerView: RecyclerView, position: Int) {
        binding.rvMonth.removeCallbacks(this)
        binding.rvMonth.post(this)
    }

}