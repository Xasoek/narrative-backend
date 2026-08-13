package com.narrative_service.emosdk.service

import com.narrative_service.emosdk.dto.BreadcrumbDto
import com.narrative_service.emosdk.dto.ChildLayerDto
import com.narrative_service.emosdk.dto.narrative.CreateNarrativeNodeRequest
import com.narrative_service.emosdk.dto.narrative.NarrativeDto
import com.narrative_service.emosdk.dto.narrative.NarrativeLayerDto
import com.narrative_service.emosdk.dto.narrative.NarrativeNodeDto
import com.narrative_service.emosdk.dto.narrative.NarrativeTreeDto
import com.narrative_service.emosdk.dto.narrative.UpdateNarrativeNodeRequest
import com.narrative_service.emosdk.dto.node.UpdateNodePositionsRequest
import com.narrative_service.emosdk.entity.Narrative
import com.narrative_service.emosdk.entity.NarrativeLayer
import com.narrative_service.emosdk.entity.NarrativeNode
import com.narrative_service.emosdk.entity.NarrativeNodeReference
import com.narrative_service.emosdk.exception.NarrativeLayerNotFoundException
import com.narrative_service.emosdk.exception.NarrativeNodeNotFoundException
import com.narrative_service.emosdk.exception.NarrativeNotFoundException
import com.narrative_service.emosdk.mapper.NarrativeLayerMapper
import com.narrative_service.emosdk.mapper.NarrativeMapper
import com.narrative_service.emosdk.mapper.NarrativeNodeMapper
import com.narrative_service.emosdk.mapper.NarrativeTreeMapper
import com.narrative_service.emosdk.repository.NarrativeLayerRepository
import com.narrative_service.emosdk.repository.NarrativeNodeReferenceRepository
import com.narrative_service.emosdk.repository.NarrativeNodeRepository
import com.narrative_service.emosdk.repository.NarrativeRepository
import com.narrative_service.emosdk.repository.ProjectRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NarrativeService(
    private val narrativeNodeSearchService: NarrativeNodeSearchService,
    private val narrativeRepository: NarrativeRepository,
    private val narrativeMapper: NarrativeMapper,
    private val narrativeLayerRepository: NarrativeLayerRepository,
    private val narrativeNodeRepository: NarrativeNodeRepository,
    private val narrativeTreeMapper: NarrativeTreeMapper,
    private val narrativeNodeReferenceRepository: NarrativeNodeReferenceRepository,
    private val narrativeLayerMapper: NarrativeLayerMapper,
    private val narrativeNodeMapper: NarrativeNodeMapper,
    private val projectRepository: ProjectRepository
) {
    fun getNarrativeByProjectId(projectId: UUID): NarrativeDto {
        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)
        return narrativeMapper.toDto(narrative)
    }

    fun getTree(projectId: UUID): NarrativeTreeDto {
        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        val layers = narrativeLayerRepository.findAllByNarrativeId(narrativeId)

        val layerIds = layers.map { requireNotNull(it.id) }

        val nodes = if (layerIds.isEmpty()) {
            emptyList()
        } else {
            narrativeNodeRepository.findAllByLayerIdInOrderByCreatedAtAscIdAsc(layerIds)
        }

        // layerId -> parentNodeId
        val layerParentNodeIds = layers.associate {
            requireNotNull(it.id) to it.parentNodeId
        }

        // nodeId -> parentNodeId
        val parentNodeIds = nodes.associate { node ->
            requireNotNull(node.id) to layerParentNodeIds[node.layerId]
        }

        // parentNodeId -> childLayerId
        val childLayerIds = layers
            .filter { it.parentNodeId != null }
            .associate {
                requireNotNull(it.parentNodeId) to requireNotNull(it.id)
            }

        return narrativeTreeMapper.toDto(
            nodes = nodes,
            parentNodeIds = parentNodeIds,
            childLayerIds = childLayerIds
        )
    }

    fun getLayer(projectId: UUID, layerId: UUID): NarrativeLayerDto {
        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        val layer = narrativeLayerRepository
            .findByIdAndNarrativeId(layerId, narrativeId)
            ?: throw NarrativeLayerNotFoundException(layerId)

        val nodes = narrativeNodeRepository
            .findAllByLayerIdOrderByCreatedAtAscIdAsc(layerId)

        val nodeIds = nodes.map { requireNotNull(it.id) }

        val references = if (nodeIds.isEmpty()) {
            emptyList()
        } else {
            narrativeNodeReferenceRepository.findAllBySourceNodeIdIn(nodeIds)
        }

        val childLayers = if (nodeIds.isEmpty()) {
            emptyList()
        } else {
            narrativeLayerRepository.findAllByParentNodeIdIn(nodeIds)
        }

        val childLayerIds = childLayers.associate {
            requireNotNull(it.parentNodeId) to requireNotNull(it.id)
        }

        return narrativeLayerMapper.toDto(
            layer = layer,
            nodes = nodes,
            references = references,
            breadcrumbs = buildBreadcrumbs(narrative, layer),
            childLayerIds = childLayerIds
        )
    }

    fun getNode(projectId: UUID, nodeId: UUID): NarrativeNodeDto {
        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        val node = narrativeNodeRepository
            .findByIdAndNarrativeId(nodeId, narrativeId)
            ?: throw NarrativeNodeNotFoundException(nodeId)

        val childLayer = narrativeLayerRepository.findByParentNodeId(nodeId)

        return narrativeNodeMapper.toDto(
            node = node,
            childLayerId = childLayer?.id
        )
    }

    @Transactional
    fun createNode(
        projectId: UUID,
        request: CreateNarrativeNodeRequest
    ): NarrativeNodeDto {

        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        narrativeLayerRepository.findByIdAndNarrativeId(request.layerId, narrativeId)
            ?: throw NarrativeLayerNotFoundException(request.layerId)

        val linkedNodeIds = request.linkedNodeIds.distinct()
        validateLinkedNodeTargets(narrativeId, linkedNodeIds)

        val node = NarrativeNode(
            layerId = request.layerId,
            title = request.title,
            content = request.content.toString(),
            positionX = request.position.x,
            positionY = request.position.y
        )

        val savedNode = narrativeNodeRepository.save(node)

        narrativeNodeSearchService.index(
            node = savedNode,
            projectId = projectId
        )

        val nodeId = requireNotNull(savedNode.id)

        val references = linkedNodeIds.map { targetNodeId ->
            NarrativeNodeReference(
                sourceNodeId = nodeId,
                targetNodeId = targetNodeId
            )
        }

        if (references.isNotEmpty()) {
            narrativeNodeReferenceRepository.saveAll(references)
        }

        return narrativeNodeMapper.toDto(
            node = savedNode,
            childLayerId = null
        )
    }

    @Transactional
    fun updateNode(
        projectId: UUID,
        nodeId: UUID,
        request: UpdateNarrativeNodeRequest
    ): NarrativeNodeDto {

        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        val node = narrativeNodeRepository
            .findByIdAndNarrativeId(nodeId, narrativeId)
            ?: throw NarrativeNodeNotFoundException(nodeId)

        request.title?.let {
            node.title = it
        }

        if (request.content != null) {
            val linkedNodeIds = request.linkedNodeIds
                ?: throw IllegalArgumentException(
                    "linkedNodeIds is required when content is provided"
                )
            val distinctLinkedNodeIds = linkedNodeIds.distinct()

            if (distinctLinkedNodeIds.contains(nodeId)) {
                throw IllegalArgumentException("A node cannot reference itself")
            }

            validateLinkedNodeTargets(narrativeId, distinctLinkedNodeIds)

            node.content = request.content.toString()

            narrativeNodeReferenceRepository
                .deleteAllBySourceNodeId(nodeId)

            val references = distinctLinkedNodeIds.map { targetNodeId ->
                NarrativeNodeReference(
                    sourceNodeId = nodeId,
                    targetNodeId = targetNodeId
                )
            }

            if (references.isNotEmpty()) {
                narrativeNodeReferenceRepository.saveAll(references)
            }
        }

        val savedNode = narrativeNodeRepository.save(node)

        narrativeNodeSearchService.index(
            node = savedNode,
            projectId = projectId
        )

        val childLayer = narrativeLayerRepository.findByParentNodeId(nodeId)

        return narrativeNodeMapper.toDto(
            node = savedNode,
            childLayerId = childLayer?.id
        )
    }

    @Transactional
    fun updateNodePositions(
        projectId: UUID,
        request: UpdateNodePositionsRequest
    ) {
        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        request.nodes.forEach { nodePosition ->
            val node = narrativeNodeRepository
                .findByIdAndNarrativeId(nodePosition.id, narrativeId)
                ?: throw NarrativeNodeNotFoundException(nodePosition.id)

            node.positionX = nodePosition.position.x
            node.positionY = nodePosition.position.y

            narrativeNodeRepository.save(node)
        }
    }

    @Transactional
    fun createOrGetChildLayer(
        projectId: UUID,
        nodeId: UUID
    ): ChildLayerDto {

        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        val node = narrativeNodeRepository
            .findByIdAndNarrativeId(nodeId, narrativeId)
            ?: throw NarrativeNodeNotFoundException(nodeId)

        val existingLayer = narrativeLayerRepository.findByParentNodeId(nodeId)

        if (existingLayer != null) {
            return ChildLayerDto(
                id = requireNotNull(existingLayer.id),
                parentNodeId = nodeId
            )
        }

        val childLayer = NarrativeLayer(
            narrativeId = requireNotNull(narrative.id),
            parentNodeId = requireNotNull(node.id)
        )

        val savedLayer = narrativeLayerRepository.save(childLayer)

        return ChildLayerDto(
            id = requireNotNull(savedLayer.id),
            parentNodeId = nodeId
        )
    }

    @Transactional
    fun deleteNode(
        projectId: UUID,
        nodeId: UUID
    ) {
        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        val narrativeId = requireNotNull(narrative.id)

        val node = narrativeNodeRepository
            .findByIdAndNarrativeId(nodeId, narrativeId)
            ?: throw NarrativeNodeNotFoundException(nodeId)


        deleteNodeRecursively(node)
    }

    private fun deleteNodeRecursively(node: NarrativeNode) {
        val nodeId = requireNotNull(node.id)

        val childLayer = narrativeLayerRepository.findByParentNodeId(nodeId)

        if (childLayer != null) {
            val childLayerId = requireNotNull(childLayer.id)

            val childNodes = narrativeNodeRepository.findAllByLayerId(childLayerId)

            childNodes.forEach { childNode ->
                deleteNodeRecursively(childNode)
            }

            narrativeLayerRepository.delete(childLayer)
        }

        narrativeNodeReferenceRepository.deleteAllBySourceNodeId(nodeId)
        narrativeNodeReferenceRepository.deleteAllByTargetNodeId(nodeId)

        narrativeNodeRepository.delete(node)
        narrativeNodeSearchService.delete(nodeId)
    }

    private fun buildBreadcrumbs(
        narrative: Narrative,
        layer: NarrativeLayer
    ): List<BreadcrumbDto> {

        val narrativeId = requireNotNull(narrative.id)
        val projectId = requireNotNull(narrative.projectId)
        val rootLayerId = requireNotNull(narrative.rootLayerId)
        val projectTitle = projectRepository.findById(projectId)
            .map { project ->
                project.name?.takeIf { it.isNotBlank() } ?: "Проект"
            }
            .orElse("Проект")

        val breadcrumbs = mutableListOf(
            BreadcrumbDto(
                layerId = rootLayerId,
                nodeId = null,
                title = projectTitle
            )
        )

        val childBreadcrumbs = mutableListOf<BreadcrumbDto>()

        var currentLayer = layer

        while (currentLayer.parentNodeId != null) {
            val currentLayerId = requireNotNull(currentLayer.id)
            val parentNodeId = requireNotNull(currentLayer.parentNodeId)

            val parentNode = narrativeNodeRepository
                .findByIdAndNarrativeId(parentNodeId, narrativeId)
                ?: throw NarrativeNodeNotFoundException(parentNodeId)

            childBreadcrumbs.add(
                BreadcrumbDto(
                    layerId = currentLayerId,
                    nodeId = requireNotNull(parentNode.id),
                    title = requireNotNull(parentNode.title)
                )
            )

            currentLayer = narrativeLayerRepository
                .findByIdAndNarrativeId(requireNotNull(parentNode.layerId), narrativeId)
                ?: throw NarrativeLayerNotFoundException(requireNotNull(parentNode.layerId))
        }

        breadcrumbs.addAll(childBreadcrumbs.reversed())

        return breadcrumbs
    }

    private fun validateLinkedNodeTargets(
        narrativeId: UUID,
        linkedNodeIds: List<UUID>
    ) {
        if (linkedNodeIds.isEmpty()) {
            return
        }

        val existingNodeIds = narrativeNodeRepository
            .findAllByIdInAndNarrativeId(linkedNodeIds, narrativeId)
            .map { requireNotNull(it.id) }
            .toSet()
        val missingNodeIds = linkedNodeIds.toSet() - existingNodeIds

        if (missingNodeIds.isNotEmpty()) {
            throw IllegalArgumentException(
                "linkedNodeIds contains nodes outside this narrative or missing nodes: $missingNodeIds"
            )
        }
    }
}
