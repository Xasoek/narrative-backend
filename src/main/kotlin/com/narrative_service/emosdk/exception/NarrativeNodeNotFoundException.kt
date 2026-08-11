package com.narrative_service.emosdk.exception

import java.util.UUID

class NarrativeNodeNotFoundException(nodeId: UUID) :
    RuntimeException("Narrative node not found: $nodeId")