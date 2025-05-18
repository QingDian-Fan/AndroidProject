package com.dian.demo.utils.sse;

import android.util.Log;

import androidx.annotation.NonNull;


import com.dian.demo.utils.sse.ds.DSReceiveResponse;
import com.dian.demo.http.gson.GsonFactory;

import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

public class SSEListener extends EventSourceListener {

    private StringBuilder stringBuilder = new StringBuilder();

    private boolean isFirstPackage;


    @Override
    public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {

    }

    /**
     * {
     * "id": "ec8772cd-08bd-4c77-9a3c-366bde2d08db",
     * "object": "chat.completion.chunk",
     * "created": 1746523126,
     * "model": "deepseek-chat",
     * "system_fingerprint": "fp_8802369eaa_prod0425fp8",
     * "choices": [
     * {
     * "index": 0,
     * "delta": {
     * "content": "闲聊"
     * },
     * "logprobs": null,
     * "finish_reason": null
     * }
     * ]
     * }
     *
     * @param eventSource
     * @param id
     * @param type
     * @param data
     */
    @Override
    public void onEvent(@NonNull EventSource eventSource, String id, String type, @NonNull String data) {
        Log.e("TAG--->SSEListener", "responseString:data-" + data);
        if ("[DONE]".equals(data)) {
            Log.e("TAG--->", "response::" + stringBuilder.toString());
            return;
        }
        DSReceiveResponse mResponseData = GsonFactory.getSingletonGson().fromJson(data, DSReceiveResponse.class);
        if (mResponseData.choices != null && mResponseData.choices.size() != 0) {
            for (int i = 0; i < mResponseData.choices.size(); i++) {
                stringBuilder.append(mResponseData.choices.get(i).delta.content);
            }
        }


    }

    @Override
    public void onClosed(@NonNull EventSource eventSource) {

    }

    @Override
    public void onFailure(@NonNull EventSource eventSource, final Throwable t, final Response response) {
        Log.e("TAG--->SSEListener", "onFailure:data-" + t.getMessage());
        if (t != null && ("Socket closed".equals(t.getMessage()) || "Socket is closed".equals(t.getMessage()) || "stream was reset: CANCEL".equals(t.getMessage()))) {
            return;
        }
    }

    public void clearBuffer() {
        isFirstPackage = true;
        if (stringBuilder != null) {
            stringBuilder.setLength(0);
        } else {
            stringBuilder = new StringBuilder();
        }
    }


}
