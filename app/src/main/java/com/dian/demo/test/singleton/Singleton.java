package com.dian.demo.test.singleton;


public class Singleton {
    private Singleton() {
    }

    private static class InnerClass {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return InnerClass.INSTANCE;
    }

}
