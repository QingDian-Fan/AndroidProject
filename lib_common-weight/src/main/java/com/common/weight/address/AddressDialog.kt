package com.common.weight.address

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.common.utils.ToastUtil
import com.common.utils.ext.singleClick
import com.common.weight.R
import com.common.weight.databinding.DialogAddressBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class AddressDialog : AppCompatDialogFragment() {

    companion object {
        fun getDialog(): AppCompatDialogFragment {
            return AddressDialog()
        }
    }

    private var _binding: DialogAddressBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddressBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initView() {
        var province: String? = null
        var city: String? = null
        var area: String?

        binding.ivClose.singleClick {
            dismissAllowingStateLoss()
        }

        binding.vpAddressPage.offscreenPageLimit = 3
        val mPageAdapter = AddressPageAdapter(requireContext())
        binding.vpAddressPage.adapter = mPageAdapter
        binding.vpAddressPage.isUserInputEnabled = false
        binding.vpAddressPage.post {
            mPageAdapter.setItemData(AddressType.PROVINCE, getProvinces())
        }

        binding.rvAddressData.layoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val tabList = mutableListOf<String>()
        val tabAdapter = AddressTabAdapter(tabList)
        binding.rvAddressData.adapter = tabAdapter
        tabAdapter.onItemClick = {
            while (it < tabList.size) {
                tabList.removeAt(it)
            }
            tabAdapter.notifyDataSetChanged()
            binding.vpAddressPage.setCurrentItem(it, true)
        }

        mPageAdapter.onPageItemClick = { name, type, value ->
            when (type) {
                AddressType.PROVINCE -> {
                    mPageAdapter.setItemData(AddressType.CITY, getCities(value!!))
                    binding.vpAddressPage.setCurrentItem(1, true)
                    tabList.add(name)
                    tabAdapter.notifyDataSetChanged()
                    province = name
                }
                AddressType.CITY -> {
                    mPageAdapter.setItemData(AddressType.AREA, getAreas(value!!))
                    binding.vpAddressPage.setCurrentItem(2, true)
                    tabList.add(name)
                    tabAdapter.notifyDataSetChanged()
                    city = name
                }
                AddressType.AREA -> {
                    area = name
                    ToastUtil.showToast(str = "$province-$city-$area")
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    private fun getAddressData(): JSONArray {
        val stringBuilder = StringBuilder()
        try {
            val assetsManager = context?.assets
            val bufferReader = BufferedReader(InputStreamReader(assetsManager?.open("city.json")))
            var lineString: String?
            while (bufferReader.readLine().also { lineString = it } != null) {
                stringBuilder.append(lineString)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return JSONArray(stringBuilder.toString())
    }

    private fun getProvinces(): ArrayList<CityData> {
        val jsonArray = getAddressData()
        val length = jsonArray.length()
        val provincesList: ArrayList<CityData> = ArrayList(length)
        for (i in 0 until length) {
            val jsonObject = jsonArray.getJSONObject(i)
            provincesList.add(CityData(jsonObject.getString("name"), jsonObject))
        }
        return provincesList
    }

    private fun getCities(jsonObject: JSONObject): ArrayList<CityData> {
        val listCity = jsonObject.getJSONArray("city")
        val length = listCity.length()
        val list: ArrayList<CityData> = ArrayList(length)
        for (i in 0 until length) {
            list.add(
                CityData(
                    listCity.getJSONObject(i).getString("name"),
                    listCity.getJSONObject(i)
                )
            )
        }
        return list
    }

    /**
     * 获取区域列表
     *
     * @param jsonObject        区域 Json
     */
    private fun getAreas(jsonObject: JSONObject): ArrayList<CityData> {
        val listArea = jsonObject.getJSONArray("area")
        val length = listArea.length()
        val list: ArrayList<CityData> = ArrayList(length)
        for (i in 0 until length) {
            val string = listArea.getString(i)
            list.add(CityData(string, null))
        }
        return list
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
}