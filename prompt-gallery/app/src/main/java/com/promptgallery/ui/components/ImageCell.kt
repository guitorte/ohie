package com.promptgallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.promptgallery.domain.model.Image
import java.io.File

/**
 * A single gallery tile. Renders the cached thumbnail, overlays favorite and
 * color-label affordances, and switches to a selection appearance during
 * multi-select. Long-press enters selection (Google Photos pattern).
 */
@Composable
fun ImageCell(
    image: Image,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    cornerRadius: Int = 14,
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(if (selected) 0.88f else 1f, label = "cellScale")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(image.thumbnailPath))
                .crossfade(true)
                .build(),
            contentDescription = image.title.ifBlank { image.fileName },
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(RoundedCornerShape(cornerRadius.dp)),
        )

        // Bottom-leading color label and favorite heart.
        ColorLabelDot(
            label = image.colorLabel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )
        if (image.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(18.dp),
            )
        }

        if (selectionMode) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (selected) Color.Black.copy(alpha = 0.25f) else Color.Transparent,
                    ),
            )
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (selected) "Selected" else "Not selected",
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp),
            )
        }
    }
}
