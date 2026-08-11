package com.narrative_service.emosdk.dto

import java.util.UUID

data class NarrativeAssetDto(
    val id: UUID,
    val url: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long
)