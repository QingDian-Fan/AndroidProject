package com.common.utils

import com.scwang.smart.refresh.layout.api.RefreshLayout

class SmartRefreshUtil private constructor(private val refreshLayout: RefreshLayout) {

    private var refreshListener: RefreshListener? = null
    private var loadMoreListener: LoadMoreListener? = null

    init {
        refreshLayout.setEnableAutoLoadMore(false)
        refreshLayout.setEnableOverScrollBounce(true)
    }

    fun setScrollMode(): SmartRefreshUtil {
        refreshLayout.setEnableRefresh(false)
        refreshLayout.setEnableLoadMore(false)
        refreshLayout.setEnablePureScrollMode(true)
        refreshLayout.setEnableNestedScroll(true)
        refreshLayout.setEnableOverScrollDrag(true)
        return this
    }

    fun setDefaultMode(): SmartRefreshUtil {
        refreshLayout.setEnableRefresh(false)
        refreshLayout.setEnableLoadMore(false)
        refreshLayout.setEnablePureScrollMode(false)
        refreshLayout.setEnableNestedScroll(false)
        refreshLayout.setEnableOverScrollDrag(false)
        return this
    }

    fun setRefreshListener(refreshListener: RefreshListener?): SmartRefreshUtil {
        this.refreshListener = refreshListener
        if (refreshListener == null) {
            refreshLayout.setEnableRefresh(false)
        } else {
            refreshLayout.setEnablePureScrollMode(false)
            refreshLayout.setEnableRefresh(true)
            refreshLayout.setOnRefreshListener { layout ->
                layout.finishRefresh(HTTP_TIMEOUT, false, false)
                refreshListener.onRefresh()
            }
        }
        return this
    }

    fun setLoadMoreListener(loadMoreListener: LoadMoreListener?): SmartRefreshUtil {
        this.loadMoreListener = loadMoreListener
        if (loadMoreListener == null) {
            refreshLayout.setEnableLoadMore(false)
        } else {
            refreshLayout.setEnablePureScrollMode(false)
            refreshLayout.setEnableLoadMore(true)
            refreshLayout.setOnLoadMoreListener { layout ->
                layout.finishLoadMore(HTTP_TIMEOUT)
                loadMoreListener.onLoadMore()
            }
        }
        return this
    }

    fun autoRefresh() {
        refreshLayout.autoRefresh()
    }

    fun autoLoadMore() {
        refreshLayout.autoLoadMore()
    }

    fun success() {
        refreshLayout.finishRefresh(true)
        refreshLayout.finishLoadMore(true)
    }

    fun fail() {
        refreshLayout.finishRefresh(false)
        refreshLayout.finishLoadMore(false)
    }

    fun interface RefreshListener {
        fun onRefresh()
    }

    fun interface LoadMoreListener {
        fun onLoadMore()
    }

    companion object {
        private const val HTTP_TIMEOUT = 3000

        @JvmStatic
        fun with(layout: RefreshLayout): SmartRefreshUtil = SmartRefreshUtil(layout)
    }
}