package com.example.holddetector.domain.challenge

data class RouteGenerationTuning(
    val holdCountVariance: Float = 0.75f,
    val detourStrength: Float = 0.75f,
    val routeWaviness: Float = 0.75f,
    val stepDistanceVariance: Float = 0.75f,
    val corridorWidth: Float = 0.75f,
    val candidateSelectionRandomness: Float = 0.75f,
    val finalSelectionRandomness: Float = 0.75f
)
