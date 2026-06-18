package com.promptgallery.domain.usecase

import com.promptgallery.domain.model.PromptTemplate
import javax.inject.Inject

/** The two prompt strings produced by filling a template. */
data class FilledPrompt(val prompt: String, val negativePrompt: String)

/**
 * Substitutes `{key}` placeholders in a template with user-supplied values,
 * falling back to each variable's default when a value is missing. Unfilled
 * placeholders are left intact so the user can spot what still needs input.
 */
class FillTemplateUseCase @Inject constructor() {

    operator fun invoke(template: PromptTemplate, values: Map<String, String>): FilledPrompt {
        val resolved = template.variables.associate { variable ->
            variable.key to (values[variable.key]?.takeIf { it.isNotBlank() } ?: variable.defaultValue)
        }
        return FilledPrompt(
            prompt = substitute(template.promptText, resolved),
            negativePrompt = substitute(template.negativePromptText, resolved),
        )
    }

    private fun substitute(text: String, values: Map<String, String>): String {
        var result = text
        values.forEach { (key, value) ->
            if (value.isNotEmpty()) result = result.replace("{$key}", value)
        }
        return result
    }
}
