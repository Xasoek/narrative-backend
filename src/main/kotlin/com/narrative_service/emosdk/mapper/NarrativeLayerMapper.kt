package com.narrative_service.emosdk.mapper

import com.narrative_service.emosdk.dto.BreadcrumbDto
import com.narrative_service.emosdk.dto.LayerNodeDto
import com.narrative_service.emosdk.dto.NodeReferenceDto
import com.narrative_service.emosdk.dto.PositionDto
import com.narrative_service.emosdk.dto.narrative.NarrativeLayerDto
import com.narrative_service.emosdk.entity.NarrativeLayer
import com.narrative_service.emosdk.entity.NarrativeNode
import com.narrative_service.emosdk.entity.NarrativeNodeReference
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import java.util.UUID

@Component
class NarrativeLayerMapper {

    fun toDto(
        layer: NarrativeLayer,
        nodes: List<NarrativeNode>,
        references: List<NarrativeNodeReference>,
        breadcrumbs: List<BreadcrumbDto>,
        childLayerIds: Map<UUID, UUID>
    ): NarrativeLayerDto {

        return NarrativeLayerDto(
            id = requireNotNull(layer.id),
            parentNodeId = layer.parentNodeId,

            breadcrumbs = breadcrumbs,

            nodes = nodes.map { node ->
                LayerNodeDto(
                    id = requireNotNull(node.id),
                    title = requireNotNull(node.title),
                    excerpt = extractExcerpt(node.content),
                    position = PositionDto(
                        x = requireNotNull(node.positionX),
                        y = requireNotNull(node.positionY)
                    ),
                    childLayerId = childLayerIds[node.id]
                )
            },

            references = references.map { reference ->
                NodeReferenceDto(
                    sourceNodeId = requireNotNull(reference.sourceNodeId),
                    targetNodeId = requireNotNull(reference.targetNodeId)
                )
            }
        )
    }

    private fun extractExcerpt(content: JsonNode?): String? {
        val textParts = mutableListOf<String>()
        collectText(content, textParts)

        val normalized = textParts
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.isBlank()) {
            return null
        }

        return normalized.take(EXCERPT_MAX_LENGTH)
    }

    private fun collectText(
        node: JsonNode?,
        textParts: MutableList<String>
    ) {
        if (node == null || node.isNull) {
            return
        }

        if (node.isObject) {
            val textNode = node.get("text")
            if (textNode != null && textNode.isString) {
                textParts.add(textNode.stringValue())
            }

            collectText(node.get("content"), textParts)
            return
        }

        if (node.isArray) {
            for (child in node) {
                collectText(child, textParts)
            }
        }
    }

    private companion object {
        const val EXCERPT_MAX_LENGTH = 160
    }
}
