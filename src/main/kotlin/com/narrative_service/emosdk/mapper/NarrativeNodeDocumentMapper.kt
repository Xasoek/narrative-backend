package com.narrative_service.emosdk.mapper

import com.narrative_service.emosdk.entity.NarrativeNode
import com.narrative_service.emosdk.search.document.NarrativeNodeDocument
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
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
        val contentText = extractText(contentJson)

        return NarrativeNodeDocument(
            id = node.id,
            layerId = node.layerId,
            title = node.title,
            content = contentText,
            projectId = projectId
        )
    }

    private fun extractText(node: JsonNode): String {
        val texts = mutableListOf<String>()

        fun walk(current: JsonNode) {
            if (current.isObject) {
                current["text"]?.let { textNode ->
                    if (textNode.isString) {
                        texts.add(textNode.asString())
                    }
                }

                current.properties().forEach { (_, value) ->
                    walk(value)
                }
            } else if (current.isArray) {
                current.forEach { child ->
                    walk(child)
                }
            }
        }

        walk(node)

        return texts.joinToString(" ")
    }
}