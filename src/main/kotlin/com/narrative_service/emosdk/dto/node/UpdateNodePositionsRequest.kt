package com.narrative_service.emosdk.dto.node

import com.narrative_service.emosdk.dto.narrative.NodePositionDto

data class UpdateNodePositionsRequest(
    val nodes: List<NodePositionDto>
)