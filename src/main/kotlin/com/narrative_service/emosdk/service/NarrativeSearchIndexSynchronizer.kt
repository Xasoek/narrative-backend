package com.narrative_service.emosdk.service

import com.narrative_service.emosdk.repository.NarrativeLayerRepository
import com.narrative_service.emosdk.repository.NarrativeNodeRepository
import com.narrative_service.emosdk.repository.NarrativeRepository
import com.narrative_service.emosdk.repository.OpenSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class NarrativeSearchIndexSynchronizer(
    private val narrativeRepository: NarrativeRepository,
    private val narrativeLayerRepository: NarrativeLayerRepository,
    private val narrativeNodeRepository: NarrativeNodeRepository,
    private val openSearchRepository: OpenSearchRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        narrativeRepository.findAll().forEach { narrative ->
            val narrativeId = narrative.id ?: return@forEach
            val projectId = narrative.projectId ?: return@forEach

            try {
                openSearchRepository.ensureIndex(projectId)

                val layerIds = narrativeLayerRepository
                    .findAllByNarrativeId(narrativeId)
                    .mapNotNull { layer -> layer.id }

                val nodes = if (layerIds.isEmpty()) {
                    emptyList()
                } else {
                    narrativeNodeRepository
                        .findAllByLayerIdInOrderByCreatedAtAscIdAsc(layerIds)
                }

                nodes.forEach { node ->
                    openSearchRepository.index(node, projectId)
                }

                openSearchRepository.refresh(projectId)
                logger.info(
                    "Synchronized {} narrative nodes for project {}",
                    nodes.size,
                    projectId
                )
            } catch (exception: Exception) {
                logger.error(
                    "Failed to synchronize narrative search index for project {}",
                    projectId,
                    exception
                )
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(NarrativeSearchIndexSynchronizer::class.java)
    }
}
