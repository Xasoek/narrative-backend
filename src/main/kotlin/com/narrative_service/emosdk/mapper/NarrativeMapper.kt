package com.narrative_service.emosdk.mapper

import com.narrative_service.emosdk.dto.narrative.NarrativeDto
import com.narrative_service.emosdk.entity.Narrative
import org.springframework.stereotype.Component

@Component
class NarrativeMapper {

    fun toDto(narrative: Narrative): NarrativeDto {
        return NarrativeDto(
            id = requireNotNull(narrative.id),
            projectId = requireNotNull(narrative.projectId),
            rootLayerId = requireNotNull(narrative.rootLayerId)
        )
    }
}