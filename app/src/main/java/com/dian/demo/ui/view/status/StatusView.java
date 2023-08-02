package com.dian.demo.ui.view.status;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dian.demo.utils.SmartRefreshUtil;

/**
 *
 */

public class StatusView extends FrameLayout {
    private final Context mContext;
    private LoadingLayout loadingView;
    private EmptyLayout emptyView;
    private ErrorLayout errorView;
    private SuccessLayout successView;

    public StatusView(@NonNull Context context) {
        this(context, null);
    }

    public StatusView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }


    public void init() {
        loadingView = new LoadingLayout(mContext);
        emptyView = new EmptyLayout(mContext);
        errorView = new ErrorLayout(mContext);
        successView = new SuccessLayout(mContext);
        addView(loadingView);
    }


    public void showErrorView() {
        removeAllViews();
        addView(errorView);
    }

    public void showLoadingView() {
        removeAllViews();
        addView(loadingView);
    }

    public void showEmptyView() {
        removeAllViews();
        addView(emptyView);
    }

    public void showSuccessView() {
        removeAllViews();
        addView(successView);
        SmartRefreshUtil.with(successView).setScrollMode();
    }


}
