package com.narrative_service.emosdk.service

import com.narrative_service.emosdk.dto.NarrativeSearchItemDto
import com.narrative_service.emosdk.dto.NarrativeSearchResponse
import com.narrative_service.emosdk.entity.NarrativeNode
import com.narrative_service.emosdk.mapper.NarrativeNodeDocumentMapper
import com.narrative_service.emosdk.repository.NarrativeNodeRepository
import com.narrative_service.emosdk.search.document.NarrativeNodeDocument
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class NarrativeNodeSearchService(
    private val openSearchClient: OpenSearchClient,
    private val narrativeNodeDocumentMapper: NarrativeNodeDocumentMapper,
    private val narrativeNodeRepository: NarrativeNodeRepository,
    private val jsonMapper: JsonMapper
) {

    fun index(
        node: NarrativeNode,
        projectId: UUID
    ) {
        val document = narrativeNodeDocumentMapper.toDocument(
            node = node,
            projectId = projectId
        )

        openSearchClient.index { request ->
            request
                .index("narrative-nodes")
                .id(node.id.toString())
                .document(document)
        }
    }

    fun search(
        projectId: UUID,
        query: String,
        page: Int,
        size: Int
    ): NarrativeSearchResponse {

        val response = openSearchClient.search(
            { request ->
                request
                    .index("narrative-nodes")
                    .from(page * size)
                    .size(size)
                    .query { q ->
                        q.bool { bool ->
                            bool
                                .must { must ->
                                    must.multiMatch { multiMatch ->
                                        multiMatch
                                            .query(query)
                                            .fields("title", "content")
                                    }
                                }
                                .filter { filter ->
                                    filter.term { term ->
                                        term
                                            .field("projectId.keyword")
                                            .value {
                                                it.stringValue(projectId.toString())
                                            }
                                    }
                                }
                        }
                    }
            },
            NarrativeNodeDocument::class.java
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

            val nodeId = document.id
                ?: return@mapNotNull null

            val node = narrativeNodeRepository
                .findById(nodeId)
                .orElse(null)
                ?: return@mapNotNull null

            NarrativeSearchItemDto(
                id = requireNotNull(node.id),
                layerId = requireNotNull(node.layerId),
                title = requireNotNull(node.title),
                content = jsonMapper.readTree(
                    requireNotNull(node.content)
                )
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

    fun delete(nodeId: UUID) {
        openSearchClient.delete { request ->
            request
                .index("narrative-nodes")
                .id(nodeId.toString())
        }
    }
}