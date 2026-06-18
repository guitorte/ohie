package com.promptgallery.ui.feature.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.promptgallery.domain.model.PromptTemplate
import com.promptgallery.domain.usecase.FilledPrompt
import com.promptgallery.ui.components.rememberCopyAction

@Composable
fun FillTemplateDialog(
    template: PromptTemplate,
    onRender: (Map<String, String>) -> FilledPrompt,
    onCopied: () -> Unit,
    onDismiss: () -> Unit,
) {
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            template.variables.forEach { put(it.key, it.defaultValue) }
        }
    }
    val copy = rememberCopyAction()
    val filled: FilledPrompt = onRender(values)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(template.name) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                template.variables.forEach { variable ->
                    OutlinedTextField(
                        value = values[variable.key].orEmpty(),
                        onValueChange = { values[variable.key] = it },
                        label = { Text(variable.label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                Text("Preview", style = MaterialTheme.typography.labelMedium)
                Text(
                    filled.prompt,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = { copy("Prompt", filled.prompt); onCopied() }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text("Copy", Modifier.padding(start = 8.dp))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun TemplateEditorDialog(
    existing: PromptTemplate?,
    onSave: (name: String, category: String, prompt: String, negative: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var prompt by remember { mutableStateOf(existing?.promptText ?: "") }
    var negative by remember { mutableStateOf(existing?.negativePromptText ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New template" else "Edit template") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Prompt (use {variable})") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(negative, { negative = it }, label = { Text("Negative prompt") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Text(
                    "Tip: wrap placeholders in braces, e.g. {subject}. They become editable fields when the template is used.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, category, prompt, negative, description) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
