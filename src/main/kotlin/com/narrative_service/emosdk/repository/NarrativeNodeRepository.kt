package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.NarrativeNode
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NarrativeNodeRepository : JpaRepository<NarrativeNode, UUID> {
}