package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityHomeBinding
import com.dian.demo.databinding.ActivityTodoListBinding
import com.dian.demo.ui.adapter.TodoPagerAdapter
import com.dian.demo.ui.fragment.HomeFragment
import com.dian.demo.ui.fragment.SettingFragment
import com.dian.demo.ui.fragment.TodoListFragment

class TodoListActivity : BaseAppBindActivity<ActivityTodoListBinding>() {
    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, TodoListActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    private val fragmentList by lazy { listOf(TodoListFragment.getFragment()) }

    override fun getLayoutId(): Int = R.layout.activity_todo_list

    override fun initialize(savedInstanceState: Bundle?) {
        binding.vpContent.adapter = TodoPagerAdapter(fragmentList, supportFragmentManager)
    }
}