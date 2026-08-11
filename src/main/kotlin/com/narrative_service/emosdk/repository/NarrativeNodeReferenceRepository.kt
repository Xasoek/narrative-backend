package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.NarrativeNodeReference
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NarrativeNodeReferenceRepository: JpaRepository<NarrativeNodeReference, UUID> {

}