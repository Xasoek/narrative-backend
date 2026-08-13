package com.narrative_service.emosdk.service

import com.narrative_service.emosdk.dto.PositionDto
import com.narrative_service.emosdk.dto.narrative.CreateNarrativeNodeRequest
import com.narrative_service.emosdk.dto.narrative.UpdateNarrativeNodeRequest
import com.narrative_service.emosdk.entity.Narrative
import com.narrative_service.emosdk.entity.NarrativeLayer
import com.narrative_service.emosdk.entity.NarrativeNode
import com.narrative_service.emosdk.entity.Project
import com.narrative_service.emosdk.mapper.NarrativeLayerMapper
import com.narrative_service.emosdk.mapper.NarrativeMapper
import com.narrative_service.emosdk.mapper.NarrativeNodeMapper
import com.narrative_service.emosdk.mapper.NarrativeTreeMapper
import com.narrative_service.emosdk.repository.NarrativeLayerRepository
import com.narrative_service.emosdk.repository.NarrativeNodeReferenceRepository
import com.narrative_service.emosdk.repository.NarrativeNodeRepository
import com.narrative_service.emosdk.repository.NarrativeRepository
import com.narrative_service.emosdk.repository.ProjectRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import tools.jackson.databind.ObjectMapper
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NarrativeServiceTest {

    private lateinit var narrativeRepository: NarrativeRepository
    private lateinit var narrativeLayerRepository: NarrativeLayerRepository
    private lateinit var narrativeNodeRepository: NarrativeNodeRepository
    private lateinit var narrativeNodeReferenceRepository: NarrativeNodeReferenceRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var service: NarrativeService

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        narrativeRepository = Mockito.mock(NarrativeRepository::class.java)
        narrativeLayerRepository = Mockito.mock(NarrativeLayerRepository::class.java)
        narrativeNodeRepository = Mockito.mock(NarrativeNodeRepository::class.java)
        narrativeNodeReferenceRepository = Mockito.mock(NarrativeNodeReferenceRepository::class.java)
        projectRepository = Mockito.mock(ProjectRepository::class.java)

        service = NarrativeService(
            narrativeRepository = narrativeRepository,
            narrativeMapper = NarrativeMapper(),
            narrativeLayerRepository = narrativeLayerRepository,
            narrativeNodeRepository = narrativeNodeRepository,
            narrativeTreeMapper = NarrativeTreeMapper(),
            narrativeNodeReferenceRepository = narrativeNodeReferenceRepository,
            narrativeLayerMapper = NarrativeLayerMapper(),
            narrativeNodeMapper = NarrativeNodeMapper(),
            projectRepository = projectRepository
        )
    }

    @Test
    fun `getLayer returns root project breadcrumb and parent layer breadcrumb`() {
        val projectId = UUID.randomUUID()
        val narrativeId = UUID.randomUUID()
        val rootLayerId = UUID.randomUUID()
        val childLayerId = UUID.randomUUID()
        val parentNodeId = UUID.randomUUID()

        val narrative = Narrative(
            id = narrativeId,
            projectId = projectId,
            rootLayerId = rootLayerId
        )
        val rootLayer = NarrativeLayer(
            id = rootLayerId,
            narrativeId = narrativeId,
            parentNodeId = null
        )
        val childLayer = NarrativeLayer(
            id = childLayerId,
            narrativeId = narrativeId,
            parentNodeId = parentNodeId
        )
        val parentNode = NarrativeNode(
            id = parentNodeId,
            layerId = rootLayerId,
            title = "Сюжет",
            content = doc("Родитель"),
            positionX = 0.0,
            positionY = 0.0
        )

        Mockito.`when`(narrativeRepository.findByProjectId(projectId)).thenReturn(narrative)
        Mockito.`when`(narrativeLayerRepository.findByIdAndNarrativeId(childLayerId, narrativeId)).thenReturn(childLayer)
        Mockito.`when`(narrativeNodeRepository.findAllByLayerIdOrderByCreatedAtAscIdAsc(childLayerId)).thenReturn(emptyList())
        Mockito.`when`(projectRepository.findById(projectId)).thenReturn(
            Optional.of(Project(id = projectId, name = "Проект Альфа"))
        )
        Mockito.`when`(narrativeNodeRepository.findByIdAndNarrativeId(parentNodeId, narrativeId)).thenReturn(parentNode)
        Mockito.`when`(narrativeLayerRepository.findByIdAndNarrativeId(rootLayerId, narrativeId)).thenReturn(rootLayer)

        val result = service.getLayer(projectId, childLayerId)

        assertEquals(2, result.breadcrumbs.size)
        assertEquals(rootLayerId, result.breadcrumbs[0].layerId)
        assertEquals(null, result.breadcrumbs[0].nodeId)
        assertEquals("Проект Альфа", result.breadcrumbs[0].title)
        assertEquals(childLayerId, result.breadcrumbs[1].layerId)
        assertEquals(parentNodeId, result.breadcrumbs[1].nodeId)
        assertEquals("Сюжет", result.breadcrumbs[1].title)
    }

    @Test
    fun `getLayer uses fallback breadcrumb when project metadata is missing`() {
        val projectId = UUID.randomUUID()
        val narrativeId = UUID.randomUUID()
        val rootLayerId = UUID.randomUUID()

        val narrative = Narrative(
            id = narrativeId,
            projectId = projectId,
            rootLayerId = rootLayerId
        )
        val rootLayer = NarrativeLayer(
            id = rootLayerId,
            narrativeId = narrativeId,
            parentNodeId = null
        )

        Mockito.`when`(narrativeRepository.findByProjectId(projectId)).thenReturn(narrative)
        Mockito.`when`(narrativeLayerRepository.findByIdAndNarrativeId(rootLayerId, narrativeId)).thenReturn(rootLayer)
        Mockito.`when`(narrativeNodeRepository.findAllByLayerIdOrderByCreatedAtAscIdAsc(rootLayerId)).thenReturn(emptyList())
        Mockito.`when`(projectRepository.findById(projectId)).thenReturn(Optional.empty())

        val result = service.getLayer(projectId, rootLayerId)

        assertEquals(1, result.breadcrumbs.size)
        assertEquals(rootLayerId, result.breadcrumbs[0].layerId)
        assertEquals(null, result.breadcrumbs[0].nodeId)
        assertEquals("Проект", result.breadcrumbs[0].title)
    }

    @Test
    fun `createNode rejects linked nodes outside narrative`() {
        val projectId = UUID.randomUUID()
        val narrativeId = UUID.randomUUID()
        val rootLayerId = UUID.randomUUID()
        val linkedNodeId = UUID.randomUUID()

        Mockito.`when`(narrativeRepository.findByProjectId(projectId)).thenReturn(
            Narrative(
                id = narrativeId,
                projectId = projectId,
                rootLayerId = rootLayerId
            )
        )
        Mockito.`when`(narrativeLayerRepository.findByIdAndNarrativeId(rootLayerId, narrativeId)).thenReturn(
            NarrativeLayer(
                id = rootLayerId,
                narrativeId = narrativeId
            )
        )
        Mockito.`when`(
            narrativeNodeRepository.findAllByIdInAndNarrativeId(listOf(linkedNodeId), narrativeId)
        ).thenReturn(emptyList())

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.createNode(
                projectId,
                CreateNarrativeNodeRequest(
                    layerId = rootLayerId,
                    title = "Новая карточка",
                    content = doc("Контент"),
                    position = PositionDto(x = 760.0, y = 220.0),
                    linkedNodeIds = listOf(linkedNodeId)
                )
            )
        }

        assertTrue(requireNotNull(exception.message).contains("linkedNodeIds"))
    }

    @Test
    fun `updateNode requires linkedNodeIds when content is provided`() {
        val projectId = UUID.randomUUID()
        val narrativeId = UUID.randomUUID()
        val rootLayerId = UUID.randomUUID()
        val nodeId = UUID.randomUUID()

        Mockito.`when`(narrativeRepository.findByProjectId(projectId)).thenReturn(
            Narrative(
                id = narrativeId,
                projectId = projectId,
                rootLayerId = rootLayerId
            )
        )
        Mockito.`when`(narrativeNodeRepository.findByIdAndNarrativeId(nodeId, narrativeId)).thenReturn(
            NarrativeNode(
                id = nodeId,
                layerId = rootLayerId,
                title = "Герой",
                content = doc("Старый текст"),
                positionX = 100.0,
                positionY = 200.0
            )
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.updateNode(
                projectId,
                nodeId,
                UpdateNarrativeNodeRequest(
                    title = null,
                    content = doc("Новый текст"),
                    linkedNodeIds = null
                )
            )
        }

        assertEquals("linkedNodeIds is required when content is provided", exception.message)
    }

    private fun doc(text: String) = objectMapper.readTree(
        """
        {
          "type": "doc",
          "content": [
            {
              "type": "paragraph",
              "content": [
                { "type": "text", "text": "$text" }
              ]
            }
          ]
        }
        """.trimIndent()
    )
}
