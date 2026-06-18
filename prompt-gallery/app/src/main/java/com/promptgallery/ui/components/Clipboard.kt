package com.promptgallery.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * One-tap copy with tactile + visual confirmation. Returns a lambda the caller
 * can wire directly to a click.
 *
 * On Android 13+ the system shows its own copy confirmation, so we suppress the
 * Toast there to avoid double feedback, but always fire haptic feedback.
 */
@Composable
fun rememberCopyAction(): (label: String, value: String) -> Unit {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(context) {
        { label, value ->
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText(label, value))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
            } else {
                // Reinforce silently for accessibility without a second visible banner.
                view.announceForAccessibility("$label copied")
            }
        }
    }
}
