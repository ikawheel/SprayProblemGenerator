package com.example.holddetector.domain.challenge

data class RouteGenerationTuning(
    val detourStrength: Float = 0.75f,
    val routeWaviness: Float = 0.75f,
    val stepDistanceVariance: Float = 0.75f,
    val corridorWidth: Float = 0.75f,
    val excludePreviouslyGeneratedHolds: Boolean = true,
    val randomStartGoalPairLimit: Int = 10,
    val routeGenerationAttemptLimit: Int = 100
)
