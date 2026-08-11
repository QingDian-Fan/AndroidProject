package com.common.share.temp;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 一次分享产生的临时文件会话。
 *
 * <p>会话是清理的唯一依据：只有登记在本对象里的文件才允许被自动删除。
 * 用户保存到相册的图片、调用方传入的原始文件不会被登记，因此永远不在自动清理范围内。
 *
 * <p>本类可能被分享线程、SDK 回调线程和后台清理线程同时访问，所有可变状态均加锁保护。
 */
public final class ShareTempSession {

    private final String sessionId;
    private final int channel;
    private final File directory;
    private final long createdAtMillis;
    private final long expiresAtMillis;

    /** 本次分享创建的全部临时文件（原图、压缩图、缩略图都要登记） */
    private final List<File> files = new ArrayList<>();

    /** 本次对外授权的 content URI，删除文件后据此撤销临时授权 */
    private final Set<String> uris = new LinkedHashSet<>();

    private ShareSessionState state = ShareSessionState.CREATING;

    /** 最早允许删除的时间；系统分享面板等无可靠结果回调的渠道据此预留安全读取期 */
    private long earliestDeleteAtMillis;

    ShareTempSession(String sessionId, int channel, File directory,
                     long createdAtMillis, long expiresAtMillis) {
        this.sessionId = sessionId;
        this.channel = channel;
        this.directory = directory;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.earliestDeleteAtMillis = createdAtMillis;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getChannel() {
        return channel;
    }

    public File getDirectory() {
        return directory;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public synchronized ShareSessionState getState() {
        return state;
    }

    synchronized void setState(ShareSessionState newState) {
        if (newState == null || state.isDeleted()) {
            return;
        }
        state = newState;
    }

    public synchronized long getEarliestDeleteAtMillis() {
        return earliestDeleteAtMillis;
    }

    /** 只允许把最早删除时间往后推，避免并发回调把安全读取期缩短 */
    synchronized void extendEarliestDeleteAt(long millis) {
        if (millis > earliestDeleteAtMillis) {
            earliestDeleteAtMillis = millis;
        }
    }

    synchronized void addFile(File file) {
        if (file != null && !files.contains(file)) {
            files.add(file);
        }
    }

    /** 返回快照，避免调用方在删除过程中遍历到被并发修改的集合 */
    public synchronized List<File> getFiles() {
        return new ArrayList<>(files);
    }

    synchronized void addUri(String uri) {
        if (uri != null && uri.length() > 0) {
            uris.add(uri);
        }
    }

    public synchronized List<String> getUris() {
        return new ArrayList<>(uris);
    }

    /** 是否已过期：超过过期时间且尚未删除完成 */
    public synchronized boolean isExpired(long nowMillis) {
        return !state.isDeleted() && nowMillis >= expiresAtMillis;
    }

    /** 是否已到达最早安全删除时间 */
    public synchronized boolean isSafeToDeleteAt(long nowMillis) {
        return nowMillis >= earliestDeleteAtMillis;
    }
}
