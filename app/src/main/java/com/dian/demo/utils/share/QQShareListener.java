package com.dian.demo.utils.share;


import com.dian.demo.utils.LogUtil;
import com.dian.demo.utils.share.channel.Channel;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.UiError;

public class QQShareListener implements IUiListener {
    @Override
    public void onComplete(Object o) {
        ShareFactory.sendSuccessAction(Channel.QQ);
    }

    @Override
    public void onError(UiError uiError) {
        LogUtil.e("QQ-Share--->","onError:"+uiError.errorMessage);
    }

    @Override
    public void onCancel() {
        LogUtil.e("QQ-Share--->","onCancel:cancel");
    }

    @Override
    public void onWarning(int i) {
        LogUtil.e("QQ-Share--->","onWarning:"+i);
    }
}