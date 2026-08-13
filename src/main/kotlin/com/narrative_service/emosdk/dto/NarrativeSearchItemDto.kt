package com.narrative_service.emosdk.dto

import tools.jackson.databind.JsonNode
import java.util.UUID

data class NarrativeSearchItemDto (
    val id: UUID,
    val layerId: UUID,
    val title: String,
    val content: JsonNode
    )