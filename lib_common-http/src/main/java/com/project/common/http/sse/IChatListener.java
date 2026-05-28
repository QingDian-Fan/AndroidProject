package com.project.common.http.sse;

public interface IChatListener {
    void onChatResult(String chatString,boolean isEnd,boolean isFirstPackage);
    void onError(String errorMsg);
}
