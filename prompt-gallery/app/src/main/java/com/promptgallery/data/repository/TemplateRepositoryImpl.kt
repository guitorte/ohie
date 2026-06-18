package com.promptgallery.data.repository

import com.promptgallery.core.util.Ids
import com.promptgallery.core.util.IoDispatcher
import com.promptgallery.data.local.dao.PromptTemplateDao
import com.promptgallery.domain.model.PromptTemplate
import com.promptgallery.domain.model.TemplateVariable
import com.promptgallery.domain.repository.TemplateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: PromptTemplateDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : TemplateRepository {

    override fun observeAll(): Flow<List<PromptTemplate>> =
        templateDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeMostUsed(limit: Int): Flow<List<PromptTemplate>> =
        templateDao.observeMostUsed(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): PromptTemplate? = withContext(dispatcher) {
        templateDao.getById(id)?.toDomain()
    }

    override suspend fun save(template: PromptTemplate) = withContext(dispatcher) {
        templateDao.upsert(template.toEntity())
    }

    override suspend fun delete(id: String) = withContext(dispatcher) {
        templateDao.deleteById(id)
    }

    override suspend fun markUsed(id: String) = withContext(dispatcher) {
        templateDao.incrementUseCount(id)
    }

    override suspend fun ensureSeeded() = withContext(dispatcher) {
        if (templateDao.count() > 0) return@withContext
        templateDao.upsertAll(starterTemplates().map { it.toEntity() })
    }

    /** The curated starter library shipped on first launch. */
    private fun starterTemplates(): List<PromptTemplate> {
        val now = System.currentTimeMillis()
        fun template(
            name: String,
            category: String,
            prompt: String,
            negative: String,
            description: String,
            variables: List<TemplateVariable>,
        ) = PromptTemplate(
            id = Ids.newId(),
            name = name,
            category = category,
            promptText = prompt,
            negativePromptText = negative,
            variables = variables,
            description = description,
            useCount = 0,
            createdDate = now,
        )

        return listOf(
            template(
                name = "Portrait",
                category = "People",
                prompt = "professional portrait of {subject}, {expression}, {lighting} lighting, " +
                    "shot on 85mm lens, shallow depth of field, ultra detailed, photorealistic, 8k",
                negative = "deformed, blurry, extra fingers, low quality, watermark, text",
                description = "Studio-quality portrait of a single subject.",
                variables = listOf(
                    TemplateVariable("subject", "Subject", "a young woman"),
                    TemplateVariable("expression", "Expression", "calm confident expression"),
                    TemplateVariable(
                        "lighting", "Lighting", "soft window",
                        options = listOf("soft window", "dramatic rim", "golden hour", "studio softbox"),
                    ),
                ),
            ),
            template(
                name = "Anime",
                category = "Illustration",
                prompt = "{subject}, anime style, {mood} mood, cel shading, vibrant colors, " +
                    "detailed background, key visual, by studio {studio}",
                negative = "realistic, 3d, photo, lowres, bad anatomy, jpeg artifacts",
                description = "Stylised anime key-visual.",
                variables = listOf(
                    TemplateVariable("subject", "Subject", "a swordsman on a rooftop at dusk"),
                    TemplateVariable("mood", "Mood", "epic"),
                    TemplateVariable("studio", "Studio", "ghibli"),
                ),
            ),
            template(
                name = "Product Photography",
                category = "Commercial",
                prompt = "commercial product photo of {product}, on {surface}, {background} background, " +
                    "studio lighting, high detail, sharp focus, advertising photography, 8k",
                negative = "cluttered, low quality, blurry, distorted, watermark",
                description = "Clean e-commerce / advertising product shot.",
                variables = listOf(
                    TemplateVariable("product", "Product", "a glass perfume bottle"),
                    TemplateVariable("surface", "Surface", "polished marble"),
                    TemplateVariable(
                        "background", "Background", "soft gradient",
                        options = listOf("soft gradient", "pure white", "dark moody", "natural wood"),
                    ),
                ),
            ),
            template(
                name = "Concept Art",
                category = "Environments",
                prompt = "concept art of {scene}, {time_of_day}, cinematic composition, " +
                    "matte painting, dramatic lighting, highly detailed, trending on artstation",
                negative = "flat, boring, low detail, oversaturated, signature",
                description = "Cinematic environment concept art.",
                variables = listOf(
                    TemplateVariable("scene", "Scene", "a ruined floating city above the clouds"),
                    TemplateVariable(
                        "time_of_day", "Time of day", "sunset",
                        options = listOf("sunrise", "midday", "sunset", "night"),
                    ),
                ),
            ),
            template(
                name = "Photorealistic Landscape",
                category = "Environments",
                prompt = "breathtaking landscape of {location}, {weather}, golden hour, " +
                    "ultra wide angle, high dynamic range, photorealistic, national geographic, 8k",
                negative = "cartoon, painting, low quality, overexposed, blurry",
                description = "Natural landscape photography look.",
                variables = listOf(
                    TemplateVariable("location", "Location", "a misty mountain valley"),
                    TemplateVariable(
                        "weather", "Weather", "light fog",
                        options = listOf("clear sky", "light fog", "storm clouds", "fresh snow"),
                    ),
                ),
            ),
        )
    }
}
