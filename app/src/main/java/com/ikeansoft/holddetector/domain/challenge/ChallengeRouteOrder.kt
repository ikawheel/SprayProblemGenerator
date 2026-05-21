package com.example.holddetector.domain.challenge

import com.example.holddetector.model.Hold

fun normalizeChallengeRouteOrder(
    challengeIndices: Set<Int>,
    preferredOrder: List<Int>,
    holds: List<Hold>,
    startIndex: Int?,
    goalIndex: Int?
): List<Int> {
    val validIndices = challengeIndices.filterTo(linkedSetOf()) { it in holds.indices }
    if (validIndices.isEmpty()) return emptyList()

    val keptOrder = preferredOrder.filter { it in validIndices }.distinct()
    val extras = validIndices - keptOrder.toSet()
    val sortedExtras = extras.sortedWith(
        compareBy<Int> { index -> routeProgress(index, holds, startIndex, goalIndex) }
            .thenBy { index -> holds[index].centerY }
            .thenBy { index -> holds[index].centerX }
    )

    val combined = (keptOrder + sortedExtras).distinct().filter { it in validIndices }
    return when {
        startIndex != null && goalIndex != null && startIndex in validIndices && goalIndex in validIndices && startIndex != goalIndex -> {
            listOf(startIndex) +
                combined.filter { it != startIndex && it != goalIndex } +
                listOf(goalIndex)
        }

        else -> combined
    }
}

private fun routeProgress(
    index: Int,
    holds: List<Hold>,
    startIndex: Int?,
    goalIndex: Int?
): Double {
    val hold = holds[index]
    val start = startIndex?.takeIf { it in holds.indices }?.let { index -> holds.getOrNull(index) }
    val goal = goalIndex?.takeIf { it in holds.indices }?.let { index -> holds.getOrNull(index) }
    if (start == null || goal == null || startIndex == goalIndex) {
        return hold.centerY.toDouble() * 10_000.0 + hold.centerX.toDouble()
    }

    val dx = (goal.centerX - start.centerX).toDouble()
    val dy = (goal.centerY - start.centerY).toDouble()
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared <= 0.0) {
        return hold.centerY.toDouble() * 10_000.0 + hold.centerX.toDouble()
    }

    val relativeX = (hold.centerX - start.centerX).toDouble()
    val relativeY = (hold.centerY - start.centerY).toDouble()
    return (relativeX * dx + relativeY * dy) / lengthSquared
}
