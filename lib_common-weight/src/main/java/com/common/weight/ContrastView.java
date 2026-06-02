package com.common.weight;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;



/**
 * 没有实现圆角
 * 配合CardView使用
 */
public class ContrastView extends View {
    private Paint mLeftPaint;
    private Paint mRightPaint;
    private Path mLeftPath;
    private Path mRightPath;


    private int mLeftColor;
    private int mRightColor;
    private float mMiddleInterval;
    private float mLeftValue;
    private float mRightValue;

    private float radiusArc;


    public ContrastView(Context context) {
        this(context, null);
    }

    public ContrastView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContrastView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ContrastView);
        mLeftColor = ta.getColor(R.styleable.ContrastView_cv_left_color, Color.parseColor("#FF11A876"));
        mRightColor = ta.getColor(R.styleable.ContrastView_cv_right_color, Color.parseColor("#FFD93D57"));
        mMiddleInterval = ta.getDimension(R.styleable.ContrastView_cv_middle_interval, dp2px(context, 4));
        mLeftValue = ta.getFloat(R.styleable.ContrastView_cv_left_value, 0);
        mRightValue = ta.getFloat(R.styleable.ContrastView_cv_right_value, 0);
        radiusArc = ta.getDimension(R.styleable.ContrastView_cv_radius_arc, dp2px(context, 5));
        ta.recycle();
        init();
    }

    private void init() {
        mLeftPaint = new Paint();
        mLeftPaint.setColor(mLeftColor);
        mLeftPaint.setAntiAlias(true);
        mLeftPaint.setStyle(Paint.Style.FILL);
        mLeftPaint.setStrokeCap(Paint.Cap.ROUND);

        mRightPaint = new Paint();
        mRightPaint.setColor(mRightColor);
        mRightPaint.setAntiAlias(true);
        mRightPaint.setStyle(Paint.Style.FILL);
        mRightPaint.setStrokeCap(Paint.Cap.ROUND);

        mLeftPath = new Path();

        mRightPath = new Path();


    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mLeftValue == 0 && mRightValue == 0) {
            mLeftValue = 1;
            mRightValue = 1;
        }

        if (mLeftValue == 0 && (mRightValue != 0)) {
            mRightPath.moveTo(0, 0);
            mRightPath.lineTo(0, getHeight());
            mRightPath.lineTo(getWidth(), getHeight());
            mRightPath.lineTo(getWidth(), 0);
            canvas.drawPath(mRightPath, mRightPaint);
            return;
        }

        if (mRightValue == 0 && (mLeftValue != 0)) {
            mLeftPath.moveTo(0, 0);
            mLeftPath.lineTo(0, getHeight());
            mLeftPath.lineTo(getWidth(), getHeight());
            mLeftPath.lineTo(getWidth(), 0);
            canvas.drawPath(mLeftPath, mLeftPaint);
            return;
        }

        float totalValue = mLeftValue + mRightValue;
        float middleIndex = getWidth() * mLeftValue / totalValue;

        mLeftPath.moveTo(0, 0);
        mLeftPath.lineTo(0, getHeight());
        mLeftPath.lineTo(middleIndex - mMiddleInterval, getHeight());
        mLeftPath.lineTo(middleIndex, 0);
        canvas.drawPath(mLeftPath, mLeftPaint);
        mLeftPath.reset();


        mRightPath.moveTo(middleIndex + mMiddleInterval, 0);
        mRightPath.lineTo(middleIndex, getHeight());
        mRightPath.lineTo(getWidth(), getHeight());
        mRightPath.lineTo(getWidth(), 0);
        canvas.drawPath(mRightPath, mRightPaint);
    }

    protected float dp2px(Context mContext, float dp) {
        float scale = mContext.getResources().getDisplayMetrics().density;
        return (dp * scale);
    }

    public void setColors(int mLeftColor, int mRightColor) {
        mLeftPaint.setColor(mLeftColor);
        mRightPaint.setColor(mRightColor);
        invalidate();
    }

    public void setValues(float mLeftValue, Float mRightValue) {
        this.mLeftValue = mLeftValue;
        this.mRightValue = mRightValue;
        invalidate();
    }

    public void setMiddleInterval(float mIntervalDp) {
        mMiddleInterval = dp2px(getContext(), mIntervalDp);
        invalidate();
    }
}

