package com.common.image.engine

import android.content.Context
import android.widget.ImageView
import coil.annotation.ExperimentalCoilApi
import coil.dispose
import coil.imageLoader
import coil.load
import coil.request.CachePolicy
import coil.size.Scale
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.common.image.ImageDiskCacheStrategy
import com.common.image.ImageEngine
import com.common.image.ImageOptions
import com.common.image.ImageRequest
import com.common.image.ImageScaleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.request.ImageRequest as CoilImageRequest

/**
 * 基于 Coil 的 [ImageEngine] 实现，与 [GlideImageEngine] 等价可替换：
 *
 * ```kotlin
 * CommonImage.init(CoilImageEngine())
 * ```
 *
 * 受 Coil 自身能力限制，以下 [ImageOptions] 项与 Glide 实现存在差异：
 * - [ImageOptions.thumbnail]：Coil 没有缩略图预加载请求的概念，该配置不生效；
 * - [ImageOptions.diskCacheStrategy]：Coil 磁盘缓存只保存原始数据，没有 Glide 的
 *   DATA / RESOURCE 之分，因此除 NONE 外的策略统一映射为「启用磁盘缓存」。
 *
 * 生命周期：`ImageView.load` 会把请求绑定到目标 View，View detach 时自动取消，
 * 无需调用方额外处理。
 */
class CoilImageEngine : ImageEngine {

    override fun load(request: ImageRequest) {
        val target = request.target
        val options = request.options
        // source 为 null 时 Coil 会走 error/fallback 分支，无需额外判空
        target.load(request.source) {
            applyOptions(target, options)
        }
    }

    override fun clear(imageView: ImageView) {
        // dispose 只取消进行中的请求，这里同步清掉已有图像，保持与 Glide clear 的语义一致
        imageView.dispose()
        imageView.setImageDrawable(null)
    }

    /**
     * Coil 没有提供全局暂停请求的能力，此处为空实现。
     * 列表滑动等场景无需手动暂停：Coil 会在 View detach 时自动取消未完成的请求。
     */
    override fun pause(context: Context) = Unit

    /** 与 [pause] 对应的空实现。 */
    override fun resume(context: Context) = Unit

    override fun clearMemory(context: Context) {
        // Coil 的 MemoryCache 实现自带同步，任意线程调用都是安全的
        context.applicationContext.imageLoader.memoryCache?.clear()
    }

    // Coil 2.x 的 DiskCache 仍标记为实验性 API，这里显式 opt-in
    @OptIn(ExperimentalCoilApi::class)
    override suspend fun clearDiskCache(context: Context) {
        withContext(Dispatchers.IO) {
            context.applicationContext.imageLoader.diskCache?.clear()
        }
    }

    private fun CoilImageRequest.Builder.applyOptions(target: ImageView, options: ImageOptions) {
        if (options.placeholderRes != 0) {
            placeholder(options.placeholderRes)
        } else if (options.placeholderDrawable != null) {
            placeholder(options.placeholderDrawable)
        }

        if (options.errorRes != 0) {
            error(options.errorRes)
        } else if (options.errorDrawable != null) {
            error(options.errorDrawable)
        }

        if (options.overrideWidth > 0 && options.overrideHeight > 0) {
            size(options.overrideWidth, options.overrideHeight)
        }

        crossfade(options.crossFade)
        memoryCachePolicy(
            if (options.skipMemoryCache) CachePolicy.DISABLED else CachePolicy.ENABLED
        )
        diskCachePolicy(options.diskCacheStrategy.toCoilCachePolicy())
        applyScaleType(target, options.scaleType)
        applyTransformations(options)
    }

    /**
     * Coil 的 [Scale] 只决定解码采样方式，真正的裁剪由 ImageView 的 scaleType 完成，
     * 因此两者需要成对设置才能得到与 Glide 一致的显示效果。
     */
    private fun CoilImageRequest.Builder.applyScaleType(
        target: ImageView,
        scaleType: ImageScaleType
    ) {
        when (scaleType) {
            ImageScaleType.CENTER_CROP -> {
                target.scaleType = ImageView.ScaleType.CENTER_CROP
                scale(Scale.FILL)
            }

            ImageScaleType.FIT_CENTER -> {
                target.scaleType = ImageView.ScaleType.FIT_CENTER
                scale(Scale.FIT)
            }

            ImageScaleType.CENTER_INSIDE -> {
                target.scaleType = ImageView.ScaleType.CENTER_INSIDE
                scale(Scale.FIT)
            }
            // NONE：保留 ImageView 在布局中已声明的 scaleType，不做干预
            ImageScaleType.NONE -> Unit
        }
    }

    private fun CoilImageRequest.Builder.applyTransformations(options: ImageOptions) {
        when {
            options.circleCrop -> transformations(CircleCropTransformation())
            options.radiusPx > 0 -> transformations(
                RoundedCornersTransformation(options.radiusPx.toFloat())
            )
        }
    }

    private fun ImageDiskCacheStrategy.toCoilCachePolicy(): CachePolicy {
        return when (this) {
            ImageDiskCacheStrategy.NONE -> CachePolicy.DISABLED
            ImageDiskCacheStrategy.AUTOMATIC,
            ImageDiskCacheStrategy.ALL,
            ImageDiskCacheStrategy.DATA,
            ImageDiskCacheStrategy.RESOURCE -> CachePolicy.ENABLED
        }
    }
}
