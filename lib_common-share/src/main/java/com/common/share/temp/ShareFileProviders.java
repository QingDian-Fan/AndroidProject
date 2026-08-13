package com.common.share.temp;

import android.content.Context;

/**
 * FileProvider authority 的唯一生成入口。
 *
 * <p>必须保证三者完全一致：
 * <pre>
 *     QQ OpenSDK 初始化 authority
 *         == Manifest 中 ${applicationId}.provider 的合并结果
 *         == FileProvider.getUriForFile() 使用的 authority
 * </pre>
 *
 * <p>Manifest 里的 {@code ${applicationId}} 会在构建时按变体展开：debug 变体带
 * {@code applicationIdSuffix}（本项目为 {@code .debug}），release 不带。因此运行时
 * 只能由 {@link Context#getPackageName()} 推导，不得硬编码固定包名，否则 debug、渠道包
 * 或带后缀的变体会拿到错误 authority，QQ SDK 生成 URI 时抛
 * {@code IllegalArgumentException: Failed to find configured root}。
 */
public final class ShareFileProviders {

    /** 与 Manifest 中 {@code android:authorities="${applicationId}.provider"} 保持一致 */
    public static final String AUTHORITY_SUFFIX = ".provider";

    private ShareFileProviders() {
    }

    /**
     * 由包名（运行时 applicationId）生成 FileProvider authority。
     *
     * <p>纯函数，便于对 debug / release / applicationIdSuffix 变体做单元测试。
     *
     * @return authority；包名为空时返回 {@code null}，调用方必须据此中止分享
     */
    public static String authorityOf(String packageName) {
        if (packageName == null) {
            return null;
        }
        String trimmed = packageName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed + AUTHORITY_SUFFIX;
    }

    /**
     * 由 Context 生成 FileProvider authority。
     *
     * @return authority；Context 为空或包名不可用时返回 {@code null}
     */
    public static String authorityOf(Context context) {
        return context == null ? null : authorityOf(context.getPackageName());
    }

    /**
     * 校验 authority 是否确实由当前 applicationId 生成。
     *
     * <p>用于自检：一旦有人把 authority 写成常量或复制成其他变体的包名，这里会返回 false。
     */
    public static boolean matchesApplicationId(String authority, String packageName) {
        String expected = authorityOf(packageName);
        return expected != null && expected.equals(authority);
    }
}
