package com.narrative_service.emosdk.mapper

import com.narrative_service.emosdk.dto.PositionDto
import com.narrative_service.emosdk.dto.narrative.NarrativeNodeDto
import com.narrative_service.emosdk.entity.NarrativeNode
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NarrativeNodeMapper {

    fun toDto(
        node: NarrativeNode,
        childLayerId: UUID?
    ): NarrativeNodeDto {
        return NarrativeNodeDto(
            id = requireNotNull(node.id),
            layerId = requireNotNull(node.layerId),
            title = requireNotNull(node.title),
            content = requireNotNull(node.content),
            position = PositionDto(
                x = requireNotNull(node.positionX),
                y = requireNotNull(node.positionY)
            ),
            childLayerId = childLayerId
        )
    }
}