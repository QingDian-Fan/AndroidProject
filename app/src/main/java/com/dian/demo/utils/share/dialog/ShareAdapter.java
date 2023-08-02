package com.dian.demo.utils.share.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import com.dian.demo.R;

import java.util.List;

public class ShareAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public Context mContext;
    public List<PlatformData> platformList;

    public ShareAdapter(Context mContext, List<PlatformData> platformList) {
        this.mContext = mContext;
        this.platformList = platformList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mView = LayoutInflater.from(mContext).inflate(R.layout.item_share_view, parent, false);
        return new ItemViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        ItemViewHolder holder = (ItemViewHolder) viewHolder;
        holder.mIvShareImage.setImageDrawable(platformList.get(position).bitmap);
        holder.mTvShareText.setText(platformList.get(position).name);
        holder.itemView.setOnClickListener(view -> {
            if (itemOnClickListener != null) {
                itemOnClickListener.CallBack(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return platformList == null ? 0 : platformList.size();
    }

    ItemOnClickListener itemOnClickListener;

    public interface ItemOnClickListener {
        void CallBack(int position);
    }

    public void setItemOnClickListener(ItemOnClickListener itemOnClickListener) {
        this.itemOnClickListener = itemOnClickListener;
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private AppCompatImageView mIvShareImage;
        private AppCompatTextView mTvShareText;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mIvShareImage = itemView.findViewById(R.id.iv_share_image);
            mTvShareText = itemView.findViewById(R.id.tv_share_text);
        }
    }
}
