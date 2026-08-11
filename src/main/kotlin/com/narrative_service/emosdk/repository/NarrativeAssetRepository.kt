package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.NarrativeAsset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NarrativeAssetRepository : JpaRepository<NarrativeAsset, UUID> {
}
