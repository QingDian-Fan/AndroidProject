package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
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
            intent.putExtra("mPage", mListPage)
            mContext.startActivity(intent)
        }
        fun start(
            mContext: Context,
            mPage: Int,
            title: String,
            coinCount:String
        ) {
            val intent = Intent()
            intent.setClass(mContext, LoginContainerActivity::class.java)
            intent.putExtra("mPage", mPage)
            intent.putExtra("title", title)
            intent.putExtra("coinCount", coinCount)

            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_login_container

    override fun initialize(savedInstanceState: Bundle?) {
        val mPage = intent.getIntExtra("mPage", 0)
        if (mPage==0||mPage==1) {
            setPageTitle(if (mPage==0) "我的分享" else "我的收藏")
            val bundle = Bundle().apply {
                putInt("LIST_PAGE", mPage)
            }
            supportFragmentManager.setFragmentResult("KEY_LIST_PAGE", bundle)
        }else if (mPage == 2){
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
            val navController = navHostFragment.navController
            val navGraph = navController.navInflater.inflate(R.navigation.navigation_login_mine)
            navGraph.startDestination = R.id.coinRecordFragment
            navController.graph = navGraph

            val title =intent.getStringExtra("title")
            val coinCount =intent.getStringExtra("coinCount")
            runOnUiThread {
                setPageTitle(title?:"")
            }
            val bundle = Bundle().apply {
                putString("coinCount", coinCount)

            }
            supportFragmentManager.setFragmentResult("KEY_COIN_LIST_PAGE", bundle)
            return
        }

    }
}