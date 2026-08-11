package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.NarrativeNodeReference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
@Repository
interface NarrativeNodeReferenceRepository: JpaRepository<NarrativeNodeReference, UUID> {
    fun findAllBySourceNodeIdIn(
        sourceNodeIds: List<UUID>
    ): List<NarrativeNodeReference>

    fun deleteAllBySourceNodeId(sourceNodeId: UUID)

    fun deleteAllByTargetNodeId(targetNodeId: UUID)
}