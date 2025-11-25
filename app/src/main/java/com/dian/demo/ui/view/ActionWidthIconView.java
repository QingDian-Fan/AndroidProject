package com.dian.demo.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class ActionWidthIconView extends AppCompatImageView {

    public ActionWidthIconView(Context context) {
        this(context, null);
    }

    public ActionWidthIconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionWidthIconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(widthSize, widthSize);
    }
}
