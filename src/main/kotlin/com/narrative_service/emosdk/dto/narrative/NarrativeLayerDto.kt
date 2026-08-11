package com.narrative_service.emosdk.dto.narrative

import com.narrative_service.emosdk.dto.BreadcrumbDto
import com.narrative_service.emosdk.dto.LayerNodeDto
import com.narrative_service.emosdk.dto.NodeReferenceDto
import java.util.UUID

data class NarrativeLayerDto(
    val id: UUID,
    val parentNodeId: UUID?,
    val breadcrumbs: List<BreadcrumbDto>,
    val nodes: List<LayerNodeDto>,
    val references: List<NodeReferenceDto>
)