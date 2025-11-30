package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dian.annotation.LoginPage
import com.dian.annotation.RequireLogin
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityLoginBinding

@RequireLogin
class LoginContainerActivity : BaseAppBindActivity<ActivityLoginBinding>() {
    companion object {
        fun start(mContext: Context, mListPage: Int) {
            val intent = Intent()
            intent.setClass(mContext, LoginContainerActivity::class.java)
            intent.putExtra("LIST_PAGE", mListPage)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_login_container

    override fun initialize(savedInstanceState: Bundle?) {
        val mListPage = intent.getIntExtra("LIST_PAGE", 0)
        setPageTitle(if (mListPage==0) "我的分享" else "我的收藏")
        val bundle = Bundle().apply {
            putInt("LIST_PAGE", mListPage)
        }
        supportFragmentManager.setFragmentResult("KEY_LIST_PAGE", bundle)
    }
}