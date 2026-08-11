package com.narrative_service.emosdk.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID


@Entity
@Table(name = "narrative_node")
class NarrativeNode(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "layer_id", nullable = false)
    var layerId: UUID? = null,

    @Column(nullable = false)
    var title: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    var content: JsonNode?? = null,

    @Column(name = "position_x", nullable = false)
    var positionX: Double? = null,

    @Column(name = "position_y", nullable = false)
    var positionY: Double? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
)