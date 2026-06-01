package com.common.weight;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class FlowLayout extends ViewGroup {

    public static final int GRAVITY_TOP = 0;
    public static final int GRAVITY_CENTER = 1;
    public static final int GRAVITY_BOTTOM = 2;

    private int itemSpacing = 0;     // 子项左右间距
    private int lineSpacing = 0;     // 行间距
    private int maxLines = Integer.MAX_VALUE; // 最大行数
    private int gravity = GRAVITY_TOP; // 行内对齐方式

    private List<List<View>> lines = new ArrayList<>();
    private List<Integer> lineHeights = new ArrayList<>();

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /********************** 公开 API：不影响你原来的调用 ************************/

    public void setItemSpacing(int itemSpacing) {
        this.itemSpacing = itemSpacing;
        requestLayout();
    }

    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
        requestLayout();
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
        requestLayout();
    }

    public void setGravity(int gravity) {
        this.gravity = gravity;
        requestLayout();
    }

    /********************** Measure ************************/

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        lines.clear();
        lineHeights.clear();

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthLimit = widthSize - getPaddingLeft() - getPaddingRight();

        int currentLineWidth = 0;
        int currentLineHeight = 0;
        List<View> currentLine = new ArrayList<>();

        int totalHeight = getPaddingTop() + getPaddingBottom();
        int childCount = getChildCount();
        int lineCount = 1;

        for (int i = 0; i < childCount; i++) {

            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            // 是否需要换行
            if (!currentLine.isEmpty()
                    && currentLineWidth + childWidth + itemSpacing > widthLimit) {

                lines.add(currentLine);
                lineHeights.add(currentLineHeight);

                totalHeight += currentLineHeight + lineSpacing;

                lineCount++;
                if (lineCount > maxLines) break;

                currentLine = new ArrayList<>();
                currentLineWidth = 0;
                currentLineHeight = 0;
            }

            currentLine.add(child);
            currentLineWidth += (currentLineWidth == 0 ? childWidth : childWidth + itemSpacing);
            currentLineHeight = Math.max(currentLineHeight, childHeight);
        }

        // 最后一行
        if (!currentLine.isEmpty() && lineCount <= maxLines) {
            lines.add(currentLine);
            lineHeights.add(currentLineHeight);
            totalHeight += currentLineHeight;
        }

        setMeasuredDimension(
                widthSize,
                resolveSize(totalHeight, heightMeasureSpec)
        );
    }

    /********************** Layout ************************/

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        int curLeft = getPaddingLeft();
        int curTop = getPaddingTop();

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {

            List<View> line = lines.get(lineIndex);
            int lineHeight = lineHeights.get(lineIndex);

            curLeft = getPaddingLeft();

            for (View child : line) {

                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();

                // 行内对齐
                int childTop;
                switch (gravity) {
                    case GRAVITY_CENTER:
                        childTop = curTop + (lineHeight - childHeight) / 2 + lp.topMargin - lp.bottomMargin;
                        break;
                    case GRAVITY_BOTTOM:
                        childTop = curTop + lineHeight - childHeight - lp.bottomMargin;
                        break;
                    case GRAVITY_TOP:
                    default:
                        childTop = curTop + lp.topMargin;
                        break;
                }

                int childLeft = curLeft + lp.leftMargin;

                child.layout(
                        childLeft,
                        childTop,
                        childLeft + childWidth,
                        childTop + childHeight
                );

                curLeft += childWidth + lp.leftMargin + lp.rightMargin + itemSpacing;
            }

            curTop += lineHeight + lineSpacing;
        }
    }

    /********************** LayoutParams 支持 Margin ************************/

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    /********************** 保持你现有 Adapter 模式 ************************/

    private FlowLayoutAdapter mAdapter;
    private DataSetObserver mObserver;

    public void setAdapter(FlowLayoutAdapter adapter) {
        if (mAdapter != null && mObserver != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }

        if (adapter == null) throw new NullPointerException("adapter cannot be null");

        mAdapter = adapter;
        mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                resetLayout();
            }
        };
        mAdapter.registerDataSetObserver(mObserver);
        resetLayout();
    }

    private void resetLayout() {
        removeAllViews();
        mAdapter.addViewToList(this);
        ArrayList<View> views = mAdapter.getViewList();
        for (View v : views) {
            addView(v);
        }
    }
}
