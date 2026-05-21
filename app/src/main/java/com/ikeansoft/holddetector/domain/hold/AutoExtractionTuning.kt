package com.ikeansoft.holddetector.domain.hold

data class AutoExtractionTuning(
    val hueTolerance: Float = 150f,
    val valueTolerance: Float = 0.80f,
    val saturationMin: Float = 0.20f,
    val backgroundDistanceThreshold: Float = 0.20f
)
