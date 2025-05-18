package com.dian.demo.test.proxy;

public interface INetExecutor {

    <T> T execute(IRequest request);

}
