package com.dian.demo.test.proxy;

import com.google.gson.Gson;

public class DefaultNetExecutor implements INetExecutor {

    private static final Gson sGson = new Gson();

    @Override
    public <T> T execute(IRequest request) {
        String response = VirtualHelper.request(request.url(), request.params());
        return (T) sGson.fromJson(response, request.responseCls());
    }

}
