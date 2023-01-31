package com.dian.demo.utils;

import androidx.annotation.Nullable;

import com.scwang.smart.refresh.layout.api.RefreshLayout;


public class SmartRefreshUtil {
    private static final int HTTP_TIMEOUT = 5000;
    private final RefreshLayout mRefreshLayout;
    private RefreshListener mRefreshListener = null;
    private LoadMoreListener mLoadMoreListener = null;

    public static SmartRefreshUtil with(RefreshLayout layout) {
        return new SmartRefreshUtil(layout);
    }

    private SmartRefreshUtil(RefreshLayout layout) {
        mRefreshLayout = layout;
        mRefreshLayout.setEnableAutoLoadMore(false);
        mRefreshLayout.setEnableOverScrollBounce(true);
    }

    public SmartRefreshUtil setScrollMode() {
        mRefreshLayout.setEnableRefresh(false);
        mRefreshLayout.setEnableLoadMore(false);
        mRefreshLayout.setEnablePureScrollMode(true);
        mRefreshLayout.setEnableNestedScroll(true);
        mRefreshLayout.setEnableOverScrollDrag(true);
        return this;
    }

    public SmartRefreshUtil setDefaultMode() {
        mRefreshLayout.setEnableRefresh(false);
        mRefreshLayout.setEnableLoadMore(false);
        mRefreshLayout.setEnablePureScrollMode(false);
        mRefreshLayout.setEnableNestedScroll(false);
        mRefreshLayout.setEnableOverScrollDrag(false);
        return this;
    }

    public SmartRefreshUtil setRefreshListener(@Nullable RefreshListener refreshListener) {
        this.mRefreshListener = refreshListener;
        if (refreshListener == null) {
            mRefreshLayout.setEnableRefresh(false);
        } else {
            mRefreshLayout.setEnablePureScrollMode(false);
            mRefreshLayout.setEnableRefresh(true);
            mRefreshLayout.setOnRefreshListener(refreshLayout -> {
                refreshLayout.finishRefresh(HTTP_TIMEOUT, false, false);
                mRefreshListener.onRefresh();
            });
        }
        return this;
    }

    public SmartRefreshUtil setLoadMoreListener(LoadMoreListener loadMoreListener) {
        this.mLoadMoreListener = loadMoreListener;
        if (loadMoreListener == null) {
            mRefreshLayout.setEnableLoadMore(false);
        } else {
            mRefreshLayout.setEnablePureScrollMode(false);
            mRefreshLayout.setEnableLoadMore(true);
            mRefreshLayout.setOnLoadMoreListener(refreshLayout -> {
                refreshLayout.finishLoadMore(HTTP_TIMEOUT);
                mLoadMoreListener.onLoadMore();
            });
        }
        return this;
    }

    public void autoRefresh() {
        mRefreshLayout.autoRefresh();
    }

    public void autoLoadMore() {
        mRefreshLayout.autoLoadMore();
    }

    public void success() {
        mRefreshLayout.finishRefresh(true);
        mRefreshLayout.finishLoadMore(true);
    }

    public void fail() {
        mRefreshLayout.finishRefresh(false);
        mRefreshLayout.finishLoadMore(false);
    }

    public interface RefreshListener {
        void onRefresh();
    }

    public interface LoadMoreListener {
        void onLoadMore();
    }
}
