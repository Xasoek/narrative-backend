package com.narrative_service.emosdk.search.document

import tools.jackson.databind.JsonNode
import java.util.UUID

data class NarrativeNodeDocument(
    val id: UUID? = null,
    val layerId: UUID? = null,
    val title: String? = null,
    val content: String? = null,
    val projectId: UUID? = null
)