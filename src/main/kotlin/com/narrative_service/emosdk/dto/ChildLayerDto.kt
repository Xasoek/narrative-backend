package com.narrative_service.emosdk.dto

import java.util.UUID

data class ChildLayerDto(
    val id: UUID,
    val parentNodeId: UUID
)