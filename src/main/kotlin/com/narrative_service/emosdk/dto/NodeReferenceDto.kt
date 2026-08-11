package com.narrative_service.emosdk.dto

import java.util.UUID

data class NodeReferenceDto(
    val sourceNodeId: UUID,
    val targetNodeId: UUID
)