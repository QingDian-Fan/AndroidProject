package com.common.share.temp;

/**
 * 分享临时文件会话状态。
 *
 * <p>{@link #DELETED} 之后不再接受任何状态变更，保证成功、取消、失败、超时
 * 和生命周期回调重复到达时只执行一次有效删除。
 */
public enum ShareSessionState {

    /** 会话目录已创建，临时文件尚未写完 */
    CREATING,

    /** 临时文件已写入并登记完成，可以交给目标应用 */
    READY,

    /** 已调起目标应用或系统分享面板 */
    LAUNCHED,

    /** 已调起并等待 SDK 结果回调 */
    WAITING_RESULT,

    /** 已进入安全延迟期，到达最早删除时间后清理 */
    PENDING_SAFE_DELETE,

    /** 本次会话的临时文件已全部删除 */
    DELETED,

    /** 删除失败，等待后续过期扫描重试 */
    DELETE_FAILED;

    /** 是否为不再需要保留文件的终态（{@link #DELETE_FAILED} 仍需重试，故不算终态） */
    public boolean isDeleted() {
        return this == DELETED;
    }
}
