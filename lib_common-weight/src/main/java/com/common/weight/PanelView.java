package com.common.weight;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.common.utils.ResourcesUtil;


public class PanelView extends View {
    private Paint mArcPaint;
    private Paint mIndicatorPaint;
    private Paint mTextPaint;

    private float mStrokeWidth = 25;
    private int minCircleRadius = 10;
    private float mDegrees;
    private RectF rectF;
    private int mWidth;
    private int mHeight;
    private float textSpace;

    public PanelView(Context context) {
        this(context, null);
    }

    public PanelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mArcPaint = new Paint();
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mStrokeWidth);
        mArcPaint.setAntiAlias(true);

        mIndicatorPaint = new Paint();
        mIndicatorPaint.setAntiAlias(true);
        mIndicatorPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(ResourcesUtil.getColor(R.color.red_5));
        mTextPaint.setTextSize(dp2px(getContext(), 10));

    }


    @Override
    protected void onDraw(Canvas canvas) {
        mWidth = getWidth();
        mHeight = getHeight();
        textSpace = mTextPaint.measureText("100");
        LinearGradient linearGradient = new LinearGradient(0, 0, getMeasuredWidth(), 0, new int[]{
                ResourcesUtil.getColor(R.color.green_5),
                ResourcesUtil.getColor(R.color.yellow_5),
                ResourcesUtil.getColor(R.color.red_5)
        }, new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP);

        rectF = new RectF(mStrokeWidth / 2 + textSpace,
                mStrokeWidth / 2 + textSpace,
                mWidth - mStrokeWidth / 2 - textSpace,
                mWidth - mStrokeWidth / 2 - textSpace);
        mArcPaint.setShader(linearGradient);
        mIndicatorPaint.setShader(linearGradient);
        canvas.drawArc(rectF, 0, -180, false, mArcPaint);

        canvas.drawText("0", 0 + textSpace / 2, mWidth / 2, mTextPaint);

        canvas.drawText("25", mWidth / 4 - mStrokeWidth - textSpace, mWidth / 4, mTextPaint);
        canvas.drawText("50", (mWidth / 2) - (textSpace / 3), textSpace - dp2px(getContext(), 2), mTextPaint);
        canvas.drawText("75", mWidth / 4 * 3 + textSpace + dp2px(getContext(), 1), mWidth / 4, mTextPaint);
        canvas.drawText("100", mWidth - textSpace - dp2px(getContext(), 2), mWidth / 2, mTextPaint);
        Path path = new Path();
        path.moveTo(mWidth / 2 - minCircleRadius * 3 / 2, mWidth / 2);
        path.lineTo(mWidth / 2, mStrokeWidth * 3 + textSpace);
        path.lineTo(mWidth / 2 + minCircleRadius * 3 / 2, mWidth / 2);
        canvas.rotate(mDegrees, mWidth / 2, mWidth / 2);

        canvas.save();
        canvas.drawCircle(mWidth / 2, mWidth / 2, minCircleRadius, mArcPaint);
        canvas.drawPath(path, mIndicatorPaint);
        canvas.restore();


    }

    public void setDegrees(float mDegrees) {
        this.mDegrees = 90 * ((mDegrees - 50) / 50);
        postInvalidate();
    }

    private float dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (dpValue * scale);
    }

}

