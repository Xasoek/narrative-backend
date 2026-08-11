package com.narrative_service.emosdk.dto.narrative

import com.narrative_service.emosdk.dto.PositionDto
import java.util.UUID

data class NarrativeNodeDto(
    val id: UUID,
    val layerId: UUID,
    val title: String,
    val content: String,
    val position: PositionDto,
    val childLayerId: UUID?
)