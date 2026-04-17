package com.example.holddetector.domain.challenge

import com.example.holddetector.model.Hold
import com.example.holddetector.model.ReachCalibrationReference
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

object ChallengeRouteGenerator {
    fun generate(
        holds: List<Hold>,
        sourceIndices: Set<Int>,
        startIndex: Int,
        goalIndex: Int,
        targetCount: Int,
        tuning: RouteGenerationTuning,
        reachCalibrationReference: ReachCalibrationReference?
    ): Set<Int>? {
        if (startIndex !in holds.indices || goalIndex !in holds.indices) return null

        val usableIndices = sourceIndices.toMutableSet().apply {
            add(startIndex)
            add(goalIndex)
        }
        if (usableIndices.size < 2) return null

        val requestedCount = targetCount.coerceIn(2, usableIndices.size)
        val holdCenters = holds.indices.associateWith { index -> holds[index].toCenterPoint(index) }
        val start = holdCenters.getValue(startIndex)
        val goal = holdCenters.getValue(goalIndex)
        val directDistance = hypot(goal.x - start.x, goal.y - start.y).coerceAtLeast(1.0)
        val reachConstraint = reachCalibrationReference?.toReachConstraint()
        val minimumCountByReach = reachConstraint?.let { constraint ->
            val directLimit = constraint.maxReachAlong(
                dx = goal.x - start.x,
                dy = goal.y - start.y
            ).coerceAtLeast(1.0)
            kotlin.math.ceil(directDistance / directLimit).toInt() + 1
        } ?: 2
        val actualCount = max(
            chooseAutoRouteCount(
                requestedCount = requestedCount,
                maxAvailableCount = usableIndices.size,
                randomness = tuning.holdCountVariance.toDouble()
            ),
            minimumCountByReach
        )
        if (actualCount > usableIndices.size) return null
        if (actualCount == 2) return setOf(startIndex, goalIndex)

        val candidateCenters = usableIndices
            .asSequence()
            .filterNot { it == startIndex || it == goalIndex }
            .mapNotNull(holdCenters::get)
            .toList()
        val intermediateCount = actualCount - 2
        if (candidateCenters.size < intermediateCount) return null

        val expectedStepDistance = directDistance / (actualCount - 1)
        val routeVariants = buildRouteVariants(
            start = start,
            goal = goal,
            candidateCenters = candidateCenters,
            expectedStepDistance = expectedStepDistance,
            tuning = tuning
        )

        val attempts = buildList {
            routeVariants.forEach { variant ->
                repeat(blendInt(3, 15, tuning.finalSelectionRandomness.toDouble())) {
                    buildRouteAttempt(
                        variant = variant,
                        start = start,
                        goal = goal,
                        candidates = candidateCenters,
                        holdCenters = holdCenters,
                        actualCount = actualCount,
                        tuning = tuning,
                        reachConstraint = reachConstraint,
                        deterministic = false
                    )?.let(::add)
                }
                if (Random.nextDouble() < blendDouble(1.0, 0.45, tuning.finalSelectionRandomness.toDouble())) {
                    buildRouteAttempt(
                        variant = variant,
                        start = start,
                        goal = goal,
                        candidates = candidateCenters,
                        holdCenters = holdCenters,
                        actualCount = actualCount,
                        tuning = tuning,
                        reachConstraint = reachConstraint,
                        deterministic = true
                    )?.let(::add)
                }
            }
        }

        return selectRouteAttempt(attempts, tuning.finalSelectionRandomness.toDouble())?.indices
    }
}

private data class HoldCenterPoint(
    val index: Int,
    val x: Double,
    val y: Double
)

private data class RoutePoint(
    val x: Double,
    val y: Double
)

private enum class AutoRouteShape {
    STRAIGHT,
    LEFT_DETOUR,
    RIGHT_DETOUR
}

private data class RouteVariant(
    val shape: AutoRouteShape,
    val pathPoints: List<RoutePoint>,
    val corridorWidth: Double
)

private data class RoutedHoldCandidate(
    val index: Int,
    val distanceAlongRoute: Double,
    val distanceToRoute: Double
)

