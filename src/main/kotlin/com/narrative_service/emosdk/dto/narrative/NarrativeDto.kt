package com.narrative_service.emosdk.dto.narrative

import java.util.UUID

data class NarrativeDto(
    val id: UUID,
    val projectId: UUID,
    val rootLayerId: UUID
)