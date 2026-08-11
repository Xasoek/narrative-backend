package com.narrative_service.emosdk.dto.narrative

import com.narrative_service.emosdk.dto.PositionDto
import tools.jackson.databind.JsonNode
import java.util.UUID

data class NarrativeNodeDto(
    val id: UUID,
    val layerId: UUID,
    val title: String,
    val content: JsonNode,
    val position: PositionDto,
    val childLayerId: UUID?
)