package com.promptgallery.ui.feature.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.promptgallery.domain.model.PromptTemplate
import com.promptgallery.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var fillTarget by remember { mutableStateOf<PromptTemplate?>(null) }
    var editTarget by remember { mutableStateOf<PromptTemplate?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Prompt templates") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editTarget = null; showEditor = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New template")
            }
        },
    ) { padding ->
        if (state.templates.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Description,
                title = "No templates",
                message = "Save reusable prompt blueprints with fillable variables.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.grouped.forEach { (category, templates) ->
                    item(key = "header_$category") {
                        Text(category, style = MaterialTheme.typography.titleMedium)
                    }
                    items(templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onUse = { fillTarget = template },
                            onEdit = { editTarget = template; showEditor = true },
                            onDelete = { viewModel.delete(template.id) },
                        )
                    }
                }
            }
        }
    }

    fillTarget?.let { template ->
        FillTemplateDialog(
            template = template,
            onRender = { values -> viewModel.render(template, values) },
            onCopied = { viewModel.markUsed(template) },
            onDismiss = { fillTarget = null },
        )
    }

    if (showEditor) {
        TemplateEditorDialog(
            existing = editTarget,
            onSave = { name, category, prompt, negative, description ->
                viewModel.saveTemplate(editTarget, name, category, prompt, negative, description)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
private fun TemplateCard(
    template: PromptTemplate,
    onUse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onUse)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(template.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Description, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                }
            }
            if (template.description.isNotBlank()) {
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                template.promptText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2,
            )
            if (template.variables.isNotEmpty()) {
                Text(
                    "Variables: ${template.variables.joinToString { "{${it.key}}" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                "Used ${template.useCount}×",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
