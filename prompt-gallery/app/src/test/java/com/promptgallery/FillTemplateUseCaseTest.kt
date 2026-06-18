package com.promptgallery

import com.promptgallery.domain.model.PromptTemplate
import com.promptgallery.domain.model.TemplateVariable
import com.promptgallery.domain.usecase.FillTemplateUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class FillTemplateUseCaseTest {

    private val useCase = FillTemplateUseCase()

    private fun template() = PromptTemplate(
        id = "t1",
        name = "Portrait",
        category = "People",
        promptText = "portrait of {subject}, {lighting} lighting",
        negativePromptText = "blurry, {avoid}",
        variables = listOf(
            TemplateVariable("subject", "Subject", "a woman"),
            TemplateVariable("lighting", "Lighting", "soft"),
            TemplateVariable("avoid", "Avoid", "deformed"),
        ),
        description = "",
        useCount = 0,
        createdDate = 0,
    )

    @Test
    fun fillsWithProvidedValues() {
        val result = useCase(template(), mapOf("subject" to "a cat", "lighting" to "dramatic"))
        assertEquals("portrait of a cat, dramatic lighting", result.prompt)
        assertEquals("blurry, deformed", result.negativePrompt)
    }

    @Test
    fun fallsBackToDefaults() {
        val result = useCase(template(), emptyMap())
        assertEquals("portrait of a woman, soft lighting", result.prompt)
    }

    @Test
    fun blankValueUsesDefault() {
        val result = useCase(template(), mapOf("subject" to "  "))
        assertEquals("portrait of a woman, soft lighting", result.prompt)
    }
}
