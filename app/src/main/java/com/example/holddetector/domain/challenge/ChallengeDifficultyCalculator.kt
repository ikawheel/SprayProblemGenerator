package com.example.holddetector.domain.challenge

import com.example.holddetector.model.Hold
import com.example.holddetector.model.ReachCalibrationReference
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

data class ChallengeDifficultyResult(
    val totalDifficulty: Double,
    val coreMoveDifficulty: Double,
    val supportMoveRatio: Double,
    val moveDifficulties: List<Double>
)

object ChallengeDifficultyCalculator {
    private const val DistanceAlpha = 0.35
    private const val GlobalBeta = 0.20
    private const val DistanceExponent = 1.5

    fun calculate(
        holds: List<Hold>,
        orderedIndices: List<Int>,
        reachCalibrationReference: ReachCalibrationReference?
    ): ChallengeDifficultyResult? {
        val routeIndices = orderedIndices
            .filter { it in holds.indices }
            .distinct()
        if (routeIndices.size < 2) return null

        val reachConstraint = reachCalibrationReference?.toReachConstraint()
        val moveDifficulties = routeIndices.zipWithNext { previousIndex, nextIndex ->
            val previousHold = holds[previousIndex]
            val nextHold = holds[nextIndex]
            val distanceLoad = normalizedDistanceLoad(previousHold, nextHold, reachConstraint)
            val holdDifficulty = previousHold.difficultyScore + nextHold.difficultyScore
            holdDifficulty * (1.0 + DistanceAlpha * distanceLoad)
        }
        if (moveDifficulties.isEmpty()) return null

        val coreMoveIndex = moveDifficulties.indices.maxByOrNull { moveDifficulties[it] } ?: return null
        val coreMoveDifficulty = moveDifficulties[coreMoveIndex]
        val supportMoves = moveDifficulties.toMutableList().apply { removeAt(coreMoveIndex) }
        val supportMoveRatio = if (supportMoves.isEmpty() || coreMoveDifficulty <= 0.0) {
            0.0
        } else {
            (supportMoves.average() / coreMoveDifficulty).coerceAtLeast(0.0)
        }

        return ChallengeDifficultyResult(
            totalDifficulty = coreMoveDifficulty * (1.0 + GlobalBeta * supportMoveRatio),
            coreMoveDifficulty = coreMoveDifficulty,
            supportMoveRatio = supportMoveRatio,
            moveDifficulties = moveDifficulties
        )
    }

    private fun normalizedDistanceLoad(
        previousHold: Hold,
        nextHold: Hold,
        reachConstraint: ReachConstraint?
    ): Double {
        val dx = abs(nextHold.centerX - previousHold.centerX).toDouble()
        val dy = abs(nextHold.centerY - previousHold.centerY).toDouble()
        val normalizedDistance = if (reachConstraint == null) {
            0.0
        } else {
            sqrt(
                (dx * dx) / (reachConstraint.horizontalReachPx * reachConstraint.horizontalReachPx) +
                    (dy * dy) / (reachConstraint.verticalReachPx * reachConstraint.verticalReachPx)
            )
        }.coerceAtMost(1.0)

        return normalizedDistance.pow(DistanceExponent)
    }
}
