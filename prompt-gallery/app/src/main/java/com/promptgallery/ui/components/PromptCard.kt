package com.promptgallery.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.promptgallery.domain.model.Image

/**
 * The signature prompt card shown beneath an image. Supports expand/collapse,
 * one-tap copy of prompt and negative prompt, and edit/duplicate/share actions.
 */
@Composable
fun PromptCard(
    image: Image,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(true) }
    val copy = rememberCopyAction()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Prompt", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                    Text(if (expanded) "Collapse" else "Expand")
                }
            }

            PromptBody(
                text = image.prompt.ifBlank { "No prompt saved" },
                expanded = expanded,
            )

            // Primary one-tap copy action — the most important interaction.
            FilledTonalButton(
                onClick = { copy("Prompt", image.prompt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = image.prompt.isNotBlank(),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text("Copy prompt", Modifier.padding(start = 8.dp))
            }

            if (image.negativePrompt.isNotBlank()) {
                Text(
                    "Negative prompt",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                PromptBody(text = image.negativePrompt, expanded = expanded)
                TextButton(
                    onClick = { copy("Negative prompt", image.negativePrompt) },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Text("Copy negative", Modifier.padding(start = 8.dp))
                }
            }

            // Secondary actions.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onEdit,
                    label = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                )
                AssistChip(
                    onClick = onDuplicate,
                    label = { Text("Duplicate") },
                    leadingIcon = { Icon(Icons.Outlined.FileCopy, contentDescription = null) },
                )
                AssistChip(
                    onClick = onShare,
                    label = { Text("Share") },
                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun PromptBody(text: String, expanded: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        maxLines = if (expanded) Int.MAX_VALUE else 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    )
}
