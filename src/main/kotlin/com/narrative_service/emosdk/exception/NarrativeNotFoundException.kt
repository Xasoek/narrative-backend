package com.narrative_service.emosdk.exception

import java.util.UUID

class NarrativeNotFoundException(projectId: UUID) :
    RuntimeException("Narrative not found for project: $projectId")