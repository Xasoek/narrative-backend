package com.narrative_service.emosdk.mapper

import com.narrative_service.emosdk.entity.NarrativeLayer
import com.narrative_service.emosdk.entity.NarrativeNode
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals

class NarrativeLayerMapperTest {

    private val objectMapper = ObjectMapper()
    private val mapper = NarrativeLayerMapper()

    @Test
    fun `toDto builds excerpt from Tiptap text content`() {
        val layerId = UUID.randomUUID()
        val nodeId = UUID.randomUUID()

        val result = mapper.toDto(
            layer = NarrativeLayer(
                id = layerId,
                narrativeId = UUID.randomUUID()
            ),
            nodes = listOf(
                NarrativeNode(
                    id = nodeId,
                    layerId = layerId,
                    title = "Герой",
                    content = objectMapper.readTree(
                        """
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [
                                { "type": "text", "text": "Краткое" },
                                { "type": "text", "text": "описание" }
                              ]
                            }
                          ]
                        }
                        """.trimIndent()
                    ),
                    positionX = 100.0,
                    positionY = 200.0
                )
            ),
            references = emptyList(),
            breadcrumbs = emptyList(),
            childLayerIds = emptyMap()
        )

        assertEquals("Краткое описание", result.nodes.single().excerpt)
    }

    @Test
    fun `toDto returns empty excerpt when content is missing`() {
        val layerId = UUID.randomUUID()

        val result = mapper.toDto(
            layer = NarrativeLayer(
                id = layerId,
                narrativeId = UUID.randomUUID()
            ),
            nodes = listOf(
                NarrativeNode(
                    id = UUID.randomUUID(),
                    layerId = layerId,
                    title = "Новая карточка",
                    content = null,
                    positionX = 760.0,
                    positionY = 220.0
                )
            ),
            references = emptyList(),
            breadcrumbs = emptyList(),
            childLayerIds = emptyMap()
        )

        assertEquals("", result.nodes.single().excerpt)
    }
}
