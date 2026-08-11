package com.narrative_service.emosdk.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "narrative.assets")
data class NarrativeAssetProperties(
    var storageDir: String = "uploads/narrative-assets",
    var publicUrlPrefix: String = "/narrative-assets",
    var maxSizeBytes: Long = 10 * 1024 * 1024
)
