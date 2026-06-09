# Auth uses reflection for the generated APT helper and Android framework hooks.
-keep class com.common.auth.** { *; }
-keep class com.dian.demo.apt.** { *; }

# Keep app classes marked for login routing and the generated login checker target.
-keep @com.dian.annotation.LoginPage class * { *; }
-keep @com.dian.annotation.RequireLogin class * { *; }
-keepclassmembers class * {
    @com.dian.annotation.CheckLogin <methods>;
}
