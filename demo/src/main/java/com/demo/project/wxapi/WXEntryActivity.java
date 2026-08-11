package com.demo.project.wxapi;

import static com.common.share.ShareConfig.WX_APPID;
import static com.common.share.ShareConfig.WX_APPKEY;

import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;


import com.common.share.ShareFactory;
import com.common.share.channel.Channel;
import com.common.share.temp.ShareTempFiles;
import com.common.utils.ResourcesUtil;
import com.common.utils.ToastUtil;
import com.demo.project.ProjectApplication;
import com.demo.project.R;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.HashMap;

public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {

    public int WX_LOGIN = 1; //微信登录为getType为1，分享为0
    private IWXAPI iwxapi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        iwxapi = WXAPIFactory.createWXAPI(this, WX_APPID);
        iwxapi.handleIntent(getIntent(), this);
    }

    @Override
    public void onReq(BaseReq baseReq) {

    }

    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp.getType() == WX_LOGIN) {
            // 登录回调不涉及分享临时文件，不得触发任何清理
            SendAuth.Resp resp = (SendAuth.Resp) baseResp;
            switch (resp.errCode) {
                case BaseResp.ErrCode.ERR_OK:
                    String code = String.valueOf(resp.code);
                    getAccessToken(code);//获取用户信息
                    break;
                case BaseResp.ErrCode.ERR_AUTH_DENIED://用户拒绝授权
                    break;
                case BaseResp.ErrCode.ERR_USER_CANCEL://用户取消
                    break;
                default:
                    break;
            }
        } else {
            // 分享结果已明确（成功 / 取消 / 拒绝 / 其他错误），微信不会再读取文件，
            // 按本次会话精准清理，而不是删除外部存储根目录下的整个 /share 目录。
            // 清理失败不会改变下面的分享结果提示。
            ShareTempFiles.finishPendingSession(Channel.WECHAT);
            switch (baseResp.errCode) {
                case BaseResp.ErrCode.ERR_OK://分享成功
                    ShareFactory.sendSuccessAction(Channel.WECHAT);
                    break;
                case BaseResp.ErrCode.ERR_USER_CANCEL:
                    ToastUtil.showToast(ProjectApplication.getAppContext(), ResourcesUtil.getString(R.string.share_cancel), false, Gravity.CENTER);
                    break;
                case BaseResp.ErrCode.ERR_AUTH_DENIED:
                    ToastUtil.showToast(ProjectApplication.getAppContext(), ResourcesUtil.getString(R.string.share_denied), false, Gravity.CENTER);
                    break;
            }
        }
        finish();
    }

    public void getAccessToken(String code) {
        HashMap<String, String> map = new HashMap<>();
        map.put("appid", WX_APPID);
        map.put("secret", WX_APPKEY);
        map.put("code", code);
        map.put("grant_type", "authorization_code");
        //getUserInfo()
    }

    public void getUserInfo(String token, String openId) {
        HashMap<String, String> map = new HashMap<>();
        map.put("access_token", token);
        map.put("openid", openId);

    }
}
