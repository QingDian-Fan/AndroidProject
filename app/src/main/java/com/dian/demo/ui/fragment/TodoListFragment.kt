package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentTodoListBinding
import com.dian.demo.di.vm.TodoViewModel


class TodoListFragment : BaseAppVMFragment<FragmentTodoListBinding, TodoViewModel>() {

    companion object {
        @JvmStatic
        fun getFragment() = TodoListFragment()
    }

    override fun createViewModel(): TodoViewModel = TodoViewModel()

    override fun getLayoutId(): Int = R.layout.fragment_todo_list

    override fun initialize(savedInstanceState: Bundle?) {
       viewModel.getTodoList(0)
    }
}