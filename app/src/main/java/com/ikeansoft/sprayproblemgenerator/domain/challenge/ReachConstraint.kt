package com.ikeansoft.sprayproblemgenerator.domain.challenge

import com.ikeansoft.sprayproblemgenerator.model.ReachCalibrationReference
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class ReachConstraint(
    val horizontalReachPx: Double,
    val verticalReachPx: Double
) {
    fun isWithinReach(dx: Double, dy: Double): Boolean {
        val normalizedDistance =
            (dx * dx) / (horizontalReachPx * horizontalReachPx) +
                (dy * dy) / (verticalReachPx * verticalReachPx)
        return normalizedDistance <= 1.0 + 1e-9
    }

    fun maxReachAlong(dx: Double, dy: Double): Double {
        val distance = hypot(dx, dy)
        if (distance <= 0.0) return min(horizontalReachPx, verticalReachPx)

        val normalizedX = dx / distance
        val normalizedY = dy / distance
        val denominator = sqrt(
            (normalizedX * normalizedX) / (horizontalReachPx * horizontalReachPx) +
                (normalizedY * normalizedY) / (verticalReachPx * verticalReachPx)
        )

        return if (denominator <= 0.0) {
            max(horizontalReachPx, verticalReachPx)
        } else {
            1.0 / denominator
        }
    }
}

internal fun ReachCalibrationReference.toReachConstraint(): ReachConstraint? {
    val calibrationDistancePx = hypot(
        (secondPoint.x - firstPoint.x).toDouble(),
        (secondPoint.y - firstPoint.y).toDouble()
    )
    if (calibrationDistancePx <= 0.0) return null

    val pixelsPerCentimeter = calibrationDistancePx / referenceLengthCm.toDouble()
    return ReachConstraint(
        horizontalReachPx = pixelsPerCentimeter * 120.0,
        verticalReachPx = pixelsPerCentimeter * 60.0
    )
}
