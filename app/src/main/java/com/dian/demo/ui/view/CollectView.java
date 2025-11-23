package com.dian.demo.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.dian.demo.R;

public class CollectView extends RevealLayout {

    private OnClickListener mOnClickListener = null;
    private int mUncheckedColor;

    public CollectView(Context context) {
        this(context, null);
    }

    public CollectView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initAttr(AttributeSet attrs) {
        super.initAttr(attrs);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.CollectView);
        mUncheckedColor = typedArray.getColor(R.styleable.CollectView_cv_uncheckedColor, 0);
        typedArray.recycle();
        setCheckWithExpand(true);
        setUncheckWithExpand(false);
        setAnimDuration(500);
        setAllowRevert(true);
    }

    @Override
    protected int getCheckedLayoutId() {
        return R.layout.layout_collect_view_checked;
    }

    @Override
    protected int getUncheckedLayoutId() {
        return R.layout.layout_collect_view_unchecked;
    }

    @Override
    protected View createUncheckedView() {
        View view = super.createUncheckedView();
        if (view instanceof HeartView) {
            HeartView heartView = (HeartView) view;
            if (mUncheckedColor != 0) {
                heartView.setColor(mUncheckedColor);
                heartView.setEdgeColor(mUncheckedColor);
            }
        }
        return view;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(CollectView.this);
                }
            }
        });
    }

    public interface OnClickListener {
        void onClick(CollectView v);
    }
}
