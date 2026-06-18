package com.promptgallery.core.util

import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Qualifiers and an abstraction over coroutine dispatchers. Injecting these
 * (rather than referencing Dispatchers.* directly) keeps ViewModels and
 * repositories testable with a deterministic test dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}
