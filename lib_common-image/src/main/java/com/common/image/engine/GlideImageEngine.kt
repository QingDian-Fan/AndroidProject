package com.common.image.engine

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.common.image.ImageDiskCacheStrategy
import com.common.image.ImageEngine
import com.common.image.ImageOptions
import com.common.image.ImageRequest
import com.common.image.ImageScaleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GlideImageEngine : ImageEngine {
    override fun load(request: ImageRequest) {
        val options = request.options
        var glideRequest = Glide.with(request.target).load(request.source)
        val requestOptions = buildRequestOptions(options)

        if (options.crossFade) {
            glideRequest = glideRequest.transition(DrawableTransitionOptions.withCrossFade())
        }
        if (options.thumbnail > 0f) {
            glideRequest = glideRequest.thumbnail(options.thumbnail)
        }

        glideRequest
            .apply(requestOptions)
            .into(request.target)
    }

    override fun clear(imageView: ImageView) {
        Glide.with(imageView).clear(imageView)
    }

    override fun pause(context: Context) {
        Glide.with(context).pauseRequests()
    }

    override fun resume(context: Context) {
        Glide.with(context).resumeRequests()
    }

    override fun clearMemory(context: Context) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Glide.get(context.applicationContext).clearMemory()
        } else {
            Handler(Looper.getMainLooper()).post {
                Glide.get(context.applicationContext).clearMemory()
            }
        }
    }

    override suspend fun clearDiskCache(context: Context) {
        withContext(Dispatchers.IO) {
            Glide.get(context.applicationContext).clearDiskCache()
        }
    }

    private fun buildRequestOptions(options: ImageOptions): RequestOptions {
        var requestOptions = RequestOptions()

        if (options.placeholderRes != 0) {
            requestOptions = requestOptions.placeholder(options.placeholderRes)
        } else if (options.placeholderDrawable != null) {
            requestOptions = requestOptions.placeholder(options.placeholderDrawable)
        }

        if (options.errorRes != 0) {
            requestOptions = requestOptions.error(options.errorRes)
        } else if (options.errorDrawable != null) {
            requestOptions = requestOptions.error(options.errorDrawable)
        }

        if (options.overrideWidth > 0 && options.overrideHeight > 0) {
            requestOptions = requestOptions.override(options.overrideWidth, options.overrideHeight)
        }

        requestOptions = requestOptions
            .skipMemoryCache(options.skipMemoryCache)
            .diskCacheStrategy(options.diskCacheStrategy.toGlideStrategy())

        return requestOptions.applyTransformations(options)
    }

    private fun RequestOptions.applyTransformations(options: ImageOptions): RequestOptions {
        if (options.circleCrop) {
            return transform(CircleCrop())
        }

        if (options.radiusPx > 0) {
            val roundedCorners = RoundedCorners(options.radiusPx)
            val transformations = when (options.scaleType) {
                ImageScaleType.CENTER_CROP -> arrayOf<Transformation<Bitmap>>(CenterCrop(), roundedCorners)
                ImageScaleType.FIT_CENTER -> arrayOf<Transformation<Bitmap>>(FitCenter(), roundedCorners)
                ImageScaleType.CENTER_INSIDE -> arrayOf<Transformation<Bitmap>>(CenterInside(), roundedCorners)
                ImageScaleType.NONE -> arrayOf<Transformation<Bitmap>>(roundedCorners)
            }
            return transform(MultiTransformation(transformations.toList()))
        }

        return when (options.scaleType) {
            ImageScaleType.CENTER_CROP -> centerCrop()
            ImageScaleType.FIT_CENTER -> fitCenter()
            ImageScaleType.CENTER_INSIDE -> centerInside()
            ImageScaleType.NONE -> this
        }
    }

    private fun ImageDiskCacheStrategy.toGlideStrategy(): DiskCacheStrategy {
        return when (this) {
            ImageDiskCacheStrategy.AUTOMATIC -> DiskCacheStrategy.AUTOMATIC
            ImageDiskCacheStrategy.ALL -> DiskCacheStrategy.ALL
            ImageDiskCacheStrategy.DATA -> DiskCacheStrategy.DATA
            ImageDiskCacheStrategy.RESOURCE -> DiskCacheStrategy.RESOURCE
            ImageDiskCacheStrategy.NONE -> DiskCacheStrategy.NONE
        }
    }
}
