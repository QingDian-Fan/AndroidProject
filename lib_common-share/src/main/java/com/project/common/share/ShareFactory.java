package com.project.common.share;


import android.app.Activity;
import android.content.Intent;

import com.dian.demo.ProjectApplication;
import com.project.common.share.channel.Channel;
import com.project.common.share.channel.CustomChannel;
import com.project.common.share.channel.QQChannel;
import com.project.common.share.channel.WeChatChannel;
import com.project.common.share.channel.WeiBoChannel;


public class ShareFactory {

    public static final String SUCCESS_CHANNEL = "channel";
    public static final String SHARE_SUCCESS_ACTION = "wechat_share";

    public static void sendSuccessAction(int channel) {
        Intent intent = new Intent(SHARE_SUCCESS_ACTION);
        intent.putExtra(SUCCESS_CHANNEL, channel);
        intent.setPackage(ProjectApplication.getAppContext().getPackageName());
        ProjectApplication.getAppContext().sendBroadcast(intent);
    }

    public static CustomChannel newChannel(Activity context, int type) {

        CustomChannel customChannel = new CustomChannel("", context);

        switch (type) {
            case Channel.WECHAT:
                customChannel = new WeChatChannel(false, context);
                break;
            case Channel.WECHAT_TIMELINE:
                customChannel = new WeChatChannel(true, context);
                break;
            case Channel.QQ:
                customChannel = new QQChannel(false, context);
                break;
            case Channel.QQ_ZONE:
                customChannel = new QQChannel(true, context);
                break;
            case Channel.WEIBO:
                customChannel = new WeiBoChannel(context);
                break;

        }
        return customChannel;
    }
}
