package com.ikeansoft.holddetector.model

import kotlin.math.abs

data class HoldPoint(
    val x: Int,
    val y: Int
)

const val MIN_HOLD_DIFFICULTY_SCORE = 1
const val MAX_HOLD_DIFFICULTY_SCORE = 5
const val DEFAULT_HOLD_DIFFICULTY_SCORE = 3

data class Hold(
    val points: List<HoldPoint>,
    val difficultyScore: Int = DEFAULT_HOLD_DIFFICULTY_SCORE,
    val isStartCandidate: Boolean = false,
    val isGoalCandidate: Boolean = false
) {
    init {
        require(points.size >= 3) { "Hold must contain at least 3 points" }
        require(difficultyScore in MIN_HOLD_DIFFICULTY_SCORE..MAX_HOLD_DIFFICULTY_SCORE) {
            "Hold difficulty score must be between $MIN_HOLD_DIFFICULTY_SCORE and $MAX_HOLD_DIFFICULTY_SCORE"
        }
    }

    val minX: Int get() = points.minOf { it.x }
    val maxX: Int get() = points.maxOf { it.x }
    val minY: Int get() = points.minOf { it.y }
    val maxY: Int get() = points.maxOf { it.y }

    val centerX: Int get() = ((minX + maxX) / 2.0).toInt()
    val centerY: Int get() = ((minY + maxY) / 2.0).toInt()
    val width: Int get() = (maxX - minX).coerceAtLeast(1)
    val height: Int get() = (maxY - minY).coerceAtLeast(1)

    val area: Double
        get() {
            if (points.size < 3) return 0.0
            var sum = 0L
            for (i in points.indices) {
                val current = points[i]
                val next = points[(i + 1) % points.size]
                sum += current.x.toLong() * next.y.toLong() - next.x.toLong() * current.y.toLong()
            }
            return abs(sum) / 2.0
        }
}
