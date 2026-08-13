package com.narrative_service.emosdk.mapper

import com.narrative_service.emosdk.entity.NarrativeNode
import com.narrative_service.emosdk.search.document.NarrativeNodeDocument
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jsonMapper
import java.util.UUID

@Component
class NarrativeNodeDocumentMapper(
    private val jsonMapper: JsonMapper
) {

    fun toDocument(
        node: NarrativeNode,
        projectId: UUID
    ): NarrativeNodeDocument {

        val contentJson = jsonMapper.readTree(requireNotNull(node.content))

        return NarrativeNodeDocument(
            id = node.id,
            layerId = node.layerId,
            title = node.title,
            content = contentJson["text"]?.asString() ?: "",
            projectId = projectId
        )
    }
}