package com.common.http.sse;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.common.http.HttpUtils;
import com.common.http.gson.GsonFactory;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * 通用 SSE（Server-Sent Events）请求客户端。
 *
 * <p>基于 OkHttp 的 {@link EventSource} 封装，通过泛型把每条事件数据自动反序列化为目标类型，
 * 与具体业务（如 DeepSeek）解耦。
 *
 * <pre>{@code
 * SseClient client = new SseClient();
 * EventSource source = client.newEventSource(request, MyResponse.class, new SseCallback<MyResponse>() {
 *     @Override public void onEvent(MyResponse data, String raw) { ... }
 *     @Override public void onDone() { ... }
 *     @Override public void onFailure(Throwable t, String response) { ... }
 * });
 * // 取消：source.cancel();
 * }</pre>
 */
public class SseClient {

    private static final String TAG = "SseClient";

    private final OkHttpClient okHttpClient;

    public SseClient() {
        this(HttpUtils.getInstance().getClient().getOkHttpClient());
    }

    public SseClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    /**
     * 发起 SSE 请求，每条事件数据反序列化为 {@code responseType}。
     *
     * @return {@link EventSource}，可调用 {@link EventSource#cancel()} 取消请求
     */
    public <T> EventSource newEventSource(@NonNull SseRequest request,
                                          @NonNull Class<T> responseType,
                                          @NonNull SseCallback<T> callback) {
        return newEventSource(request, (Type) responseType, callback);
    }

    /**
     * 发起 SSE 请求，支持泛型类型（配合 {@code TypeToken} 使用）。
     *
     * @param responseType 目标类型；传入 {@code String.class} 时不反序列化，直接回传原始字符串
     * @return {@link EventSource}，可调用 {@link EventSource#cancel()} 取消请求
     */
    public <T> EventSource newEventSource(@NonNull SseRequest request,
                                          @NonNull Type responseType,
                                          @NonNull SseCallback<T> callback) {
        Request okRequest = request.toOkHttpRequest();
        String doneSignal = request.doneSignal();
        EventSource.Factory factory = EventSources.createFactory(okHttpClient);
        return factory.newEventSource(okRequest, new EventSourceListener() {

            @Override
            public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
                callback.onOpen();
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onEvent(@NonNull EventSource eventSource,
                                @Nullable String id,
                                @Nullable String type,
                                @NonNull String data) {
                if (doneSignal != null && doneSignal.equals(data)) {
                    callback.onDone();
                    return;
                }
                T parsed;
                if (responseType == String.class) {
                    parsed = (T) data;
                } else {
                    parsed = parse(data, responseType);
                }
                callback.onEvent(parsed, data);
            }

            @Override
            public void onClosed(@NonNull EventSource eventSource) {
                callback.onClosed();
            }

            @Override
            public void onFailure(@NonNull EventSource eventSource,
                                  @Nullable Throwable t,
                                  @Nullable Response response) {
                // 主动取消 / 流被重置属于正常关闭，直接忽略
                if (isCancelled(t)) {
                    return;
                }
                callback.onFailure(t, readBody(response));
            }
        });
    }

    @Nullable
    private <T> T parse(String data, Type responseType) {
        try {
            return GsonFactory.getSingletonGson().fromJson(data, responseType);
        } catch (Exception e) {
            Log.e(TAG, "SSE 数据解析失败: " + data, e);
            return null;
        }
    }

    private static boolean isCancelled(@Nullable Throwable t) {
        if (t == null) {
            return false;
        }
        String msg = t.getMessage();
        return "Socket closed".equals(msg)
                || "Socket is closed".equals(msg)
                || "stream was reset: CANCEL".equals(msg);
    }

    @Nullable
    private static String readBody(@Nullable Response response) {
        if (response == null || response.body() == null) {
            return null;
        }
        try {
            return response.body().string();
        } catch (IOException e) {
            return null;
        }
    }
}