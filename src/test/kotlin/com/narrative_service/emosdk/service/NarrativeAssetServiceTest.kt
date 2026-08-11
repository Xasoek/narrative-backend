package com.narrative_service.emosdk.service

import com.narrative_service.emosdk.config.NarrativeAssetProperties
import com.narrative_service.emosdk.entity.Narrative
import com.narrative_service.emosdk.entity.NarrativeAsset
import com.narrative_service.emosdk.repository.NarrativeAssetRepository
import com.narrative_service.emosdk.repository.NarrativeRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NarrativeAssetServiceTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `uploadAsset stores image metadata and returns public url`() {
        val projectId = UUID.randomUUID()
        val narrativeId = UUID.randomUUID()
        val assetId = UUID.randomUUID()
        val narrativeRepository = Mockito.mock(NarrativeRepository::class.java)
        val assetRepository = Mockito.mock(NarrativeAssetRepository::class.java)
        val service = service(narrativeRepository, assetRepository)

        Mockito.`when`(narrativeRepository.findByProjectId(projectId)).thenReturn(
            Narrative(
                id = narrativeId,
                projectId = projectId,
                rootLayerId = UUID.randomUUID()
            )
        )
        Mockito.`when`(assetRepository.save(ArgumentMatchers.any(NarrativeAsset::class.java))).thenAnswer { invocation ->
            invocation.getArgument<NarrativeAsset>(0).apply {
                id = assetId
            }
        }

        val file = MockMultipartFile(
            "file",
            "pixel.png",
            "image/png",
            oneByOnePng()
        )

        val result = service.uploadAsset(projectId, file)

        assertEquals(assetId, result.id)
        assertEquals("image/png", result.mimeType)
        assertEquals(1, result.width)
        assertEquals(1, result.height)
        assertEquals(file.size, result.size)
        assertTrue(result.url.startsWith("/narrative-assets/narrative/$narrativeId/"))

        Files.walk(tempDir).use { paths ->
            assertEquals(1L, paths.filter { Files.isRegularFile(it) }.count())
        }
    }

    @Test
    fun `uploadAsset rejects mismatched MIME type`() {
        val projectId = UUID.randomUUID()
        val narrativeRepository = Mockito.mock(NarrativeRepository::class.java)
        val assetRepository = Mockito.mock(NarrativeAssetRepository::class.java)
        val service = service(narrativeRepository, assetRepository)

        Mockito.`when`(narrativeRepository.findByProjectId(projectId)).thenReturn(
            Narrative(
                id = UUID.randomUUID(),
                projectId = projectId,
                rootLayerId = UUID.randomUUID()
            )
        )

        val file = MockMultipartFile(
            "file",
            "pixel.txt",
            "text/plain",
            oneByOnePng()
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.uploadAsset(projectId, file)
        }

        assertEquals("Declared MIME type does not match uploaded image", exception.message)
    }

    private fun service(
        narrativeRepository: NarrativeRepository,
        assetRepository: NarrativeAssetRepository
    ): NarrativeAssetService {
        return NarrativeAssetService(
            narrativeRepository = narrativeRepository,
            narrativeAssetRepository = assetRepository,
            properties = NarrativeAssetProperties(
                storageDir = tempDir.toString(),
                publicUrlPrefix = "/narrative-assets",
                maxSizeBytes = 1024
            )
        )
    }

    private fun oneByOnePng(): ByteArray {
        return Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
        )
    }
}
