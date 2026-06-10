# Keep public security APIs stable for host apps that call them from Java or reflection.
-keep class com.common.security.** { *; }
