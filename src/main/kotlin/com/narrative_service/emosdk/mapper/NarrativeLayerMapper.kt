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
                    excerpt = null,
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
}