-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Retrofit APIs and generic response wrappers.
-keep interface com.common.http.RequestService { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep class com.common.http.Result { *; }
-keep class com.common.http.HttpError { *; }
-keep class com.common.http.ResponseHolder { *; }
-keep class com.common.http.ResponseHolder$* { *; }
-keep class com.common.http.HttpClientConfig { *; }

# Room database, DAO and entities.
-keep class com.common.http.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-dontwarn androidx.room.paging.**

# OkHttp optional TLS providers.
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
