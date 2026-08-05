#############################################
# lib-common-media-picker consumer rules
#############################################

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Parcelable Creator 由 Framework 按固定字段名反射读取。
# 依据：AlbumInfo.kt（@Parcelize），通过 Bundle 在 Activity/DialogFragment 间传递。
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ImageSelectUtil 是本模块对外入口（宿主链式调用），回调接口方法名构成二进制契约。
# 依据：ImageSelectUtil.kt、ImageSelectListener.kt、ImageCancelListener.kt
-keep interface com.common.media.picker.ImageSelectListener { *; }
-keep interface com.common.media.picker.ImageCancelListener { *; }
-keepclassmembers class com.common.media.picker.ImageSelectUtil {
    public <methods>;
}

# ImageSelectActivity.start() 上标注了 @CheckPermissions，切面在运行期读取该注解；
# 注解与方法的保留规则由 lib_common-aop 的 consumer rules 统一提供，此处不重复。

# 说明：ImagePreviewActivity.openVideoPlayerByReflect() 反射的
# "com.demo.project.ui.activity.VideoPlayerActivity#start(Context, String)"
# 属于**宿主**的类，其名称必须由 demo 的规则固定（见 demo/proguard-rules.pro）。
# 本模块无法预知宿主类名，不在此添加规则。

# PhotoView 未随 AAR 提供 consumer rules，其对可选依赖的引用需要抑制。
-dontwarn com.github.chrisbanes.photoview.**

# 说明：Activity 由本模块 Manifest 注册，AGP 会自动生成对应 keep 规则；
# 适配器、Dialog、内部工具类均无反射入口，允许 R8 裁剪、优化与改名，
# 因此移除原来的 com.common.media.picker.** 整包 keep。
