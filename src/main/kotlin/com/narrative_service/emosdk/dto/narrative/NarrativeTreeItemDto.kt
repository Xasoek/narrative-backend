package com.narrative_service.emosdk.dto.narrative

import java.util.UUID

data class NarrativeTreeItemDto(
    var id: UUID,
    val title: String,
    val layerId: UUID,
    val parentNodeId: UUID? = null,
    val childLayerId: UUID? = null,
)