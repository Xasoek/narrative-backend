package com.narrative_service.emosdk.service

import com.narrative_service.emosdk.dto.NarrativeSearchItemDto
import com.narrative_service.emosdk.dto.NarrativeSearchResponse
import com.narrative_service.emosdk.repository.OpenSearchRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NarrativeNodeSearchService(
    private val openSearchRepository: OpenSearchRepository,
) {

    fun search(
        projectId: UUID,
        query: String,
        page: Int,
        size: Int
    ): NarrativeSearchResponse {

        val response = openSearchRepository.search(
            query = query,
            projectId = projectId,
            size = size,
            page = page
        )

        val totalElements = response.hits()
            .total()
            ?.value()
            ?: 0L

        val totalPages =
            if (size <= 0) {
                0
            } else {
                ((totalElements + size - 1) / size).toInt()
            }

        val documents = response.hits()
            .hits()
            .mapNotNull { it.source() }

        val items = documents.mapNotNull { document ->

            NarrativeSearchItemDto(
                id = document.id ?: return@mapNotNull null,
                layerId = document.layerId ?: return@mapNotNull null,
                title = document.title ?: return@mapNotNull null,
                content = document.content ?: ""
            )
        }

        return NarrativeSearchResponse(
            query = query,
            totalElements = totalElements,
            totalPages = totalPages,
            page = page,
            size = size,
            items = items
        )
    }
}