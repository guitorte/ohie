package com.promptgallery.domain.model

import androidx.compose.ui.graphics.Color

/**
 * A Lightroom-style color flag used for at-a-glance triage of images.
 * Persisted by [name]; unknown values degrade gracefully to [NONE].
 */
enum class ColorLabel(val displayName: String, val swatch: Color) {
    NONE("None", Color(0x00000000)),
    RED("Red", Color(0xFFE53935)),
    ORANGE("Orange", Color(0xFFFB8C00)),
    YELLOW("Yellow", Color(0xFFFDD835)),
    GREEN("Green", Color(0xFF43A047)),
    BLUE("Blue", Color(0xFF1E88E5)),
    PURPLE("Purple", Color(0xFF8E24AA));

    companion object {
        fun fromName(value: String?): ColorLabel =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}
