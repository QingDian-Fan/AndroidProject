package com.dian.demo.test.proxy;

@URL("http://***.***.***")
public interface LoginApi {

    User login(@Param("username") String username,
               @Param("password") String password);

}
