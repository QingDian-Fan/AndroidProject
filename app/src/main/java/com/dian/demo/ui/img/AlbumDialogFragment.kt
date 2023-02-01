package com.dian.demo.ui.img

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dian.demo.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class AlbumDialogFragment : BottomSheetDialogFragment() {

    private var rvData: RecyclerView? = null

    lateinit var onChooseAlbumListener: (info: AlbumInfo) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: Dialog = super.onCreateDialog(savedInstanceState)

        val view: View = View.inflate(context, R.layout.dialog_album, null)
        dialog.setContentView(view)
        initData(view)
        return dialog
    }

    private fun initData(mView: View) {
        rvData = mView.findViewById(R.id.rv_data)
        val dataList = arguments?.getParcelableArrayList<AlbumInfo>("dataList")
        rvData?.layoutManager = LinearLayoutManager(context)
        val mAdapter = AlbumAdapter(requireContext(), dataList!!)
        rvData?.adapter = mAdapter

        mAdapter.onItemClickListener = { _, info ->
            onChooseAlbumListener.invoke(info)
            rvData?.postDelayed({
                dismissAllowingStateLoss()
            }, 100)

        }
    }


}