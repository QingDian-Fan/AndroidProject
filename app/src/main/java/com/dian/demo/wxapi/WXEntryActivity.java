package com.dian.demo.wxapi;

import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.dian.demo.ProjectApplication;
import com.dian.demo.R;
import com.dian.demo.config.AppConfig;
import com.dian.demo.utils.CacheUtil;
import com.dian.demo.utils.ResourcesUtil;
import com.dian.demo.utils.ToastUtil;
import com.dian.demo.utils.share.ShareFactory;
import com.dian.demo.utils.share.channel.Channel;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;
import java.util.HashMap;

public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {

    public int WX_LOGIN = 1; //微信登录为getType为1，分享为0
    private IWXAPI iwxapi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        iwxapi = WXAPIFactory.createWXAPI(this, AppConfig.WX_APPID);
        iwxapi.handleIntent(getIntent(), this);
    }

    @Override
    public void onReq(BaseReq baseReq) {

    }

    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp.getType() == WX_LOGIN) {
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
            CacheUtil.INSTANCE.deleteDir(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "share"));
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
        map.put("appid", AppConfig.WX_APPID);
        map.put("secret", AppConfig.WX_APPKEY);
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
