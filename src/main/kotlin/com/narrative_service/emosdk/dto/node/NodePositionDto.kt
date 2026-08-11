package com.narrative_service.emosdk.dto.narrative

import com.narrative_service.emosdk.dto.PositionDto
import java.util.UUID

data class NodePositionDto(
    val id: UUID,
    val position: PositionDto
)