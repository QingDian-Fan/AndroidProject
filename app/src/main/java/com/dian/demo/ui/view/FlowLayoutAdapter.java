package com.dian.demo.ui.view;


import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public abstract class FlowLayoutAdapter {
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    // 子view的集合
    private final ArrayList<View> views = new ArrayList<>();

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * Notifies the attached observers that the underlying data has been changed
     * and any View reflecting the data set should refresh itself.
     */
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    // 得到条目数
    public abstract int getItemCount();

    // 根据位置得到子View布局
    public abstract View getItemView(int position, ViewGroup parent);

    // 将子view布局添加到总的list里面
    public void addViewToList(ViewGroup parent) {
        views.clear();
        int counts = getItemCount();
        if (counts == 0) return;

        for (int i = 0; i < counts; i++) {
            views.add(getItemView(i, parent));
        }
    }

    /**
     * 得到列表里面的所有子view
     *
     * @return view集合
     */
    public ArrayList<View> getViewList() {
        return views;
    }
}

