package com.narrative_service.emosdk.service

import com.narrative_service.emosdk.config.NarrativeAssetProperties
import com.narrative_service.emosdk.dto.NarrativeAssetDto
import com.narrative_service.emosdk.entity.NarrativeAsset
import com.narrative_service.emosdk.exception.NarrativeNotFoundException
import com.narrative_service.emosdk.repository.NarrativeAssetRepository
import com.narrative_service.emosdk.repository.NarrativeRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

@Service
class NarrativeAssetService(
    private val narrativeRepository: NarrativeRepository,
    private val narrativeAssetRepository: NarrativeAssetRepository,
    private val properties: NarrativeAssetProperties
) {

    @Transactional
    fun uploadAsset(
        projectId: UUID,
        file: MultipartFile
    ): NarrativeAssetDto {
        val narrative = narrativeRepository.findByProjectId(projectId)
            ?: throw NarrativeNotFoundException(projectId)

        if (file.isEmpty) {
            throw IllegalArgumentException("Asset file is empty")
        }

        if (file.size > properties.maxSizeBytes) {
            throw IllegalArgumentException("Asset file exceeds ${properties.maxSizeBytes} bytes")
        }

        val bytes = file.bytes
        val metadata = ImageMetadataReader.read(bytes)
            ?: throw IllegalArgumentException("Unsupported image MIME type")
        val declaredMimeType = file.contentType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()

        if (
            declaredMimeType != null &&
            declaredMimeType != "application/octet-stream" &&
            declaredMimeType != metadata.mimeType
        ) {
            throw IllegalArgumentException("Declared MIME type does not match uploaded image")
        }

        val storageKey = buildStorageKey(
            narrativeId = requireNotNull(narrative.id),
            extension = metadata.extension
        )
        val savedAsset = narrativeAssetRepository.save(
            NarrativeAsset(
                narrativeId = requireNotNull(narrative.id),
                storageKey = storageKey,
                mimeType = metadata.mimeType,
                sizeBytes = file.size
            )
        )

        writeAsset(storageKey, bytes)

        return NarrativeAssetDto(
            id = requireNotNull(savedAsset.id),
            url = buildAssetUrl(storageKey),
            mimeType = metadata.mimeType,
            width = metadata.width,
            height = metadata.height,
            size = file.size
        )
    }

    private fun buildStorageKey(
        narrativeId: UUID,
        extension: String
    ): String {
        return "narrative/$narrativeId/${UUID.randomUUID()}.$extension"
    }

    private fun writeAsset(
        storageKey: String,
        bytes: ByteArray
    ) {
        val storageRoot = storageRoot()
        val target = storageRoot.resolve(storageKey).normalize()

        if (!target.startsWith(storageRoot)) {
            throw IllegalArgumentException("Invalid asset storage key")
        }

        Files.createDirectories(requireNotNull(target.parent))
        Files.write(target, bytes)
    }

    private fun buildAssetUrl(storageKey: String): String {
        return "${properties.publicUrlPrefix.trimEnd('/')}/$storageKey"
    }

    private fun storageRoot(): Path {
        return Paths.get(properties.storageDir).toAbsolutePath().normalize()
    }
}
