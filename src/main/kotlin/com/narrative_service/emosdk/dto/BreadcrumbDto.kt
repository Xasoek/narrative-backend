package com.narrative_service.emosdk.dto

import java.util.UUID

data class BreadcrumbDto(
    val layerId: UUID,
    val nodeId: UUID?,
    val title: String
)