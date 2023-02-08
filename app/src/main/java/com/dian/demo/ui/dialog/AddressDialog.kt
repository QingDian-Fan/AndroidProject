package com.dian.demo.ui.dialog

import android.content.Context
import android.os.Bundle
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
import com.dian.demo.di.model.ProvinceData
import com.dian.demo.ui.adapter.AddressTabAdapter
import com.dian.demo.utils.ext.singleClick
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
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
        val mView = inflater.inflate(R.layout.dialog_address, container, true)
        initView(mView)
        return mView
    }

    private fun initView(mView: View) {
        val tvTitle: AppCompatTextView by lazy { mView.findViewById(R.id.tv_title) }
        val ivClose: AppCompatImageView by lazy { mView.findViewById(R.id.iv_close) }
        val rvAddressData: RecyclerView by lazy { mView.findViewById(R.id.rv_address_data) }
        val vpAddressPage: ViewPager2 by lazy { mView.findViewById(R.id.vp_address_page) }

        ivClose.singleClick {
            dismissAllowingStateLoss()
        }

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val jsonAdapter = moshi.adapter(ArrayList<ProvinceData>().javaClass)
        var provinces = jsonAdapter.fromJson(getAddressData())


        rvAddressData.layoutManager = LinearLayoutManager(context,HORIZONTAL,false)
        val tabList = mutableListOf<String>()
        val tabAdapter = AddressTabAdapter(tabList)
        rvAddressData.adapter = tabAdapter




    }

    private fun getAddressData(): String {
        val assetsManager = context?.assets
        val bufferReader = BufferedReader(InputStreamReader(assetsManager?.open("city.json")))
        var lineString: String
        val stringBuilder = StringBuilder()
        while (bufferReader.readLine().also { lineString = it } != null) {
            stringBuilder.append(lineString)
        }
        return stringBuilder.toString()
    }




}