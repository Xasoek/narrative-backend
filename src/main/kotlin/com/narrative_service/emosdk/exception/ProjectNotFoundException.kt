package com.narrative_service.emosdk.exception

import java.util.UUID

class ProjectNotFoundException(projectId: UUID) :
    RuntimeException("Project not found: $projectId")
