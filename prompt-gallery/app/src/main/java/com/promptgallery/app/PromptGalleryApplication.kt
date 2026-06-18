package com.promptgallery.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Besides bootstrapping Hilt it configures a global
 * Coil [ImageLoader] tuned for a large local gallery: a generous memory cache
 * for smooth scrolling and a dedicated disk cache for decoded thumbnails.
 */
@HiltAndroidApp
class PromptGalleryApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        // Local files never change under us, so we don't re-validate from network.
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}
