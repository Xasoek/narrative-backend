package com.narrative_service.emosdk.exception

import java.util.UUID

class NarrativeLayerNotFoundException(layerId: UUID) :
    RuntimeException("Narrative layer not found: $layerId")