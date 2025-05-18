package com.dian.demo.ui.view.status;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;

public class SuccessLayout extends SmartRefreshLayout {

    public SuccessLayout(Context context) {
        super(context);
    }

    public SuccessLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
    }
}
