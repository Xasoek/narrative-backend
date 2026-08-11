package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.Narrative
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NarrativeRepository : JpaRepository<Narrative, UUID> {
    fun findByProjectId(projectId: UUID): Narrative?
}