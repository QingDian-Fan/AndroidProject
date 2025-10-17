package com.dian.demo.ui.status;



import static com.dian.demo.ui.status.Gloading.STATUS_EMPTY_DATA;
import static com.dian.demo.ui.status.Gloading.STATUS_LOADING;
import static com.dian.demo.ui.status.Gloading.STATUS_LOAD_FAILED;
import static com.dian.demo.ui.status.Gloading.STATUS_LOAD_SUCCESS;
import static com.dian.demo.ui.status.Gloading.STATUS_TASK_OVER;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;


import com.dian.demo.ProjectApplication;
import com.dian.demo.R;
import com.dian.demo.utils.LogUtil;
import com.dian.demo.utils.NetWorkUtil;

import org.jetbrains.annotations.NotNull;


/**
 * simple loading status view for global usage
 *
 * @author dzzch
 */
@SuppressLint("ViewConstructor")
public class GlobalLoadingStatusView extends LinearLayout implements View.OnClickListener {

    private final Runnable mRetryTask;

    private final TextView mTextView;
    private final ImageView mImageView;
    private final ConstraintLayout lytLoading;


    public GlobalLoadingStatusView(Context context, Runnable retryTask) {
        super(context);

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        LayoutInflater.from(context).inflate(R.layout.view_global_loading_status, this, true);

        mImageView = findViewById(R.id.image);
        mTextView = findViewById(R.id.text);
        lytLoading = findViewById(R.id.lyt_loading);

        this.mRetryTask = retryTask;
        setBackgroundColor(0xFFF0F0F0);

    }

    public void setMsgViewVisibility(boolean visible) {
        mTextView.setVisibility(visible ? VISIBLE : GONE);
    }

    @SuppressLint("SetTextI18n")
    public void setStatus(int status, @NotNull String msg) {
        boolean show = true;
        OnClickListener onClickListener = null;
        int image = R.mipmap.layout_status_loading;
        String str = ProjectApplication.getAppInstance().getString(R.string.str_none);
        mImageView.setVisibility(VISIBLE);
        mTextView.setVisibility(VISIBLE);
        lytLoading.setVisibility(GONE);
        mImageView.clearAnimation();
        switch (status) {
            case STATUS_LOAD_SUCCESS:
                LogUtil.d("setStatue STATUS_LOAD_SUCCESS");
                show = false;
                break;
            case STATUS_LOADING:
                mTextView.setText("");
                mImageView.setVisibility(GONE);
                mTextView.setVisibility(GONE);
                lytLoading.setVisibility(VISIBLE);
                break;
            case STATUS_LOAD_FAILED:
                LogUtil.d("setStatue STATUS_LOAD_FAILED");
                str = ProjectApplication.getAppInstance().getString(R.string.load_failed);
                image = R.mipmap.layout_status_no_network;
                boolean networkConn = NetWorkUtil.isNetworkAvailable();
                mTextView.setText(str + msg);
                if (!networkConn) {
                    String temp1 = ProjectApplication.getAppInstance().getString(R.string.load_failed_no_network1);
                    String temp2 = ProjectApplication.getAppInstance().getString(R.string.load_failed_no_network2);
                    SpannableStringBuilder builder = new SpannableStringBuilder(temp1 + temp2);
                    ForegroundColorSpan blueSpan = new ForegroundColorSpan(Color.parseColor("#2C48F3"));
                    builder.setSpan(blueSpan, temp1.length(), temp1.length() + temp2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    image = R.mipmap.layout_status_no_network;
                    mTextView.setText(builder);
                }
                onClickListener = this;
                break;
            case STATUS_EMPTY_DATA:
                LogUtil.d("setStatue STATUS_EMPTY_DATA");
                str = ProjectApplication.getAppInstance().getString(R.string.empty);
                image = R.mipmap.layout_status_empty;
                if (msg.isEmpty()) {
                    mTextView.setText(str);
                } else {
                    mTextView.setText(msg);
                }
                break;
            case STATUS_TASK_OVER:
                LogUtil.d("setStatue STATUS_TASK_OVER");
                str = ProjectApplication.getAppInstance().getString(R.string.complete_all);
                image = R.mipmap.layout_state_complete_all;
                mTextView.setText(str);
                break;
            default:
                break;
        }
        mImageView.setImageResource(image);
        setOnClickListener(onClickListener);
        LogUtil.d(String.format("setVisibility %s", show));
        setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (mRetryTask != null) {
            mRetryTask.run();
        }
    }
}