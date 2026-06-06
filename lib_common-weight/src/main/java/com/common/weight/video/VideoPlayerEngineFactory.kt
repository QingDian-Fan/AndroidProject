package com.common.weight.video

import android.content.Context

fun interface VideoPlayerEngineFactory {

    fun create(context: Context): VideoPlayerEngine
}
