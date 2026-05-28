package com.project.common.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

/**
 * Activity 和 Fragment 的生命周期
 */
public class LifecycleLogger {

    public void install(Application application) {
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallback());
    }


    private class ActivityLifecycleCallback implements Application.ActivityLifecycleCallbacks {

        private final FragmentLifecycleCallback fragmentLifecycleCallback = new FragmentLifecycleCallback();

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            if (activity instanceof FragmentActivity) {
                ((FragmentActivity) activity).getSupportFragmentManager()
                        .registerFragmentLifecycleCallbacks(fragmentLifecycleCallback, true);
            }
            log(activity, "onActivityCreated");
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            log(activity, "onActivityStarted");
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            log(activity, "onActivityResumed");
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            log(activity, "onActivityPaused");
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            log(activity, "onActivityStopped");
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            log(activity, "onActivitySaveInstanceState");
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            log(activity, "onActivityDestroyed");
        }
    }

    private class FragmentLifecycleCallback extends FragmentManager.FragmentLifecycleCallbacks {
        @Override
        public void onFragmentAttached(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Context context) {
            log(f, "onFragmentAttached");
        }

        @Override
        public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            log(f, "onFragmentCreated");
        }

        @Override
        public void onFragmentActivityCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
            log(f, "onFragmentActivityCreated");
        }

        @Override
        public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
            log(f, "onFragmentViewCreated");
        }

        @Override
        public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
            log(f, "onFragmentStarted");
        }

        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            log(f, "onFragmentResumed");
        }

        @Override
        public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
            log(f, "onFragmentPaused");
        }

        @Override
        public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
            log(f, "onFragmentStopped");
        }

        @Override
        public void onFragmentSaveInstanceState(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull Bundle outState) {
            log(f, "onFragmentSaveInstanceState");
        }

        @Override
        public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            log(f, "onFragmentViewDestroyed");
        }

        @Override
        public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            log(f, "onFragmentDestroyed");
        }

        @Override
        public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {
            log(f, "onFragmentDetached");
        }
    }

    private void log(Object object, String state) {
        LogUtil.e(String.format("%s %s", object.getClass().getSimpleName(), state));
    }

}
