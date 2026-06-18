package com.promptgallery.core.util

import java.util.UUID

/** Centralised identifier generation so it can be stubbed in tests. */
object Ids {
    fun newId(): String = UUID.randomUUID().toString()
}
