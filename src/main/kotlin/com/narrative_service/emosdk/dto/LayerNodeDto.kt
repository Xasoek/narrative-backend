package com.narrative_service.emosdk.dto

import java.util.UUID

data class LayerNodeDto(
    val id: UUID,
    val title: String,
    val excerpt: String?,
    val position: PositionDto,
    val childLayerId: UUID?
)