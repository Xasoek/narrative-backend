package com.narrative_service.emosdk.repository

import com.narrative_service.emosdk.entity.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectRepository : JpaRepository<Project, UUID>
