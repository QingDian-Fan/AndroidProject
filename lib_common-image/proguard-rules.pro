# Glide integration.
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.bumptech.glide.GeneratedRequestManagerFactory { *; }
-dontwarn com.bumptech.glide.**

# Public image facade can be called from Java or by reflection in host apps.
-keep class com.common.image.** { *; }
