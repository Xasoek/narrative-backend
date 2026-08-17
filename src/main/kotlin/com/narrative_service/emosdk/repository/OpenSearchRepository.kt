package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.NarrativeNode
import com.narrative_service.emosdk.mapper.NarrativeNodeDocumentMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.stereotype.Repository
import com.narrative_service.emosdk.search.document.NarrativeNodeDocument
import java.util.UUID

@Repository
class OpenSearchRepository(
    private final val openSearchClient: OpenSearchClient,
    private final val openSearchMapper: NarrativeNodeDocumentMapper,
) {
    fun index(node: NarrativeNode, projectId: UUID) {
        val document = openSearchMapper.toDocument(
            node = node,
            projectId = projectId
        )

        openSearchClient.index {request ->
            request.index("narrative-node-$projectId")
                .id(node.id.toString())
                .document(document)
        }
    }


    fun delete(nodeId: UUID, projectId: UUID) {
        openSearchClient.delete { request ->
            request.index("narrative-node-$projectId")
                .id(nodeId.toString())
        }
    }

    fun search(
        query: String,
        projectId: UUID,
        size: Int,
        page: Int
    ) = openSearchClient.search(
        { request ->
            request
                .index("narrative-node-$projectId")
                .size(size)
                .from(page * size)
                .query { q ->
                    q.multiMatch { multiMatch ->
                        multiMatch
                            .query(query)
                            .fields("title", "content")
                    }
                }
        },
        NarrativeNodeDocument::class.java
    )
}