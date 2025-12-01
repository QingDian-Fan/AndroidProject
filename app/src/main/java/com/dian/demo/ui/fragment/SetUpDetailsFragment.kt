package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentSetUpDetailsBinding


class SetUpDetailsFragment : BaseAppBindFragment<FragmentSetUpDetailsBinding>() {

    override fun getLayoutId(): Int = R.layout.fragment_set_up_details

    override fun initialize(savedInstanceState: Bundle?) {
        TODO("Not yet implemented")
    }

    companion object {

        @JvmStatic
        fun getFragment() =SetUpDetailsFragment()
    }
}