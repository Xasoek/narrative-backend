package com.narrative_service.emosdk.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(name = "narrative_asset")
class NarrativeAsset(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "narrative_id", nullable = false)
    var narrativeId: UUID? = null,

    @Column(name = "storage_key", nullable = false)
    var storageKey: String? = null,

    @Column(name = "mime_type", nullable = false)
    var mimeType: String? = null,

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
)