private data class RouteAttempt(
    val shape: AutoRouteShape,
    val indices: Set<Int>,
    val score: Double,
    val deterministic: Boolean
)

private data class PolylineProjection(
    val distanceAlongRoute: Double,
    val distanceToRoute: Double
)

private data class ReachConstraint(
    val horizontalReachPx: Double,
    val verticalReachPx: Double
) {
    fun isWithinReach(from: HoldCenterPoint, to: HoldCenterPoint): Boolean {
        return isWithinReach(
            dx = to.x - from.x,
            dy = to.y - from.y
        )
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

    private fun isWithinReach(dx: Double, dy: Double): Boolean {
        val normalizedDistance =
            (dx * dx) / (horizontalReachPx * horizontalReachPx) +
                (dy * dy) / (verticalReachPx * verticalReachPx)
        return normalizedDistance <= 1.0 + 1e-9
    }
}

private fun normalizedRandomness(randomness: Double): Double {
    return randomness.coerceIn(0.0, 1.0)
}

private fun blendDouble(stable: Double, wild: Double, randomness: Double): Double {
    val t = normalizedRandomness(randomness)
    return stable + (wild - stable) * t
}

private fun blendInt(stable: Int, wild: Int, randomness: Double): Int {
    return blendDouble(stable.toDouble(), wild.toDouble(), randomness).roundToInt()
}

private fun chooseAutoRouteCount(
    requestedCount: Int,
    maxAvailableCount: Int,
    randomness: Double
): Int {
    if (maxAvailableCount <= 2) return 2

    val variance = when {
        requestedCount <= 4 -> blendInt(0, 2, randomness)
        requestedCount <= 8 -> blendInt(1, 4, randomness)
        else -> blendInt(1, 5, randomness)
    }
    val lowerBound = max(2, requestedCount - variance)
    val upperBound = min(maxAvailableCount, requestedCount + variance)

    if (upperBound <= lowerBound) {
        return lowerBound
    }

    return Random.nextInt(lowerBound, upperBound + 1)
}

private fun buildRouteAttempt(
    variant: RouteVariant,
    start: HoldCenterPoint,
    goal: HoldCenterPoint,
    candidates: List<HoldCenterPoint>,
    holdCenters: Map<Int, HoldCenterPoint>,
    actualCount: Int,
    tuning: RouteGenerationTuning,
    reachConstraint: ReachConstraint?,
    deterministic: Boolean = false
): RouteAttempt? {
    val intermediateCount = actualCount - 2
    if (intermediateCount <= 0) {
        if (reachConstraint != null && !reachConstraint.isWithinReach(start, goal)) {
            return null
        }
        return RouteAttempt(
            shape = variant.shape,
            indices = setOf(start.index, goal.index),
            score = 0.0,
            deterministic = deterministic
        )
    }

    val routeLength = calculatePolylineLength(variant.pathPoints)
    if (routeLength <= 0.0) return null

    val corridorCandidates = candidates
        .mapNotNull { candidate ->
            projectPointOntoPolyline(
                point = candidate.toRoutePoint(),
                polyline = variant.pathPoints
            )?.takeIf { projection ->
                projection.distanceToRoute <= variant.corridorWidth
            }?.let { projection ->
                RoutedHoldCandidate(
                    index = candidate.index,
                    distanceAlongRoute = projection.distanceAlongRoute,
                    distanceToRoute = projection.distanceToRoute
                )
            }
        }
        .sortedBy { it.distanceAlongRoute }

    if (corridorCandidates.size < intermediateCount) return null

    val expectedRouteStepDistance = routeLength / (actualCount - 1)
    val targetDistances = createTargetDistances(
        routeLength = routeLength,
        intermediateCount = intermediateCount,
        randomness = tuning.stepDistanceVariance.toDouble(),
        deterministic = deterministic
    )

    val selected = mutableListOf<RoutedHoldCandidate>()
    val remaining = corridorCandidates.toMutableList()
    var previousDistanceAlongRoute = 0.0
    var previousHoldCenter = start
    val stepVariance = tuning.stepDistanceVariance.toDouble()
    val candidateRandomness = tuning.candidateSelectionRandomness.toDouble()
    val minimumStepRatio = if (deterministic) {
        0.35
    } else {
        Random.nextDouble(
            blendDouble(0.28, 0.08, stepVariance),
            blendDouble(0.40, 0.58, stepVariance)
        )
    }
    val targetPenaltyWeight = if (deterministic) {
        1.0
    } else {
        Random.nextDouble(
            blendDouble(0.95, 0.48, candidateRandomness),
            blendDouble(1.05, 1.34, candidateRandomness)
        )
    }
    val spacingPenaltyWeight = if (deterministic) {
        0.45
    } else {
        Random.nextDouble(
            blendDouble(0.40, 0.02, candidateRandomness),
            blendDouble(0.55, 0.92, candidateRandomness)
        )
    }
    val routePenaltyWeight = if (deterministic) {
        0.85
    } else {
        Random.nextDouble(
            blendDouble(0.75, 0.08, candidateRandomness),
            blendDouble(0.90, 1.08, candidateRandomness)
        )
    }

    targetDistances.forEachIndexed { targetIndex, targetDistance ->
        val remainingSlots = targetDistances.size - targetIndex - 1
        val dynamicStepRatio = if (deterministic) {
            minimumStepRatio
        } else {
            Random.nextDouble(
                max(0.04, minimumStepRatio * blendDouble(0.90, 0.45, stepVariance)),
                min(0.82, minimumStepRatio * blendDouble(1.10, 1.90, stepVariance))
            )
        }
        val minAllowedDistance = previousDistanceAlongRoute + expectedRouteStepDistance * dynamicStepRatio
        val maxAllowedDistance =
            routeLength - expectedRouteStepDistance * dynamicStepRatio * (remainingSlots + 1)

        val eligible = remaining.filter { candidate ->
            val candidateCenter = holdCenters[candidate.index] ?: return@filter false
            val withinReach = reachConstraint == null ||
                reachConstraint.isWithinReach(previousHoldCenter, candidateCenter)
            withinReach && candidate.distanceAlongRoute in minAllowedDistance..maxAllowedDistance
        }.ifEmpty {
            remaining.filter { candidate ->
                val candidateCenter = holdCenters[candidate.index] ?: return@filter false
                val withinReach = reachConstraint == null ||
                    reachConstraint.isWithinReach(previousHoldCenter, candidateCenter)
                withinReach && candidate.distanceAlongRoute > previousDistanceAlongRoute
            }
        }.ifEmpty {
            remaining.filter { candidate ->
                val candidateCenter = holdCenters[candidate.index] ?: return@filter false
                reachConstraint == null ||
                    reachConstraint.isWithinReach(previousHoldCenter, candidateCenter)
            }
        }
        if (eligible.isEmpty()) return null

        val scoredCandidates = eligible
            .map { candidate ->
                val targetPenalty = abs(candidate.distanceAlongRoute - targetDistance)
                val stepDistance = candidate.distanceAlongRoute - previousDistanceAlongRoute
                val spacingPenalty = abs(stepDistance - expectedRouteStepDistance)
                val routePenalty = candidate.distanceToRoute
                val jitter = if (deterministic) {
                    0.0
                } else {
                    Random.nextDouble(
                        0.0,
                        expectedRouteStepDistance * blendDouble(0.08, 0.42, candidateRandomness) + 1.0
                    )
                }
                candidate to (
                    targetPenalty * targetPenaltyWeight +
                        spacingPenalty * spacingPenaltyWeight +
                        routePenalty * routePenaltyWeight +
                        jitter
                    )
            }
            .sortedBy { (_, score) -> score }

        val chosen = pickRouteCandidate(
            scoredCandidates = scoredCandidates,
            randomness = candidateRandomness,
            deterministic = deterministic
        )

        selected += chosen
        remaining.removeAll { it.index == chosen.index }
        previousDistanceAlongRoute = chosen.distanceAlongRoute
        previousHoldCenter = holdCenters[chosen.index] ?: previousHoldCenter
    }

    if (reachConstraint != null && !reachConstraint.isWithinReach(previousHoldCenter, goal)) {
        return null
    }

    val routeIndices = buildList {
        add(start.index)
        selected.sortedBy { it.distanceAlongRoute }.forEach { add(it.index) }
        add(goal.index)
    }

    val score = scoreGeneratedRoute(
        routePoints = routeIndices.mapNotNull { index -> holdCenters[index] },
        expectedStepDistance = expectedRouteStepDistance,
        routeLength = routeIndices.size,
        adherenceDistances = selected.map { it.distanceToRoute },
        availableCandidateCount = corridorCandidates.size,
        randomness = stepVariance
    )

    return RouteAttempt(
        shape = variant.shape,
        indices = routeIndices.toSet(),
        score = score,
        deterministic = deterministic
    )
}

private fun buildRouteVariants(
    start: HoldCenterPoint,
    goal: HoldCenterPoint,
    candidateCenters: List<HoldCenterPoint>,
    expectedStepDistance: Double,
    tuning: RouteGenerationTuning,
): List<RouteVariant> {
    val detourStrength = tuning.detourStrength.toDouble()
    val routeWaviness = tuning.routeWaviness.toDouble()
    val corridorWidth = tuning.corridorWidth.toDouble()
    val directDx = goal.x - start.x
    val directDy = goal.y - start.y
    val directLength = hypot(directDx, directDy).coerceAtLeast(1.0)

    val signedOffsets = candidateCenters.map { candidate ->
        signedDistanceFromLine(
            point = candidate.toRoutePoint(),
            lineStart = start.toRoutePoint(),
            lineEnd = goal.toRoutePoint()
        )
    }
    val leftExtent = signedOffsets
        .filter { it > expectedStepDistance * 0.12 }
        .maxOrNull()
        ?.let(::abs)
        ?: 0.0
    val rightExtent = signedOffsets
        .filter { it < -expectedStepDistance * 0.12 }
        .minOrNull()
        ?.let(::abs)
        ?: 0.0

    val baseDetour = max(expectedStepDistance * 0.95, directLength * 0.18)
    val leftOffset = chooseDetourOffset(
        availableExtent = leftExtent,
        baseDetour = baseDetour,
        directLength = directLength,
        randomness = detourStrength
    )
    val rightOffset = chooseDetourOffset(
        availableExtent = rightExtent,
        baseDetour = baseDetour,
        directLength = directLength,
        randomness = detourStrength
    )

    val straightWidth = max(expectedStepDistance * 0.55, 40.0)
    val leftWidth = max(expectedStepDistance * 0.72, max(leftOffset * 0.55, 48.0))
    val rightWidth = max(expectedStepDistance * 0.72, max(rightOffset * 0.55, 48.0))
    return buildList {
        add(
            buildOffsetRouteVariant(
                shape = AutoRouteShape.STRAIGHT,
                start = start,
                goal = goal,
                firstProgress = 0.30,
                midpointProgress = 0.50,
                secondProgress = 0.70,
                firstOffset = 0.0,
                midpointOffset = 0.0,
                secondOffset = 0.0,
                minimumWidth = straightWidth,
                randomness = corridorWidth
            )
        )

        repeat(blendInt(2, 7, routeWaviness)) {
            val straightWave = expectedStepDistance * Random.nextDouble(
                blendDouble(0.05, 0.12, routeWaviness),
                blendDouble(0.60, 2.40, routeWaviness)
            )
            val straightCenterOffsetRange = blendDouble(0.15, 1.15, routeWaviness)
            val straightCenterOffset = expectedStepDistance * Random.nextDouble(
                -straightCenterOffsetRange,
                straightCenterOffsetRange
            )
            add(
                buildOffsetRouteVariant(
                    shape = AutoRouteShape.STRAIGHT,
                    start = start,
                    goal = goal,
                    firstProgress = Random.nextDouble(
                        blendDouble(0.22, 0.10, routeWaviness),
                        blendDouble(0.32, 0.42, routeWaviness)
                    ),
                    midpointProgress = Random.nextDouble(
                        blendDouble(0.46, 0.30, routeWaviness),
                        blendDouble(0.54, 0.70, routeWaviness)
                    ),
                    secondProgress = Random.nextDouble(
                        blendDouble(0.68, 0.58, routeWaviness),
                        blendDouble(0.80, 0.92, routeWaviness)
                    ),
                    firstOffset = straightWave * Random.nextDouble(
                        -blendDouble(0.40, 1.35, routeWaviness),
                        blendDouble(0.40, 1.35, routeWaviness)
                    ),
                    midpointOffset = straightCenterOffset,
                    secondOffset = straightWave * Random.nextDouble(
                        -blendDouble(0.45, 1.45, routeWaviness),
                        blendDouble(0.45, 1.45, routeWaviness)
                    ),
                    minimumWidth = max(
                        straightWidth,
                        expectedStepDistance * Random.nextDouble(
                            blendDouble(0.90, 0.68, corridorWidth),
                            blendDouble(1.10, 1.74, corridorWidth)
                        )
                    ),
                    randomness = corridorWidth
                )
            )
        }

        repeat(blendInt(2, 7, routeWaviness)) {
            add(
                buildOffsetRouteVariant(
                    shape = AutoRouteShape.LEFT_DETOUR,
                    start = start,
                    goal = goal,
                    firstProgress = Random.nextDouble(
                        blendDouble(0.20, 0.10, routeWaviness),
                        blendDouble(0.32, 0.40, routeWaviness)
                    ),
                    midpointProgress = Random.nextDouble(
                        blendDouble(0.46, 0.28, routeWaviness),
                        blendDouble(0.56, 0.70, routeWaviness)
                    ),
                    secondProgress = Random.nextDouble(
                        blendDouble(0.70, 0.58, routeWaviness),
                        blendDouble(0.82, 0.92, routeWaviness)
                    ),
                    firstOffset = leftOffset * Random.nextDouble(
                        blendDouble(0.85, 0.32, detourStrength),
                        blendDouble(1.10, 1.95, detourStrength)
                    ),
                    midpointOffset = leftOffset * Random.nextDouble(
                        -blendDouble(0.12, 0.55, routeWaviness),
                        blendDouble(0.55, 1.55, routeWaviness)
                    ),
                    secondOffset = leftOffset * Random.nextDouble(
                        -blendDouble(0.20, 1.05, routeWaviness),
                        blendDouble(0.55, 1.20, routeWaviness)
                    ),
                    minimumWidth = max(
                        leftWidth,
                        expectedStepDistance * Random.nextDouble(
                            blendDouble(0.92, 0.66, corridorWidth),
                            blendDouble(1.12, 1.92, corridorWidth)
                        )
                    ),
                    randomness = corridorWidth
                )
            )
        }

        repeat(blendInt(2, 7, routeWaviness)) {
            add(
                buildOffsetRouteVariant(
                    shape = AutoRouteShape.RIGHT_DETOUR,
                    start = start,
                    goal = goal,
                    firstProgress = Random.nextDouble(
                        blendDouble(0.20, 0.10, routeWaviness),
                        blendDouble(0.32, 0.40, routeWaviness)
                    ),
                    midpointProgress = Random.nextDouble(
                        blendDouble(0.46, 0.28, routeWaviness),
                        blendDouble(0.56, 0.70, routeWaviness)
                    ),
                    secondProgress = Random.nextDouble(
                        blendDouble(0.70, 0.58, routeWaviness),
                        blendDouble(0.82, 0.92, routeWaviness)
                    ),
                    firstOffset = -rightOffset * Random.nextDouble(
                        blendDouble(0.85, 0.32, detourStrength),
                        blendDouble(1.10, 1.95, detourStrength)
                    ),
                    midpointOffset = -rightOffset * Random.nextDouble(
                        -blendDouble(0.12, 0.55, routeWaviness),
                        blendDouble(0.55, 1.55, routeWaviness)
                    ),
                    secondOffset = -rightOffset * Random.nextDouble(
                        -blendDouble(0.20, 1.05, routeWaviness),
                        blendDouble(0.55, 1.20, routeWaviness)
                    ),
                    minimumWidth = max(
                        rightWidth,
                        expectedStepDistance * Random.nextDouble(
                            blendDouble(0.92, 0.66, corridorWidth),
                            blendDouble(1.12, 1.92, corridorWidth)
                        )
                    ),
                    randomness = corridorWidth
                )
            )
        }
    }
}

private fun chooseDetourOffset(
    availableExtent: Double,
    baseDetour: Double,
    directLength: Double,
    randomness: Double
): Double {
    val lowerBound = baseDetour * 0.75
    val upperBound = max(lowerBound, directLength * 0.62)
    val preferredOffset = if (availableExtent > 0.0) {
        max(baseDetour * 0.75, availableExtent * 0.85)
    } else {
        baseDetour
    }
    val clampedPreferred = preferredOffset.coerceIn(lowerBound, upperBound)
    val randomLowerBound = max(lowerBound, clampedPreferred * blendDouble(0.90, 0.35, randomness))
    val randomUpperBound = min(
        upperBound,
        max(randomLowerBound, clampedPreferred * blendDouble(1.10, 1.95, randomness))
    )

    if (randomUpperBound - randomLowerBound < 0.0001) {
        return clampedPreferred
    }

    return Random.nextDouble(randomLowerBound, randomUpperBound)
}

private fun buildOffsetRouteVariant(
    shape: AutoRouteShape,
    start: HoldCenterPoint,
    goal: HoldCenterPoint,
    firstProgress: Double,
    midpointProgress: Double,
    secondProgress: Double,
    firstOffset: Double,
    midpointOffset: Double,
    secondOffset: Double,
    minimumWidth: Double,
    randomness: Double
): RouteVariant {
    val maximumOffset = max(
        abs(firstOffset),
        max(abs(midpointOffset), abs(secondOffset))
    )
    val randomizedMinimumWidth = minimumWidth * Random.nextDouble(
        blendDouble(0.90, 0.58, randomness),
        blendDouble(1.10, 1.92, randomness)
    )
    val adaptiveWidth =
        maximumOffset * Random.nextDouble(
            blendDouble(0.45, 0.18, randomness),
            blendDouble(0.75, 1.22, randomness)
        ) +
            Random.nextDouble(
                blendDouble(28.0, 18.0, randomness),
                blendDouble(52.0, 72.0, randomness)
            )

    return RouteVariant(
        shape = shape,
        pathPoints = listOf(
            start.toRoutePoint(),
            interpolateOffsetRoutePoint(start, goal, firstProgress, firstOffset),
            interpolateOffsetRoutePoint(start, goal, midpointProgress, midpointOffset),
            interpolateOffsetRoutePoint(start, goal, secondProgress, secondOffset),
            goal.toRoutePoint()
        ),
        corridorWidth = max(randomizedMinimumWidth, adaptiveWidth)
    )
}

private fun interpolateOffsetRoutePoint(
    start: HoldCenterPoint,
    goal: HoldCenterPoint,
    progress: Double,
    offset: Double
): RoutePoint {
    val clampedProgress = progress.coerceIn(0.0, 1.0)
    val directDx = goal.x - start.x
    val directDy = goal.y - start.y
    val directLength = hypot(directDx, directDy).coerceAtLeast(1.0)
    val perpendicularX = -directDy / directLength
    val perpendicularY = directDx / directLength

    val baseX = start.x + directDx * clampedProgress
    val baseY = start.y + directDy * clampedProgress

    return RoutePoint(
        x = baseX + perpendicularX * offset,
        y = baseY + perpendicularY * offset
    )
}

private fun createTargetDistances(
    routeLength: Double,
    intermediateCount: Int,
    randomness: Double,
    deterministic: Boolean
): List<Double> {
    if (intermediateCount <= 0) return emptyList()

    val spacing = routeLength / (intermediateCount + 1)
    val minGap = spacing * if (deterministic) {
        0.26
    } else {
        Random.nextDouble(
            blendDouble(0.24, 0.03, randomness),
            blendDouble(0.28, 0.22, randomness)
        )
    }
    val segmentWeights = if (deterministic) {
        List(intermediateCount + 1) { 1.0 }
    } else {
        List(intermediateCount + 1) {
            Random.nextDouble(
                blendDouble(0.90, 0.12, randomness),
                blendDouble(1.20, 3.00, randomness)
            )
        }
    }
    val totalWeight = segmentWeights.sum().coerceAtLeast(0.0001)
    val targetDistances = mutableListOf<Double>()
    var previousDistance = 0.0
    var accumulatedDistance = 0.0

    for (index in 0 until intermediateCount) {
        accumulatedDistance += routeLength * (segmentWeights[index] / totalWeight)
        val baseDistance = accumulatedDistance
        val remainingSlots = intermediateCount - index - 1
        val jitter = if (deterministic) {
            0.0
        } else {
            val jitterScale = blendDouble(0.10, 0.92, randomness)
            Random.nextDouble(-spacing * jitterScale, spacing * jitterScale)
        }
        val minAllowed = previousDistance + minGap
        val maxAllowed = routeLength - minGap * (remainingSlots + 1)
        val safeUpperBound = max(minAllowed, maxAllowed)
        val targetDistance = (baseDistance + jitter).coerceIn(minAllowed, safeUpperBound)
        targetDistances += targetDistance
        previousDistance = targetDistance
    }

    return targetDistances
}

private fun pickRouteCandidate(
    scoredCandidates: List<Pair<RoutedHoldCandidate, Double>>,
    randomness: Double,
    deterministic: Boolean
): RoutedHoldCandidate {
    val choicePool = scoredCandidates.take(min(blendInt(3, 10, randomness), scoredCandidates.size))
    if (choicePool.isEmpty()) {
        return scoredCandidates.first().first
    }
    if (deterministic || choicePool.size == 1) {
        return choicePool.first().first
    }

    val bestScore = choicePool.first().second
    val weightedPool = choicePool.map { (candidate, score) ->
        val normalizedScore = ((score - bestScore) / (abs(bestScore) + 1.0)).coerceAtLeast(0.0)
        candidate to (
            1.0 / (1.0 + normalizedScore * blendDouble(2.60, 0.72, randomness))
            )
    }

    return pickWeighted(weightedPool)
}

private fun selectRouteAttempt(
    attempts: List<RouteAttempt>,
    randomness: Double
): RouteAttempt? {
    if (attempts.isEmpty()) return null

    val uniqueAttempts = attempts
        .distinctBy { it.shape to it.indices }
        .map { attempt ->
            val noisyScore = attempt.score + if (attempt.deterministic) {
                Random.nextDouble(
                    blendDouble(0.0, 18.0, randomness),
                    blendDouble(8.0, 42.0, randomness)
                )
            } else {
                Random.nextDouble(
                    -blendDouble(2.0, 18.0, randomness),
                    blendDouble(4.0, 28.0, randomness)
                )
            }
            attempt to noisyScore
        }
        .sortedBy { (_, noisyScore) -> noisyScore }
    val pool = uniqueAttempts.take(min(blendInt(4, 14, randomness), uniqueAttempts.size))
    if (pool.size == 1) return pool.first().first

    val weightedPool = pool.mapIndexed { index, (attempt, noisyScore) ->
        val noiseBoost = Random.nextDouble(
            blendDouble(0.95, 0.78, randomness),
            blendDouble(1.05, 1.32, randomness)
        )
        val scoreBoost = 1.0 / (
            1.0 + max(0.0, noisyScore - pool.first().second) * blendDouble(0.08, 0.012, randomness)
            )
        attempt to (noiseBoost * scoreBoost / (1.0 + index * blendDouble(0.85, 0.22, randomness)))
    }

    return pickWeighted(weightedPool)
}

private fun <T> pickWeighted(weightedItems: List<Pair<T, Double>>): T {
    if (weightedItems.size == 1) return weightedItems.first().first

    val totalWeight = weightedItems.sumOf { (_, weight) -> weight.coerceAtLeast(0.0) }
        .coerceAtLeast(0.0001)
    var draw = Random.nextDouble(totalWeight)

    weightedItems.forEach { (item, weight) ->
        draw -= weight.coerceAtLeast(0.0)
        if (draw <= 0.0) {
            return item
        }
    }

    return weightedItems.last().first
}

private fun scoreGeneratedRoute(
    routePoints: List<HoldCenterPoint>,
    expectedStepDistance: Double,
    routeLength: Int,
    adherenceDistances: List<Double>,
    availableCandidateCount: Int,
    randomness: Double
): Double {
    if (routePoints.size != routeLength || routePoints.size < 2) return Double.MAX_VALUE

    val distances = routePoints.zipWithNext { first, second ->
        hypot(second.x - first.x, second.y - first.y)
    }
    val averageDistance = distances.average()
    val spacingPenalty = distances.sumOf { distance ->
        abs(distance - expectedStepDistance) * blendDouble(0.32, 0.16, randomness)
    }
    val adherencePenalty = adherenceDistances.sumOf { distance ->
        distance * blendDouble(0.52, 0.42, randomness)
    }
    val densityBonus = min(availableCandidateCount, 10) * 3.4
    val spacingVarietyBonus = distances.sumOf { distance ->
        abs(distance - averageDistance) * blendDouble(0.04, 0.34, randomness)
    }

    return spacingPenalty + adherencePenalty - densityBonus - spacingVarietyBonus
}

private fun signedDistanceFromLine(
    point: RoutePoint,
    lineStart: RoutePoint,
    lineEnd: RoutePoint
): Double {
    val lineDx = lineEnd.x - lineStart.x
    val lineDy = lineEnd.y - lineStart.y
    val lineLength = hypot(lineDx, lineDy).coerceAtLeast(1.0)

    return ((point.x - lineStart.x) * lineDy - (point.y - lineStart.y) * lineDx) / lineLength
}

private fun calculatePolylineLength(polyline: List<RoutePoint>): Double {
    if (polyline.size < 2) return 0.0

    return polyline.zipWithNext().sumOf { (start, end) ->
        hypot(end.x - start.x, end.y - start.y)
    }
}

private fun projectPointOntoPolyline(
    point: RoutePoint,
    polyline: List<RoutePoint>
): PolylineProjection? {
    if (polyline.size < 2) return null

    var traversedDistance = 0.0
    var bestProjection: PolylineProjection? = null

    polyline.zipWithNext().forEach { (segmentStart, segmentEnd) ->
        val segmentDx = segmentEnd.x - segmentStart.x
        val segmentDy = segmentEnd.y - segmentStart.y
        val segmentLengthSquared = max(segmentDx * segmentDx + segmentDy * segmentDy, 0.0001)
        val segmentLength = hypot(segmentDx, segmentDy)
        val rawT = ((point.x - segmentStart.x) * segmentDx + (point.y - segmentStart.y) * segmentDy) /
            segmentLengthSquared
        val t = rawT.coerceIn(0.0, 1.0)
        val projectedX = segmentStart.x + segmentDx * t
        val projectedY = segmentStart.y + segmentDy * t
        val distanceToRoute = hypot(point.x - projectedX, point.y - projectedY)
        val distanceAlongRoute = traversedDistance + segmentLength * t

        val candidateProjection = PolylineProjection(
            distanceAlongRoute = distanceAlongRoute,
            distanceToRoute = distanceToRoute
        )
        val bestDistance = bestProjection?.distanceToRoute ?: Double.MAX_VALUE
        if (distanceToRoute < bestDistance) {
            bestProjection = candidateProjection
        }

        traversedDistance += segmentLength
    }

    return bestProjection
}

private fun Hold.toCenterPoint(index: Int): HoldCenterPoint {
    return HoldCenterPoint(
        index = index,
        x = centerX.toDouble(),
        y = centerY.toDouble()
    )
}

private fun HoldCenterPoint.toRoutePoint(): RoutePoint {
    return RoutePoint(x = x, y = y)
}

private fun ReachCalibrationReference.toReachConstraint(): ReachConstraint? {
    val calibrationDistancePx = hypot(
        (secondPoint.x - firstPoint.x).toDouble(),
        (secondPoint.y - firstPoint.y).toDouble()
    )
    if (calibrationDistancePx <= 0.0) return null

    val pixelsPerCentimeter = calibrationDistancePx / 150.0
    return ReachConstraint(
        horizontalReachPx = pixelsPerCentimeter * 120.0,
        verticalReachPx = pixelsPerCentimeter * 90.0
    )
}
