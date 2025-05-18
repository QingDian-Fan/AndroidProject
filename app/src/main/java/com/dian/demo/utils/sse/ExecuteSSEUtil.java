package com.dian.demo.utils.sse;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dian.demo.utils.sse.ds.DSReceiveResponse;
import com.dian.demo.utils.sse.ds.DSSendData;
import com.dian.demo.utils.sse.ds.DSSendMessage;
import com.dian.demo.http.gson.GsonFactory;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class ExecuteSSEUtil {
    private static volatile ExecuteSSEUtil instance;
    private final DSSendData mChatDatas;
    private final ArrayList<DSSendMessage> messages;


    private ExecuteSSEUtil() {
        messages = new ArrayList<>();
        mChatDatas = new DSSendData("deepseek-chat", true, messages);
    }

    public static ExecuteSSEUtil getInstance() {
        if (null == instance) {
            synchronized (ExecuteSSEUtil.class) {
                if (null == instance) {
                    instance = new ExecuteSSEUtil();

                }
            }
        }
        return instance;
    }

    private final Executor workService = Executors.newSingleThreadExecutor();
    private StringBuilder stringBuilder = new StringBuilder();
    private StringBuilder mStringBuilder = new StringBuilder();
    private boolean isFirstPackage;
    private EventSource eventSource;

    public void executeSSE( boolean isUser, String content,IChatListener mChatListener) {
        workService.execute(() -> {
            Log.e("TAG--->Deek", "send::" + content);
            cancelRequest();
            clearBuffer();
            messages.add(new DSSendMessage(isUser ? "user" : "system", content));
            String paramJson = GsonFactory.getSingletonGson().toJson(mChatDatas);
            RequestBody formBody = RequestBody.create(paramJson, MediaType.parse("application/json; charset=utf-8"));
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.addHeader("Content-Type", "application/json");
            requestBuilder.addHeader("Authorization", "Bearer sk-8150839b306e486da7575741e827a0be");
            Request request = requestBuilder.url("https://api.deepseek.com/chat/completions").post(formBody).build();
            EventSource.Factory factory = EventSources.createFactory(OkHttpUtil.getInstance());
            eventSource = factory.newEventSource(request, new EventSourceListener() {
                @Override
                public void onClosed(@NonNull EventSource eventSource) {
                    super.onClosed(eventSource);

                }

                @Override
                public void onEvent(@NonNull EventSource eventSource, @Nullable String id, @Nullable String type, @NonNull String data) {
                    super.onEvent(eventSource, id, type, data);
                    Log.e("TAG--->SSEListener", "responseString:data-" + data);
                    if ("[DONE]".equals(data)) {
                        Log.e("TAG--->Deek", "receive::" + stringBuilder.toString());
                        messages.add(new DSSendMessage("system", stringBuilder.toString()));
                        clearBuffer();

                        String contentString = mStringBuilder.toString();
                        mChatListener.onChatResult(contentString, true, isFirstPackage);
                        isFirstPackage = false;
                        return;
                    }

                    DSReceiveResponse mResponseData = GsonFactory.getSingletonGson().fromJson(data, DSReceiveResponse.class);
                    if (mResponseData.choices != null && mResponseData.choices.size() != 0) {
                        for (int i = 0; i < mResponseData.choices.size(); i++) {
                            stringBuilder.append(mResponseData.choices.get(i).delta.content);
                            mStringBuilder.append(mResponseData.choices.get(i).delta.content);
                        }
                    }
                    String contentString = mStringBuilder.toString();
                    if (contentString.length() > 10) {
                        Pattern pattern = Pattern.compile("[，。]");
                        Matcher matcher = pattern.matcher(contentString);
                        int splitIndex = 0;
                        while (matcher.find()) {
                            int index = matcher.start();
                            if (index > 10) {
                                if (index < contentString.length()) splitIndex = index + 1;
                                else splitIndex = index;
                                break;
                            }
                        }
                        if (splitIndex != 0) {
                            String startString = contentString.substring(0, splitIndex);
                            String endString = contentString.substring(splitIndex);
                            mStringBuilder.setLength(0);
                            mStringBuilder.append(endString);
                            mChatListener.onChatResult(startString.replaceAll("[* -]", ""), false, isFirstPackage);
                            isFirstPackage = false;
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                    super.onFailure(eventSource, t, response);
                    if (t != null && ("Socket closed".equals(t.getMessage()) || "Socket is closed".equals(t.getMessage()) || "stream was reset: CANCEL".equals(t.getMessage()))) {
                        return;
                    }
                    if (t != null) {
                        Log.e("TAG--->", "使用事件源时出现异常:" + t.getMessage());
                        mChatListener.onError(t.getMessage());
                    }
                }

                @Override
                public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
                    super.onOpen(eventSource, response);

                }
            });
        });
    }

    public void cancelRequest() {
        if (eventSource != null) {
            eventSource.cancel();
        }
    }

    public void cleanChat() {
        messages.clear();
        clearBuffer();
    }

    public void clearBuffer() {
        isFirstPackage = true;
        if (stringBuilder != null) {
            stringBuilder.setLength(0);
        } else {
            stringBuilder = new StringBuilder();
        }
        if (mStringBuilder != null) {
            mStringBuilder.setLength(0);
        } else {
            mStringBuilder = new StringBuilder();
        }
    }
}
