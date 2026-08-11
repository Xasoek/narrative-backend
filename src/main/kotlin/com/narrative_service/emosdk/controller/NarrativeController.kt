package com.narrative_service.emosdk.controller

import com.narrative_service.emosdk.dto.ChildLayerDto
import com.narrative_service.emosdk.dto.NarrativeAssetDto
import com.narrative_service.emosdk.dto.narrative.CreateNarrativeNodeRequest
import com.narrative_service.emosdk.dto.narrative.NarrativeDto
import com.narrative_service.emosdk.dto.narrative.NarrativeLayerDto
import com.narrative_service.emosdk.dto.narrative.NarrativeNodeDto
import com.narrative_service.emosdk.dto.narrative.NarrativeTreeDto
import com.narrative_service.emosdk.dto.narrative.UpdateNarrativeNodeRequest
import com.narrative_service.emosdk.dto.node.UpdateNodePositionsRequest
import com.narrative_service.emosdk.service.NarrativeAssetService
import com.narrative_service.emosdk.service.NarrativeService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/projects/{projectId}/narrative")
class NarrativeController(
    private val narrativeService: NarrativeService,
    private val narrativeAssetService: NarrativeAssetService
) {
    @GetMapping
    fun getNarrative(
        @PathVariable projectId: UUID
    ): NarrativeDto {
        return narrativeService.getNarrativeByProjectId(projectId)
    }

    @GetMapping("/tree")
    fun getNarrativeTree(@PathVariable projectId: UUID): NarrativeTreeDto {
        return narrativeService.getTree(projectId)
    }

    @GetMapping("/layers/{layerId}")
    fun getLayer(
        @PathVariable projectId: UUID,
        @PathVariable layerId: UUID
    ): NarrativeLayerDto {
        return narrativeService.getLayer(projectId, layerId)
    }

    @GetMapping("/nodes/{nodeId}")
    fun getNode(
        @PathVariable projectId: UUID,
        @PathVariable nodeId: UUID
    ): NarrativeNodeDto {
        return narrativeService.getNode(projectId, nodeId)
    }

    @PostMapping("/nodes")
    fun createNode(
        @PathVariable projectId: UUID,
        @RequestBody request: CreateNarrativeNodeRequest
    ): NarrativeNodeDto {
        return narrativeService.createNode(projectId, request)
    }

    @PatchMapping("/nodes/{nodeId}")
    fun updateNode(
        @PathVariable projectId: UUID,
        @PathVariable nodeId: UUID,
        @RequestBody request: UpdateNarrativeNodeRequest
    ): NarrativeNodeDto {
        return narrativeService.updateNode(projectId, nodeId, request)
    }

    @PatchMapping("/node-positions")
    fun updateNodePositions(
        @PathVariable projectId: UUID,
        @RequestBody request: UpdateNodePositionsRequest
    ) {
        narrativeService.updateNodePositions(projectId, request)
    }

    @PostMapping("/nodes/{nodeId}/child-layer")
    fun createOrGetChildLayer(
        @PathVariable projectId: UUID,
        @PathVariable nodeId: UUID
    ): ChildLayerDto {
        return narrativeService.createOrGetChildLayer(projectId, nodeId)
    }

    @PostMapping(
        "/assets",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadAsset(
        @PathVariable projectId: UUID,
        @RequestPart("file") file: MultipartFile
    ): NarrativeAssetDto {
        return narrativeAssetService.uploadAsset(projectId, file)
    }

    @DeleteMapping("/nodes/{nodeId}")
    fun deleteNode(
        @PathVariable projectId: UUID,
        @PathVariable nodeId: UUID
    ) {
        narrativeService.deleteNode(projectId, nodeId)
    }
}
