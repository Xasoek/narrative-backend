package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.NarrativeLayer
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NarrativeLayerRepository : JpaRepository<NarrativeLayer, UUID> {
    fun findAllByNarrativeId(narrativeId: UUID): List<NarrativeLayer>
    fun findAllByParentNodeIdIn(
        parentNodeIds: List<UUID>
    ): List<NarrativeLayer>

    fun findByParentNodeId(parentNodeId: UUID): NarrativeLayer?
    fun findByIdAndNarrativeId(
        id: UUID,
        narrativeId: UUID
    ): NarrativeLayer?
}