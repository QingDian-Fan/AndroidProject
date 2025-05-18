package com.dian.demo.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2
import com.dian.demo.R
import com.dian.demo.di.model.CityData
import com.dian.demo.ui.adapter.AddressPageAdapter
import com.dian.demo.ui.adapter.AddressTabAdapter
import com.dian.demo.ui.adapter.AddressType
import com.dian.demo.ui.view.NoScrollViewPager
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.ext.singleClick
import org.json.JSONArray
import org.json.JSONException
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mView = LayoutInflater.from(context).inflate(R.layout.dialog_address, null, false)

        initView(mView)
        return mView
    }

    private fun initView(mView: View) {
        val tvTitle: AppCompatTextView by lazy { mView.findViewById(R.id.tv_title) }
        val ivClose: AppCompatImageView by lazy { mView.findViewById(R.id.iv_close) }
        val rvAddressData: RecyclerView by lazy { mView.findViewById(R.id.rv_address_data) }
        val vpAddressPage: ViewPager2 by lazy { mView.findViewById(R.id.vp_address_page) }

        var province: String? = null
        var city: String? = null
        var area: String?


        ivClose.singleClick {
            dismissAllowingStateLoss()
        }

        vpAddressPage.offscreenPageLimit = 3
        val mPageAdapter = AddressPageAdapter(requireContext())
        vpAddressPage.adapter = mPageAdapter
        vpAddressPage.isUserInputEnabled = false
        vpAddressPage.post {
            mPageAdapter.setItemData(AddressType.PROVINCE, getProvinces())
        }


        rvAddressData.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        val tabList = mutableListOf<String>()
        val tabAdapter = AddressTabAdapter(tabList)
        rvAddressData.adapter = tabAdapter
        tabAdapter.onItemClick = {
            while (it < tabList.size) {
                tabList.removeAt(it)
            }
            tabAdapter.notifyDataSetChanged()
            vpAddressPage.setCurrentItem(it, true)

        }


        mPageAdapter.onPageItemClick = { name, type, value ->
            when (type) {
                AddressType.PROVINCE -> {
                    mPageAdapter.setItemData(AddressType.CITY, getCities(value!!))
                    vpAddressPage.setCurrentItem(1, true)
                    tabList.add(name)
                    tabAdapter.notifyDataSetChanged()
                    province = name
                }
                AddressType.CITY -> {
                    mPageAdapter.setItemData(AddressType.AREA, getAreas(value!!))
                    vpAddressPage.setCurrentItem(2, true)
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