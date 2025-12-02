package com.dian.demo.ui.view;


import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private List<List<View>> mChildViews = new ArrayList<>();
    private ArrayList<View> childViews = new ArrayList<>();//每一行的View
    private int lines;
    private int mCurrentLines;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //判断用户是否设置宽度为wrap_content，如果是wrap_content则抛出异常
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
            throw new RuntimeException("FlowLayout does not allow setting layout_width to wrap_content");
        }
        mCurrentLines = 0;
        //获取View个数
        int childCount = getChildCount();
        //获取宽度
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        //计算高度
        int heightSize = getPaddingTop() + getPaddingBottom();
        int lineWidth = 0;
        int maxLineHeight = 0;
        mChildViews.clear();
        childViews.clear();
        //循环所有的View计算高度  注意处理Gone,padding margin
        for (int i = 0; i < childCount; i++) {
            //获取子View
            View mChildView = getChildAt(i);
            if (mChildView.getVisibility() == GONE) continue;
            //这句话执行完毕之后  就可以获取子View的宽高了 因为这句话会调用子View 的measure方法
            measureChild(mChildView, widthMeasureSpec, heightMeasureSpec);
            //获取layoutParams  计算最大高度
            MarginLayoutParams layoutParams = (MarginLayoutParams) mChildView.getLayoutParams();
            maxLineHeight = Math.max(maxLineHeight, mChildView.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin);
            int mChildWidth = mChildView.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin;

            if (lineWidth + mChildWidth > widthSize - getPaddingLeft() - getPaddingRight()) {
                // 需要换行
                heightSize += maxLineHeight;
                maxLineHeight = 0;
                lineWidth = mChildWidth;
                mChildViews.add(childViews);
                childViews = new ArrayList<>();
                mCurrentLines++;
                if (lines != 0 && mCurrentLines == lines) break;
            } else {
                //未满一行不需要换行
                lineWidth += mChildWidth;
            }

            childViews.add(mChildView);
            if (i == (childCount - 1)) {
                heightSize += maxLineHeight;
                mChildViews.add(childViews);
            }
        }
        //设置自己的宽高
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int top = getPaddingTop();

        for (List<View> lineViews : mChildViews) {
            int left = getPaddingLeft();
            int maxHeight = 0;

            for (View child : lineViews) {
                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                int childLeft = left + lp.leftMargin;
                int childTop = top + lp.topMargin;
                int childRight = childLeft + child.getMeasuredWidth();
                int childBottom = childTop + child.getMeasuredHeight();

                child.layout(childLeft, childTop, childRight, childBottom);

                left += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;

                maxHeight = Math.max(maxHeight,
                        child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            }

            top += maxHeight; // 换到下一行
        }
    }


    public void setLines(int lines) {
        this.lines = lines;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    private FlowLayoutAdapter mAdapter;
    private DataSetObserver mDataSetObserver;


    public void setAdapter(FlowLayoutAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            //注销观察者
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mAdapter = null;
        }

        if (adapter == null) throw new NullPointerException("adapter does not allow is null");


        this.mAdapter = adapter;
        //刷新数据
        mDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                resetLayout();
            }
        };
        //注册观察者
        mAdapter.registerDataSetObserver(mDataSetObserver);
        resetLayout();
    }

    protected final void resetLayout() {
        this.removeAllViews();
        int counts = mAdapter.getItemCount();
        mAdapter.addViewToList(this);
        ArrayList<View> views = mAdapter.getViewList();
        for (int i = 0; i < counts; i++) {
            this.addView(views.get(i));
        }
    }
}

