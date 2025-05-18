package com.dian.demo.utils.sse;

import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

public class OkHttpUtil {
    private volatile static OkHttpClient okHttpClient;

    public static ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);

    public static OkHttpClient getInstance() {
        if (null == okHttpClient) { //加同步安全
            synchronized (OkHttpClient.class) {
                if (null == okHttpClient) { //okhttp可以缓存数据....指定缓存路径
                    okHttpClient = new OkHttpClient.Builder()//构建器
                            .proxy(Proxy.NO_PROXY) //来屏蔽系统代理
                            .connectTimeout(600, TimeUnit.SECONDS)//连接超时
                            .writeTimeout(600, TimeUnit.SECONDS)//写入超时
                            .readTimeout(600, TimeUnit.SECONDS)//读取超时
                            .build();
                    okHttpClient.dispatcher().setMaxRequestsPerHost(200);
                    okHttpClient.dispatcher().setMaxRequests(200);
                }
            }
        }
        return okHttpClient;
    }
}
