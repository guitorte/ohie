package com.promptgallery.ui.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.promptgallery.core.util.Ids
import com.promptgallery.domain.model.PromptTemplate
import com.promptgallery.domain.model.TemplateVariable
import com.promptgallery.domain.repository.TemplateRepository
import com.promptgallery.domain.usecase.FillTemplateUseCase
import com.promptgallery.domain.usecase.FilledPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplatesUiState(
    val templates: List<PromptTemplate> = emptyList(),
    val grouped: Map<String, List<PromptTemplate>> = emptyMap(),
)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val fillTemplate: FillTemplateUseCase,
) : ViewModel() {

    val uiState: StateFlow<TemplatesUiState> = templateRepository.observeAll()
        .map { list ->
            TemplatesUiState(
                templates = list,
                grouped = list.groupBy { it.category.ifBlank { "Uncategorized" } },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplatesUiState())

    init {
        viewModelScope.launch { templateRepository.ensureSeeded() }
    }

    /** Pure render — safe to call during composition for a live preview. */
    fun render(template: PromptTemplate, values: Map<String, String>): FilledPrompt =
        fillTemplate(template, values)

    /** Records usage; call this only when the user actually copies/reuses. */
    fun markUsed(template: PromptTemplate) {
        viewModelScope.launch { templateRepository.markUsed(template.id) }
    }

    fun saveTemplate(
        existing: PromptTemplate?,
        name: String,
        category: String,
        prompt: String,
        negative: String,
        description: String,
    ) {
        if (name.isBlank() || prompt.isBlank()) return
        val variables = extractVariables(prompt + " " + negative)
        val template = (existing ?: blankTemplate()).copy(
            name = name.trim(),
            category = category.trim(),
            promptText = prompt,
            negativePromptText = negative,
            description = description,
            variables = variables,
        )
        viewModelScope.launch { templateRepository.save(template) }
    }

    fun delete(id: String) = viewModelScope.launch { templateRepository.delete(id) }

    private fun blankTemplate() = PromptTemplate(
        id = Ids.newId(),
        name = "",
        category = "",
        promptText = "",
        negativePromptText = "",
        variables = emptyList(),
        description = "",
        useCount = 0,
        createdDate = System.currentTimeMillis(),
    )

    /** Discovers `{placeholder}` tokens and turns them into editable variables. */
    private fun extractVariables(text: String): List<TemplateVariable> =
        VARIABLE_PATTERN.findAll(text)
            .map { it.groupValues[1] }
            .distinct()
            .map { key -> TemplateVariable(key = key, label = key.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
            .toList()

    companion object {
        private val VARIABLE_PATTERN = Regex("\\{([a-zA-Z0-9_]+)}")
    }
}
