package com.example.holddetector.domain.hold

data class AutoExtractionTuning(
    val hueTolerance: Float = 24f,
    val valueTolerance: Float = 0.24f,
    val saturationMin: Float = 0.22f,
    val backgroundDistanceThreshold: Float = 0.18f
)
