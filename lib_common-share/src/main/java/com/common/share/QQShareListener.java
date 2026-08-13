package com.common.share;



import com.common.share.channel.Channel;
import com.common.share.temp.ShareTempFileStore;
import com.common.share.temp.ShareTempFiles;
import com.common.utils.LogUtil;
import com.common.utils.Utils;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.UiError;

/**
 * QQ / QQ 空间分享结果回调。
 *
 * <p>成功、取消、失败、警告四条路径都会结束本次临时文件会话：
 * 前三者说明 QQ 已经读取完毕或不会再读取，可以立即删除本次生成的图片；
 * {@code onWarning} 不能证明 QQ 已经读完，改走安全延迟清理。
 *
 * <p>清理是幂等的，与 {@link ShareActivity} 中的回调重复到达也只会真正删除一次，
 * 且清理结果不会改变分享成功 / 取消 / 失败的业务语义。
 */
public class QQShareListener implements IUiListener {

    private static final String TAG = "QQ-Share--->";

    @Override
    public void onComplete(Object o) {
        logResult("onComplete");
        finishShareSession();
        ShareFactory.sendSuccessAction(Channel.QQ);
    }

    @Override
    public void onError(UiError uiError) {
        // errorMessage 由 SDK 生成，不含账号与鉴权数据
        LogUtil.e(TAG, "onError:" + (uiError == null ? "unknown" : uiError.errorMessage));
        finishShareSession();
    }

    @Override
    public void onCancel() {
        LogUtil.e(TAG,"onCancel:cancel");
        finishShareSession();
    }

    @Override
    public void onWarning(int i) {
        LogUtil.e(TAG,"onWarning:"+i);
        // 警告不代表 QQ 一定已经读完文件（可能仍在走降级分享），进入安全延迟清理
        ShareTempFiles.finishPendingSessionAfterSafeDelay(
                Channel.QQ, ShareTempFileStore.DEFAULT_SAFE_DELETE_DELAY_MILLIS);
        logResult("onWarning-safeDelay");
    }

    private void finishShareSession() {
        ShareTempFiles.finishPendingSession(Channel.QQ);
    }

    /** Debug 下记录回调与清理状态；不输出路径、URI、AppId 和账号信息 */
    private void logResult(String state) {
        if (Utils.INSTANCE.isDebug()) {
            LogUtil.d(TAG, "result=" + state + ", cleanupScheduled=true");
        }
    }
}
