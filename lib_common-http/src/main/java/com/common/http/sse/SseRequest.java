package com.common.http.sse;


import com.common.http.gson.GsonFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 通用 SSE 请求配置。
 *
 * <p>使用 {@link Builder} 构建，支持自定义 URL、请求方法、请求头、JSON 请求体以及
 * 结束标记（如 OpenAI / DeepSeek 风格的 "[DONE]"）。
 *
 * <pre>{@code
 * SseRequest request = new SseRequest.Builder()
 *         .url("https://api.deepseek.com/chat/completions")
 *         .bearer("sk-xxx")
 *         .post(sendData)          // 任意对象，内部用 Gson 序列化
 *         .build();
 * }</pre>
 */
public class SseRequest {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final String bodyJson;
    private final String doneSignal;

    private SseRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.bodyJson = builder.bodyJson;
        this.doneSignal = builder.doneSignal;
    }

    /** 收到该数据时表示流结束，触发 {@link SseCallback#onDone()}，默认 "[DONE]"。 */
    public String doneSignal() {
        return doneSignal;
    }

    /** 转换为 OkHttp 请求对象。 */
    public Request toOkHttpRequest() {
        Request.Builder requestBuilder = new Request.Builder().url(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        RequestBody body = bodyJson == null ? null : RequestBody.create(bodyJson, JSON);
        return requestBuilder.method(method, body).build();
    }

    public static class Builder {
        private String url;
        private String method = "POST";
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String bodyJson;
        private String doneSignal = "[DONE]";

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        /** 便捷设置 Bearer Token 鉴权头。 */
        public Builder bearer(String token) {
            return header("Authorization", "Bearer " + token);
        }

        /** 使用 GET 方法（无请求体）。 */
        public Builder get() {
            this.method = "GET";
            this.bodyJson = null;
            return this;
        }

        /** 使用 POST 方法，body 对象将通过 Gson 序列化为 JSON。 */
        public Builder post(Object body) {
            return postJson(GsonFactory.getSingletonGson().toJson(body));
        }

        /** 使用 POST 方法，直接传入已序列化的 JSON 字符串。 */
        public Builder postJson(String json) {
            this.method = "POST";
            this.bodyJson = json;
            return this;
        }

        /** 自定义结束标记，默认 "[DONE]"。 */
        public Builder doneSignal(String doneSignal) {
            this.doneSignal = doneSignal;
            return this;
        }

        public SseRequest build() {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("SseRequest url 不能为空");
            }
            if (!headers.containsKey("Content-Type") && bodyJson != null) {
                headers.put("Content-Type", "application/json");
            }
            return new SseRequest(this);
        }
    }
}