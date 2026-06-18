package com.common.auth.hook;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.common.auth.AuthManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class LoginHookUtil {
    private static final String TAG = "LoginHookUtil";
    private static boolean hookInstalled = false;

    private LoginHookUtil() {
    }

    public static void hookAms(Context context) {
        HookAms(context);
    }

    public static synchronized void HookAms(Context context) {
        if (hookInstalled || context == null) {
            return;
        }

        try {
            Field singletonField;
            Class<?> managerInterfaceClass;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Class<?> activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager");
                singletonField = activityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton");
                managerInterfaceClass = Class.forName("android.app.IActivityTaskManager");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                singletonField = ActivityManager.class.getDeclaredField("IActivityManagerSingleton");
                managerInterfaceClass = Class.forName("android.app.IActivityManager");
            } else {
                Class<?> activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
                singletonField = activityManagerNativeClass.getDeclaredField("gDefault");
                managerInterfaceClass = Class.forName("android.app.IActivityManager");
            }
            singletonField.setAccessible(true);
            Object singleton = singletonField.get(null);

            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);

            Object mInstance = mInstanceField.get(singleton);
            if (mInstance == null) {
                Method getMethod = singletonClass.getDeclaredMethod("get");
                getMethod.setAccessible(true);
                mInstance = getMethod.invoke(singleton);
            }

            if (mInstance == null) {
                return;
            }

            Object rawManager = mInstance;
            Object proxy = Proxy.newProxyInstance(
                    context.getClassLoader(),
                    new Class[]{managerInterfaceClass},
                    (proxyObject, method, args) -> {
                        if ("startActivity".equals(method.getName()) && args != null) {
                            for (int i = 0; i < args.length; i++) {
                                if (args[i] instanceof Intent) {
                                    Intent originIntent = (Intent) args[i];
                                    ComponentName componentName = originIntent.getComponent();
                                    if (componentName != null && !isLogin() && isRequireLogin(componentName.getClassName())) {
                                        Intent loginIntent = AuthManager.createLoginIntent(context, originIntent);
                                        if (loginIntent != null) {
                                            args[i] = loginIntent;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        return method.invoke(rawManager, args);
                    }
            );

            mInstanceField.set(singleton, proxy);
            hookInstalled = true;
        } catch (Exception e) {
            Log.e(TAG, "hook AMS failed", e);
        }
    }

    public static Class<?> getLoginActivityClass(String helperClassName) {
        try {
            Class<?> clazz = Class.forName(helperClassName);
            Method method = clazz.getMethod("getLoginActivity");
            String activityName = (String) method.invoke(null);
            if (activityName == null || activityName.length() == 0 || "null".equals(activityName)) {
                return null;
            }
            return Class.forName(activityName);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRequireLogin(String activityName) {
        if (activityName == null || activityName.length() == 0) {
            return false;
        }

        try {
            Class<?> clazz = Class.forName(AuthManager.getAuthConfig().getGeneratedHelperClassName());
            Method method = clazz.getMethod("getRequireLoginList");
            List<?> activities = (List<?>) method.invoke(null);
            return activities != null && activities.contains(activityName);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isLogin() {
        if (AuthManager.isLogin()) {
            return true;
        }
        if (AuthManager.hasCustomLoginChecker()) {
            return false;
        }
        return isGeneratedLogin();
    }

    private static boolean isGeneratedLogin() {
        try {
            Class<?> clazz = Class.forName(AuthManager.getAuthConfig().getGeneratedHelperClassName());
            Method method = clazz.getMethod("getJudgeLoginMethod");
            String methodPath = (String) method.invoke(null);
            if (methodPath == null || methodPath.length() == 0 || "null".equals(methodPath)) {
                return false;
            }

            String[] parts = methodPath.split("#");
            if (parts.length != 2) {
                return false;
            }

            Class<?> loginClass = Class.forName(parts[0]);
            Method loginMethod = loginClass.getMethod(parts[1]);
            Object result = loginMethod.invoke(null);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }
}
