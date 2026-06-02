package com.common.weight;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.common.utils.ResourcesUtil;


public class TrackTextView extends AppCompatTextView {
    private int mDefaultColor;
    private int mChangeColor;

    private Paint mDefaultPaint;
    private Paint mChangePaint;

    private float mCurrentProgress = 0.3f;

    public TrackTextView(@NonNull Context context) {
        this(context, null);
    }

    public TrackTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrackTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TrackTextView);
        mDefaultColor = ta.getColor(R.styleable.TrackTextView_default_color, Color.parseColor("#8c8c8c"));
        mChangeColor = ta.getColor(R.styleable.TrackTextView_change_color, Color.parseColor("#FF0000"));
        ta.recycle();
        init();
    }

    private void init() {
        mDefaultPaint = new Paint();
        mDefaultPaint.setColor(mDefaultColor);
        mDefaultPaint.setAntiAlias(true);
        mDefaultPaint.setTextSize(getTextSize());
        mDefaultPaint.setDither(true);

        mChangePaint = new Paint();
        mChangePaint.setColor(mChangeColor);
        mChangePaint.setAntiAlias(true);
        mChangePaint.setTextSize(getTextSize());
        mChangePaint.setDither(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int end = (int) ((getWidth() - getPaddingRight() - getPaddingLeft()) * mCurrentProgress + getPaddingLeft());
        drawText(canvas, getPaddingLeft(), end, mDefaultPaint);
        drawText(canvas, end, getWidth() - getPaddingRight(), mChangePaint);

    }

    public void drawText(Canvas canvas, int start, int end, Paint mPaint) {
        canvas.save();
        String contentString = getText().toString();
        Rect rect = new Rect(start, 0, end, getHeight());
        canvas.clipRect(rect);
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        int centerY = getHeight() / 2;
        float baseline = centerY + (fm.bottom - fm.top) / 2 - fm.bottom;
        canvas.drawText(contentString, getPaddingLeft(), baseline, mPaint);
        canvas.restore();
    }

    public void setCurrentProgress(float mCurrentProgress) {
        this.mCurrentProgress = mCurrentProgress;
        invalidate();
    }
}

