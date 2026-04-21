package com.example.holddetector.domain.hold

data class AutoExtractionTuning(
    val minimumThreshold: Float = 18f,
    val standardDeviationMultiplier: Float = 1.6f,
    val areaFilterStrength: Float = 1f,
    val smoothingStrength: Float = 0.4f
)
