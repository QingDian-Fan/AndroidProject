java.lang.IllegalStateException: Could not execute method for android:onClick
at androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick(AppCompatViewInflater.java:473)
at android.view.View.performClick(View.java:7729)
at com.google.android.material.button.MaterialButton.performClick(MaterialButton.java:1218)
at android.view.View.performClickInternal(View.java:7698)
at android.view.View.access$3700(View.java:886)
at android.view.View$PerformClick.run(View.java:30220)
at android.os.Handler.handleCallback(Handler.java:966)
at android.os.Handler.dispatchMessage(Handler.java:110)
at android.os.Looper.loopOnce(Looper.java:205)
at android.os.Looper.loop(Looper.java:293)
at android.app.ActivityThread.loopProcess(ActivityThread.java:9998)
at android.app.ActivityThread.main(ActivityThread.java:9987)
at java.lang.reflect.Method.invoke(Native Method)
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:586)
at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1240)
Caused by: java.lang.reflect.InvocationTargetException
at java.lang.reflect.Method.invoke(Native Method)
at androidx.appcompat.app.AppCompatViewInflater$DeclaredOnClickListener.onClick(AppCompatViewInflater.java:468)
at android.view.View.performClick(View.java:7729) 
at com.google.android.material.button.MaterialButton.performClick(MaterialButton.java:1218) 
at android.view.View.performClickInternal(View.java:7698) 
at android.view.View.access$3700(View.java:886) 
                                                                                                    	at android.view.View$PerformClick.run(View.java:30220) 
at android.os.Handler.handleCallback(Handler.java:966) 
at android.os.Handler.dispatchMessage(Handler.java:110) 
at android.os.Looper.loopOnce(Looper.java:205) 
at android.os.Looper.loop(Looper.java:293) 
at android.app.ActivityThread.loopProcess(ActivityThread.java:9998) 
at android.app.ActivityThread.main(ActivityThread.java:9987) 
at java.lang.reflect.Method.invoke(Native Method) 
at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:586) 
                                                                                                    	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1240) 
                                                                                                    Caused by: java.lang.NoSuchMethodError: No static method aspectOf()Lcom/common/aop/CheckPermissionsAspect; in class Lcom/common/aop/CheckPermissionsAspect; or its super classes (declaration of 'com.common.aop.CheckPermissionsAspect' appears in /data/app/~~B-kVl1A444vUehLtne56TQ==/com.demo.project.debug-WRZUVwRmtazthLPh22vYHA==/base.apk!classes5.dex)
at com.common.media.picker.ImageSelectActivity$Companion.start(ImageSelectActivity.kt:1)
at com.common.media.picker.ImageSelectUtil.create(ImageSelectUtil.kt:64)
at com.demo.project.ui.HomeActivity.clickView(HomeActivity.kt:97)