package com.common.http.sse;

/**
 * 通用 SSE 回调，泛型 {@code T} 为每条事件数据反序列化后的类型。
 *
 * <p>当响应类型为 {@link String} 时，{@link #onEvent(Object, String)} 的 {@code data}
 * 即为原始字符串（不做反序列化）。
 *
 * <p>所有回调均运行在 OkHttp 的回调线程，如需更新 UI 请自行切换到主线程。
 */
public interface SseCallback<T> {

    /** 连接已建立。 */
    default void onOpen() {
    }

    /**
     * 收到一条事件数据。
     *
     * @param data    反序列化后的数据对象（解析失败时为 {@code null}）
     * @param rawData 原始事件数据字符串
     */
    void onEvent(T data, String rawData);

    /** 收到结束标记（{@link SseRequest#doneSignal()}），流正常结束。 */
    default void onDone() {
    }

    /** 连接已关闭。 */
    default void onClosed() {
    }

    /**
     * 发生错误。
     *
     * @param t        异常，可能为 {@code null}
     * @param response 服务端返回的响应体文本，可能为 {@code null}
     */
    void onFailure(Throwable t, String response);
}