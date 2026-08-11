package com.narrative_service.emosdk.mapper

import com.narrative_service.emosdk.dto.narrative.NarrativeTreeDto
import com.narrative_service.emosdk.dto.narrative.NarrativeTreeItemDto
import com.narrative_service.emosdk.entity.NarrativeNode
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NarrativeTreeMapper {

    fun toDto(
        nodes: List<NarrativeNode>,
        parentNodeIds: Map<UUID, UUID?>,
        childLayerIds: Map<UUID, UUID>
    ): NarrativeTreeDto {

        val items = nodes.map { node ->
            NarrativeTreeItemDto(
                id = requireNotNull(node.id),
                title = requireNotNull(node.title),
                layerId = requireNotNull(node.layerId),
                parentNodeId = parentNodeIds[node.id],
                childLayerId = childLayerIds[node.id]
            )
        }

        return NarrativeTreeDto(
            items = items
        )
    }
}