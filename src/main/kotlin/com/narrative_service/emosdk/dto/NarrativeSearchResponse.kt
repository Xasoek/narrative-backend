package com.narrative_service.emosdk.dto

import com.narrative_service.emosdk.search.document.NarrativeNodeDocument

data class NarrativeSearchResponse(
    val query: String,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val items: List<NarrativeSearchItemDto>
)
