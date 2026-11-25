package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LuminaApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var coilImageLoader: ImageLoader

    override fun newImageLoader(): ImageLoader = coilImageLoader

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (!::coilImageLoader.isInitialized) return
        val memCache = coilImageLoader.memoryCache ?: return
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            memCache.clear()
        } else {
            memCache.trimMemory(level)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::coilImageLoader.isInitialized) {
            coilImageLoader.memoryCache?.clear()
        }
    }
}
