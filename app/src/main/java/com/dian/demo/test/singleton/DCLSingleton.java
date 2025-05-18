package com.dian.demo.test.singleton;

public class DCLSingleton {
    private volatile static DCLSingleton singleton = null;

    private DCLSingleton() {
    }

    public static DCLSingleton getInstance() {
        if (null == singleton) {
            synchronized (DCLSingleton.class) {
                if (null == singleton) {
                    singleton = new DCLSingleton();
                }
            }
        }
        return singleton;
    }

}
