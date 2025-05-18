package com.dian.demo.test.proxy;

import java.util.Map;

public interface IRequest {

    String url();

    Map<String, Object> params();

    Class<?> responseCls();

}
