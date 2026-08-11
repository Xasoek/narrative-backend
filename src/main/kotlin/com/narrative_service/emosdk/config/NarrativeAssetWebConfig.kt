package com.narrative_service.emosdk.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
@EnableConfigurationProperties(NarrativeAssetProperties::class)
class NarrativeAssetWebConfig(
    private val properties: NarrativeAssetProperties
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val urlPrefix = properties.publicUrlPrefix.trimEnd('/')
        val storageRoot = Paths.get(properties.storageDir)
            .toAbsolutePath()
            .normalize()
            .toUri()
            .toString()
            .let { uri -> if (uri.endsWith('/')) uri else "$uri/" }

        registry.addResourceHandler("$urlPrefix/**")
            .addResourceLocations(storageRoot)
    }
}
