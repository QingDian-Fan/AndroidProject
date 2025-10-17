package com.dian.demo.ui.status;

import android.view.View;


import org.jetbrains.annotations.NotNull;


/**
 * demo to show how to create a {@link Gloading.Adapter}
 *
 * @author billy.qi
 * @see GlobalLoadingStatusView
 * @since 19/3/18 18:37
 */
public class GlobalAdapter implements Gloading.Adapter {

    @Override
    public View getView(Gloading.Holder holder, View convertView, int status, @NotNull String msg) {
        GlobalLoadingStatusView loadingStatusView = null;
        //reuse the old view, if possible
        if (convertView instanceof GlobalLoadingStatusView) {
            loadingStatusView = (GlobalLoadingStatusView) convertView;
        }
        if (loadingStatusView == null) {
            loadingStatusView = new GlobalLoadingStatusView(holder.getContext(), holder.getRetryTask());
        }
        loadingStatusView.setStatus(status,msg);
        Object data = holder.getData();
        //show or not show msg view
        boolean hideMsgView = StatusConstants.HIDE_LOADING_STATUS_MSG.equals(data);
        loadingStatusView.setMsgViewVisibility(!hideMsgView);
        return loadingStatusView;
    }

}
