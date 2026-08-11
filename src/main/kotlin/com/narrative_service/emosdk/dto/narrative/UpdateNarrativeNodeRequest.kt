package com.narrative_service.emosdk.dto.narrative

import tools.jackson.databind.JsonNode
import java.util.UUID

data class UpdateNarrativeNodeRequest(
    val title: String?,
    val content: JsonNode?,
    val linkedNodeIds: List<UUID>?
)