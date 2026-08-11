package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.NarrativeNode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface NarrativeNodeRepository : JpaRepository<NarrativeNode, UUID> {

    fun findAllByLayerIdInOrderByCreatedAtAscIdAsc(
        layerIds: List<UUID>
    ): List<NarrativeNode>

    fun findAllByLayerIdOrderByCreatedAtAscIdAsc(
        layerId: UUID
    ): List<NarrativeNode>

    fun findAllByLayerId(layerId: UUID): List<NarrativeNode>

    @Query(
        """
    SELECT n
    FROM NarrativeNode n
    JOIN NarrativeLayer l ON l.id = n.layerId
    WHERE n.id IN :nodeIds
      AND l.narrativeId = :narrativeId
    """
    )
    fun findAllByIdInAndNarrativeId(
        @Param("nodeIds") nodeIds: Collection<UUID>,
        @Param("narrativeId") narrativeId: UUID
    ): List<NarrativeNode>

    @Query(
        """
    SELECT n
    FROM NarrativeNode n
    JOIN NarrativeLayer l ON l.id = n.layerId
    WHERE n.id = :nodeId
      AND l.narrativeId = :narrativeId
    """
    )
    fun findByIdAndNarrativeId(
        @Param("nodeId") nodeId: UUID,
        @Param("narrativeId") narrativeId: UUID
    ): NarrativeNode?
}
