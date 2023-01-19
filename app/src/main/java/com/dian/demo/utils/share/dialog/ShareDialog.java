package com.dian.demo.utils.share.dialog;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dian.demo.ProjectApplication;
import com.dian.demo.R;
import com.dian.demo.utils.IntentUtils;
import com.dian.demo.utils.ResourcesUtils;
import com.dian.demo.utils.ToastUtils;
import com.dian.demo.utils.share.ShareFactory;
import com.dian.demo.utils.share.ShareModel;
import com.dian.demo.utils.share.ShareUtils;
import com.dian.demo.utils.share.channel.Channel;
import com.dian.demo.utils.share.channel.CustomChannel;

import java.util.ArrayList;
import java.util.List;


public class ShareDialog extends DialogFragment implements ShareAdapter.ItemOnClickListener {
    private RecyclerView mRvSharePlatform;
    private AppCompatTextView mTvShareCancel;

    private ShareModel shareModel;
    private boolean isShow = true, isHeaderShow = false;
    private String title, content;

    public int BITMAP_TYPE = 0;//0:不是图片,1:bitmap,2:图片路径

    private List<PlatformData> platformList = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        View mView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_share_layout, null, false);
        dialog.setContentView(mView);
        initView(mView);
        initPlatformData();
        initData();
        return dialog;
    }


    private void initView(View mView) {
        NestedScrollView mSvShare = mView.findViewById(R.id.sv_share);
        AppCompatImageView mIvShare = mView.findViewById(R.id.iv_share);
        AppCompatTextView mTvShareTitle = mView.findViewById(R.id.tv_share_title);
        AppCompatTextView mTvShareContent = mView.findViewById(R.id.tv_share_content);
        mRvSharePlatform = mView.findViewById(R.id.rv_share_platform);
        mTvShareCancel = mView.findViewById(R.id.tv_share_cancel);

        mTvShareCancel.setVisibility(isShow ? View.VISIBLE : View.GONE);
        mTvShareTitle.setVisibility(isHeaderShow ? View.VISIBLE : View.GONE);
        mTvShareContent.setVisibility(isHeaderShow ? View.VISIBLE : View.GONE);

        mTvShareTitle.setText(isHeaderShow ? (title != null ? title : "") : "");
        mTvShareContent.setText(isHeaderShow ? (content != null ? content : "") : "");

        mSvShare.setVisibility(BITMAP_TYPE == 0 ? View.GONE : View.VISIBLE);
        mIvShare.setVisibility(BITMAP_TYPE == 0 ? View.GONE : View.VISIBLE);
        if (BITMAP_TYPE == 1) {
            mIvShare.setImageBitmap(shareModel.bitmap);
        } else if (BITMAP_TYPE == 2) {
            Glide.with(requireContext()).load(shareModel.imgLink).into(mIvShare);
        }
    }

    private void initPlatformData() {
        if (IntentUtils.isInstalled(requireContext(), Channel.PACKAGE_QQ)) {
            platformList.add(new PlatformData(1, ResourcesUtils.getString(R.string.share_qq), Channel.PACKAGE_QQ, ResourcesUtils.getDrawable(R.drawable.icon_share_qq), Channel.QQ));
            platformList.add(new PlatformData(2, ResourcesUtils.getString(R.string.share_qzone), Channel.PACKAGE_QQ, ResourcesUtils.getDrawable(R.drawable.icon_share_qzone), Channel.QQ_ZONE));
        }
        if (IntentUtils.isInstalled(requireContext(), Channel.PACKAGE_WECHAT)) {
            platformList.add(new PlatformData(3, ResourcesUtils.getString(R.string.share_wechat), Channel.PACKAGE_WECHAT, ResourcesUtils.getDrawable(R.drawable.icon_share_wechat), Channel.WECHAT));
            platformList.add(new PlatformData(4, ResourcesUtils.getString(R.string.share_moment), Channel.PACKAGE_WECHAT, ResourcesUtils.getDrawable(R.drawable.icon_share_moment), Channel.WECHAT_TIMELINE));
        }
        if (IntentUtils.isInstalled(requireContext(), Channel.PACKAGE_WEIBO)) {
            platformList.add(new PlatformData(7, ResourcesUtils.getString(R.string.share_weibo), Channel.PACKAGE_WEIBO, ResourcesUtils.getDrawable(R.drawable.icon_share_weibo), Channel.WEIBO));
        }
        if (shareModel.type == ShareModel.TYPE_HTML) {
            platformList.add(new PlatformData(5, ResourcesUtils.getString(R.string.share_link), Channel.PACKAGE_THIS, ResourcesUtils.getDrawable(R.drawable.icon_share_link), Channel.COPY_LINK));
        } else if (shareModel.type == ShareModel.TYPE_BITMAP) {
            platformList.add(new PlatformData(6, ResourcesUtils.getString(R.string.share_save_img), Channel.PACKAGE_THIS, ResourcesUtils.getDrawable(R.drawable.icon_share_save_img), Channel.SAVE_LOCAL));
        }
        initShareAdapter();
    }

    private void initData() {
        mTvShareCancel.setOnClickListener(view -> {
            dismissAllowingStateLoss();
        });
    }

    public void setText(boolean isShow, String text) {
        this.shareModel = ShareModel.shareText(text);
        this.isShow = isShow;
        BITMAP_TYPE = 0;
    }

    public void setLinkData(boolean isShow, String shareLink, String imageLink, String linkTitle, String linkContent) {
        this.shareModel = ShareModel.shareUrl(linkTitle, linkContent, shareLink, imageLink);
        this.isShow = isShow;
        BITMAP_TYPE = 0;
    }

    public void setLinkData(boolean isShow, String shareLink, String imageLink, String linkTitle, String linkContent, String title, String content) {
        this.shareModel = ShareModel.shareUrl(linkTitle, linkContent, shareLink, imageLink);
        this.isShow = isShow;
        isHeaderShow = true;
        this.title = title;
        this.content = content;
        BITMAP_TYPE = 0;
    }

    public void setLinkData(boolean isShow, Bitmap bitmap, String shareLink, String linkTitle, String linkContent) {
        this.shareModel = ShareModel.shareUrl(linkTitle, linkContent, shareLink, bitmap);
        this.isShow = isShow;
        BITMAP_TYPE = 0;
    }

    public void setLinkData(boolean isShow, Bitmap bitmap, String shareLink, String linkTitle, String linkContent, String title, String content) {
        this.shareModel = ShareModel.shareUrl(linkTitle, linkContent, shareLink, bitmap);
        this.isShow = isShow;
        isHeaderShow = true;
        this.title = title;
        this.content = content;
        BITMAP_TYPE = 0;
    }


    public void setBitmapData(Bitmap mBitmap) {
        this.shareModel = ShareModel.shareBitmap(mBitmap);
        BITMAP_TYPE = 1;
    }

    public void setBitmapData(Bitmap mBitmap, String title, String content) {
        this.shareModel = ShareModel.shareBitmap(mBitmap);
        isHeaderShow = true;
        this.title = title;
        this.content = content;
        BITMAP_TYPE = 1;
    }

    public void setBitmapData(String imageUrl) {
        this.shareModel = ShareModel.shareBitmap(imageUrl);
        BITMAP_TYPE = 2;
    }

    public void setBitmapData(String imageUrl, String title, String content) {
        this.shareModel = ShareModel.shareBitmap(imageUrl);
        isHeaderShow = true;
        this.title = title;
        this.content = content;
        BITMAP_TYPE = 2;
    }

    private void initShareAdapter() {
        ShareAdapter mAdapter = new ShareAdapter(getContext(), platformList);
        mRvSharePlatform.setLayoutManager(new GridLayoutManager(getContext(), platformList.size()));
        mRvSharePlatform.setAdapter(mAdapter);
        mAdapter.setItemOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().setCanceledOnTouchOutside(false);
            if (getDialog().getWindow() != null) {
                getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.MATCH_PARENT;
                getDialog().getWindow().setAttributes(params);
            }
        }
    }

    @Override
    public void CallBack(int position) {
        PlatformData platformData = platformList.get(position);
        if (platformData == null) return;
        dismissAllowingStateLoss();
        if (platformData.shareChannel == Channel.COPY_LINK) {
            getContext().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("url", shareModel.link));
            ToastUtils.showToast(ProjectApplication.getAppContext(), ResourcesUtils.getString(R.string.link_is_copy), false, Gravity.CENTER);
            return;
        } else if (platformData.shareChannel == Channel.SAVE_LOCAL) {
            if (shareModel.bitmap != null) {
                ShareUtils.saveBmp2Uri(getContext(), shareModel.bitmap, "share" + System.currentTimeMillis());
            } else {
                Glide.with(this).asBitmap().load(shareModel.imgLink).into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        ShareUtils.saveBmp2Uri(getContext(), bitmap, "share" + System.currentTimeMillis());
                    }
                });
            }
            ToastUtils.showToast(ProjectApplication.getAppContext(), ResourcesUtils.getString(R.string.img_is_save), false, Gravity.CENTER);
            return;
        }
        CustomChannel customChannel = ShareFactory.newChannel(getActivity(), platformData.shareChannel);
        switch (shareModel.type) {
            case ShareModel.TYPE_TEXT:
                customChannel.shareText(shareModel.des);
                break;
            case ShareModel.TYPE_BITMAP:
                if (shareModel.bitmap != null) {
                    customChannel.shareBitmap(shareModel.bitmap);
                } else {
                    Glide.with(this).asBitmap().load(shareModel.imgLink).into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                            customChannel.shareBitmap(bitmap);
                        }
                    });
                }
                break;
            case ShareModel.TYPE_HTML:
                if (shareModel.bitmap != null) {
                    customChannel.shareLink(shareModel.title, shareModel.des, shareModel.link, shareModel.bitmap);
                } else if (!TextUtils.isEmpty(shareModel.imgLink)) {
                    Glide.with(requireContext()).asBitmap().load(Uri.parse(shareModel.imgLink)).into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                            customChannel.shareLink(shareModel.title, shareModel.des, shareModel.link, bitmap);
                        }
                    });
                } else {
                    customChannel.shareLink(shareModel.title, shareModel.des, shareModel.link, null);
                }
                break;
        }
    }
}
