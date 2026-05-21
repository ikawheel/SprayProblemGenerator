package com.ikeansoft.sprayproblemgenerator.model

data class SavedChallengeSummary(
    val id: String,
    val wallId: String,
    val holdCount: Int,
    val startHoldIndex: Int?,
    val goalHoldIndex: Int?,
    val challengeHoldIndices: Set<Int>,
    val generationMethodName: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class SavedChallengeDetail(
    val id: String,
    val wallId: String,
    val holdCount: Int,
    val generationMethodName: String?,
    val startHoldIndex: Int?,
    val goalHoldIndex: Int?,
    val challengeHoldIndices: Set<Int>,
    val challengeOrderedHoldIndices: List<Int>,
    val createdAt: Long,
    val updatedAt: Long
)
