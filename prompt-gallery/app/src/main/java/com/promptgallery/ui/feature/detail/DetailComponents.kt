package com.promptgallery.ui.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.promptgallery.domain.model.Collection
import com.promptgallery.domain.model.Image
import com.promptgallery.domain.model.ImageVersion
import com.promptgallery.domain.model.Tag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditPromptDialog(
    initialPrompt: String,
    initialNegative: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var prompt by remember { mutableStateOf(initialPrompt) }
    var negative by remember { mutableStateOf(initialNegative) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit prompt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                OutlinedTextField(
                    value = negative,
                    onValueChange = { negative = it },
                    label = { Text("Negative prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(prompt, negative) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistorySheet(
    versions: List<ImageVersion>,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Version history", style = MaterialTheme.typography.titleLarge)
            if (versions.isEmpty()) {
                Text(
                    "No earlier versions yet. Edits to the prompt are recorded here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(versions)
            }
        }
    }
}

// LazyColumn DSL helper kept separate so it can render the version rows.
private fun androidx.compose.foundation.lazy.LazyListScope.items(versions: List<ImageVersion>) {
    items(versions.size) { index ->
        val version = versions[index]
        VersionRow(version)
    }
}

@Composable
private fun VersionRow(version: ImageVersion) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "v${version.versionNumber} · ${formatDate(version.editedDate)}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                version.prompt.take(160),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditorDialog(
    allTags: List<Tag>,
    selectedTagNames: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(selectedTagNames) } }
    var newTag by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTags.forEach { tag ->
                        val isSelected = tag.name in selected
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selected.remove(tag.name) else selected.add(tag.name)
                            },
                            label = { Text(tag.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("New tag") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (newTag.isNotBlank()) {
                    TextButton(onClick = {
                        if (newTag.trim() !in selected) selected.add(newTag.trim())
                        newTag = ""
                    }) { Text("Add \"$newTag\"") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected.toList()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionEditorDialog(
    allCollections: List<Collection>,
    selectedIds: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(selectedIds) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collections") },
        text = {
            if (allCollections.isEmpty()) {
                Text("No collections yet. Create one from the Library tab.")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allCollections.filter { !it.isSmartCollection }.forEach { collection ->
                        val isSelected = collection.id in selected
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selected.remove(collection.id) else selected.add(collection.id)
                            },
                            label = { Text(collection.name) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected.toList()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun MetadataSection(image: Image) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Details", style = MaterialTheme.typography.titleMedium)
            MetaRow("Model", image.aiModel)
            MetaRow("Dimensions", if (image.width > 0) "${image.width} × ${image.height}" else "")
            MetaRow("Aspect ratio", image.aspectRatio)
            MetaRow("Seed", image.seed?.toString() ?: "")
            MetaRow("Sampler", image.sampler)
            MetaRow("CFG", image.cfg?.toString() ?: "")
            MetaRow("Steps", image.steps?.toString() ?: "")
            MetaRow("File", image.fileName)
            MetaRow("Size", if (image.fileSize > 0) "${image.fileSize / 1024} KB" else "")
            MetaRow("Created", formatDate(image.creationDate))
            MetaRow("Imported", formatDate(image.importDate))
            MetaRow("Source", image.sourceUrl)
            if (image.customNotes.isNotBlank()) {
                Text("Notes", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                Text(image.customNotes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
private fun formatDate(millis: Long): String =
    if (millis <= 0) "" else dateFormat.format(Date(millis))
