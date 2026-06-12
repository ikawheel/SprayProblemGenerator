package com.ikeansoft.sprayproblemgenerator.ui

import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.domain.challenge.ChallengeRouteGenerator
import com.ikeansoft.sprayproblemgenerator.domain.challenge.RouteGenerationTuning
import com.ikeansoft.sprayproblemgenerator.domain.challenge.normalizeChallengeRouteOrder
import com.ikeansoft.sprayproblemgenerator.model.DEFAULT_HOLD_DIFFICULTY_SCORE
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.model.ReachCalibrationReference
import kotlin.math.roundToInt
import kotlin.random.Random

internal data class ChallengeMessageResult(
    val state: MainUiState,
    val message: String? = null
)

internal data class RandomStartGoalGenerationResult(
    val startIndex: Int,
    val goalIndex: Int,
    val orderedIndices: List<Int>
)

internal fun RouteGenerationTuning.randomizedRouteShape(): RouteGenerationTuning {
    return copy(
        detourStrength = Random.Default.nextFloat(),
        routeWaviness = Random.Default.nextFloat(),
        stepDistanceVariance = Random.Default.nextFloat(),
        corridorWidth = Random.Default.nextFloat()
    )
}

internal fun challengeSelectionCandidateIndices(state: MainUiState): Set<Int> {
    val baseIndices = if (state.hasDrawTargetSelection) {
        state.drawTargetHoldIndices
    } else {
        state.holds.indices.toSet()
    }
    return filterChallengeEligibleIndices(
        holds = state.holds,
        indices = baseIndices,
        minScore = state.challengeDifficultyScoreMin,
        maxScore = state.challengeDifficultyScoreMax
    )
}

internal fun filterChallengeEligibleIndices(
    holds: List<Hold>,
    indices: Set<Int>,
    minScore: Int,
    maxScore: Int
): Set<Int> {
    return indices.filterTo(linkedSetOf()) { index ->
        val score = holds.getOrNull(index)?.difficultyScore ?: DEFAULT_HOLD_DIFFICULTY_SCORE
        score in minScore..maxScore
    }
}

internal fun buildChallengeHoldTappedResult(
    state: MainUiState,
    index: Int?,
    text: (Int, Array<out Any>) -> String
): ChallengeMessageResult {
    if (state.isDrawTargetSelectionMode) return ChallengeMessageResult(state)
    val selectionCandidateIndices = challengeSelectionCandidateIndices(state)

    return when (state.routeSelectionMode) {
        RouteSelectionMode.SELECTING_START -> {
            if (index == null || index !in selectionCandidateIndices) {
                ChallengeMessageResult(
                    state = state,
                    message = text(R.string.message_select_start_from_candidates, emptyArray())
                )
            } else {
                ChallengeMessageResult(
                    state = state.copy(
                        selectedHoldIndex = index,
                        startHoldIndex = index,
                        goalHoldIndex = null,
                        challengeHoldIndices = emptySet(),
                        challengeOrderedHoldIndices = emptyList(),
                        lastGeneratedIntermediateHoldIndices = emptySet(),
                        routeSelectionMode = RouteSelectionMode.SELECTING_GOAL
                    ),
                    message = text(R.string.message_start_set_next_goal, emptyArray())
                )
            }
        }

        RouteSelectionMode.SELECTING_GOAL -> {
            if (index == null || index !in selectionCandidateIndices) {
                ChallengeMessageResult(
                    state = state,
                    message = text(R.string.message_select_goal_from_candidates, emptyArray())
                )
            } else if (index == state.startHoldIndex) {
                ChallengeMessageResult(
                    state = state,
                    message = text(R.string.message_start_goal_must_differ, emptyArray())
                )
            } else {
                ChallengeMessageResult(
                    state = state.copy(
                        selectedHoldIndex = index,
                        goalHoldIndex = index,
                        challengeHoldIndices = emptySet(),
                        challengeOrderedHoldIndices = emptyList(),
                        lastGeneratedIntermediateHoldIndices = emptySet(),
                        routeSelectionMode = RouteSelectionMode.NONE
                    ),
                    message = text(R.string.message_goal_set, emptyArray())
                )
            }
        }

        RouteSelectionMode.NONE -> {
            if (index == null || state.challengeHoldIndices.isEmpty() || index !in selectionCandidateIndices) {
                ChallengeMessageResult(state)
            } else {
                val updated = state.challengeHoldIndices.toMutableSet()
                val added = if (updated.contains(index)) {
                    updated.remove(index)
                    false
                } else {
                    updated.add(index)
                    true
                }
                val updatedOrder = normalizeChallengeRouteOrder(
                    challengeIndices = updated,
                    preferredOrder = state.challengeOrderedHoldIndices,
                    holds = state.holds,
                    startIndex = state.startHoldIndex,
                    goalIndex = state.goalHoldIndex
                )
                ChallengeMessageResult(
                    state = state.copy(
                        selectedHoldIndex = index,
                        challengeHoldIndices = updated,
                        challengeOrderedHoldIndices = updatedOrder,
                        startHoldIndex = if (state.startHoldIndex == index && !added) null else state.startHoldIndex,
                        goalHoldIndex = if (state.goalHoldIndex == index && !added) null else state.goalHoldIndex
                    ),
                    message = if (added) {
                        text(R.string.message_challenge_hold_added, emptyArray())
                    } else {
                        text(R.string.message_challenge_hold_removed, emptyArray())
                    }
                )
            }
        }
    }
}

internal fun buildChallengeDifficultyRangeState(
    state: MainUiState,
    start: Float,
    endInclusive: Float
): MainUiState {
    val minScore = start.roundToInt().coerceIn(
        com.ikeansoft.sprayproblemgenerator.model.MIN_HOLD_DIFFICULTY_SCORE,
        com.ikeansoft.sprayproblemgenerator.model.MAX_HOLD_DIFFICULTY_SCORE
    )
    val maxScore = endInclusive.roundToInt().coerceIn(
        com.ikeansoft.sprayproblemgenerator.model.MIN_HOLD_DIFFICULTY_SCORE,
        com.ikeansoft.sprayproblemgenerator.model.MAX_HOLD_DIFFICULTY_SCORE
    )
    val normalizedMin = minOf(minScore, maxScore)
    val normalizedMax = maxOf(minScore, maxScore)
    val filteredChallengeIndices = filterChallengeEligibleIndices(
        holds = state.holds,
        indices = state.challengeHoldIndices,
        minScore = normalizedMin,
        maxScore = normalizedMax
    )
    val filteredStartIndex = state.startHoldIndex?.takeIf { index ->
        index in filterChallengeEligibleIndices(
            holds = state.holds,
            indices = setOf(index),
            minScore = normalizedMin,
            maxScore = normalizedMax
        )
    }
    val filteredGoalIndex = state.goalHoldIndex?.takeIf { index ->
        index in filterChallengeEligibleIndices(
            holds = state.holds,
            indices = setOf(index),
            minScore = normalizedMin,
            maxScore = normalizedMax
        )
    }
    val normalizedRouteSelectionMode = when (state.routeSelectionMode) {
        RouteSelectionMode.SELECTING_START -> RouteSelectionMode.SELECTING_START
        RouteSelectionMode.SELECTING_GOAL -> if (filteredStartIndex != null) {
            RouteSelectionMode.SELECTING_GOAL
        } else {
            RouteSelectionMode.SELECTING_START
        }
        RouteSelectionMode.NONE -> RouteSelectionMode.NONE
    }

    return state.copy(
        challengeDifficultyScoreMin = normalizedMin,
        challengeDifficultyScoreMax = normalizedMax,
        challengeHoldIndices = filteredChallengeIndices,
        challengeOrderedHoldIndices = normalizeChallengeRouteOrder(
            challengeIndices = filteredChallengeIndices,
            preferredOrder = state.challengeOrderedHoldIndices,
            holds = state.holds,
            startIndex = filteredStartIndex,
            goalIndex = filteredGoalIndex
        ),
        startHoldIndex = filteredStartIndex,
        goalHoldIndex = filteredGoalIndex,
        selectedHoldIndex = state.selectedHoldIndex?.takeIf { index ->
            index in filterChallengeEligibleIndices(
                holds = state.holds,
                indices = setOf(index),
                minScore = normalizedMin,
                maxScore = normalizedMax
            )
        },
        routeSelectionMode = normalizedRouteSelectionMode,
        lastGeneratedIntermediateHoldIndices = state.lastGeneratedIntermediateHoldIndices.filterTo(linkedSetOf()) { index ->
            index in filterChallengeEligibleIndices(
                holds = state.holds,
                indices = setOf(index),
                minScore = normalizedMin,
                maxScore = normalizedMax
            )
        }
    )
}

internal fun buildChallengeFlowState(
    state: MainUiState,
    targetStep: ChallengeFlowStep,
    pushedChallengeFlowBackStack: (MainUiState, ChallengeFlowStep) -> List<ChallengeFlowStep>
): MainUiState {
    return state.copy(
        challengeFlowStep = targetStep,
        challengeFlowBackStack = pushedChallengeFlowBackStack(state, targetStep),
        selectedHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        isDrawTargetSelectionMode = false,
        message = null
    )
}

internal fun buildChallengeGenerationStateResult(
    state: MainUiState,
    pushedChallengeFlowBackStack: (MainUiState, ChallengeFlowStep) -> List<ChallengeFlowStep>,
    selectMethodMessage: String
): ChallengeMessageResult {
    if (state.challengeGenerationMethod == null) {
        return ChallengeMessageResult(
            state = state,
            message = selectMethodMessage
        )
    }
    return ChallengeMessageResult(
        state = buildChallengeFlowState(
            state = state,
            targetStep = ChallengeFlowStep.GENERATION,
            pushedChallengeFlowBackStack = pushedChallengeFlowBackStack
        )
    )
}

internal fun buildSelectedChallengeGenerationMethodState(
    state: MainUiState,
    method: ChallengeGenerationMethod,
    pushedChallengeFlowBackStack: (MainUiState, ChallengeFlowStep) -> List<ChallengeFlowStep>
): MainUiState {
    val targetStep = if (state.challengeFlowStep == ChallengeFlowStep.COMMON_SETTINGS) {
        ChallengeFlowStep.GENERATION
    } else {
        ChallengeFlowStep.COMMON_SETTINGS
    }
    return state.copy(
        challengeGenerationMethod = method,
        challengeFlowStep = targetStep,
        challengeFlowBackStack = pushedChallengeFlowBackStack(state, targetStep),
        selectedHoldIndex = null,
        challengeHoldIndices = emptySet(),
        challengeOrderedHoldIndices = emptyList(),
        lastGeneratedIntermediateHoldIndices = emptySet(),
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        isDrawTargetSelectionMode = false,
        message = null
    )
}

internal fun buildDrawTargetSelectionState(
    state: MainUiState,
    message: String
): MainUiState {
    return state.copy(
        selectedHoldIndex = null,
        challengeHoldIndices = emptySet(),
        challengeOrderedHoldIndices = emptyList(),
        lastGeneratedIntermediateHoldIndices = emptySet(),
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        isDrawTargetSelectionMode = true,
        message = message
    )
}

internal fun buildAppliedDrawTargetSelectionState(
    state: MainUiState,
    indices: Set<Int>,
    emptyMessage: String,
    selectedCountMessage: (Int) -> String
): MainUiState {
    val eligibleIndices = filterChallengeEligibleIndices(
        holds = state.holds,
        indices = indices,
        minScore = state.challengeDifficultyScoreMin,
        maxScore = state.challengeDifficultyScoreMax
    )
    return state.copy(
        selectedHoldIndex = null,
        challengeHoldIndices = emptySet(),
        challengeOrderedHoldIndices = emptyList(),
        lastGeneratedIntermediateHoldIndices = emptySet(),
        drawTargetHoldIndices = indices,
        hasDrawTargetSelection = true,
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        isDrawTargetSelectionMode = false,
        message = if (eligibleIndices.isEmpty()) emptyMessage else selectedCountMessage(eligibleIndices.size)
    )
}

internal fun buildChallengeStartGoalSelectionResult(
    state: MainUiState,
    finishRangeSelectionFirstMessage: String,
    selectCandidateHoldsFirstMessage: String,
    selectStartPromptMessage: String
): ChallengeMessageResult {
    if (state.isDrawTargetSelectionMode) {
        return ChallengeMessageResult(state = state, message = finishRangeSelectionFirstMessage)
    }
    if (challengeSelectionCandidateIndices(state).isEmpty()) {
        return ChallengeMessageResult(state = state, message = selectCandidateHoldsFirstMessage)
    }
    return ChallengeMessageResult(
        state = state.copy(
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            lastGeneratedIntermediateHoldIndices = emptySet(),
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.SELECTING_START,
            message = selectStartPromptMessage
        )
    )
}

internal fun buildClearedChallengeSelectionState(
    state: MainUiState,
    message: String
): MainUiState {
    return state.copy(
        selectedHoldIndex = null,
        challengeHoldIndices = emptySet(),
        challengeOrderedHoldIndices = emptyList(),
        lastGeneratedIntermediateHoldIndices = emptySet(),
        drawTargetHoldIndices = emptySet(),
        hasDrawTargetSelection = false,
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        isDrawTargetSelectionMode = false,
        message = message
    )
}

internal fun generateChallengeRouteWithRetries(
    holds: List<Hold>,
    sourceIndices: Set<Int>,
    startIndex: Int,
    goalIndex: Int,
    targetCount: Int?,
    tuning: RouteGenerationTuning,
    reachCalibrationReference: ReachCalibrationReference?
): List<Int>? {
    val maximumAttempts = tuning.routeGenerationAttemptLimit.coerceAtLeast(1)

    repeat(maximumAttempts) {
        ChallengeRouteGenerator.generate(
            holds = holds,
            sourceIndices = sourceIndices,
            startIndex = startIndex,
            goalIndex = goalIndex,
            targetCount = targetCount,
            tuning = tuning,
            reachCalibrationReference = reachCalibrationReference
        )?.let { generatedRoute ->
            return generatedRoute
        }
    }

    return null
}

internal fun generateChallengeRouteWithRandomStartGoal(
    holds: List<Hold>,
    selectionCandidateIndices: Set<Int>,
    lastGeneratedIntermediateHoldIndices: Set<Int>,
    targetCount: Int?,
    tuning: RouteGenerationTuning,
    reachCalibrationReference: ReachCalibrationReference?
): RandomStartGoalGenerationResult? {
    val filteredSelectionCandidateIndices = if (tuning.excludePreviouslyGeneratedHolds) {
        selectionCandidateIndices
            .filterNotTo(linkedSetOf()) { it in lastGeneratedIntermediateHoldIndices }
            .takeIf { it.size >= 2 }
            ?: selectionCandidateIndices
    } else {
        selectionCandidateIndices
    }

    val preferredStartIndices = filteredSelectionCandidateIndices.filterTo(linkedSetOf()) { index ->
        holds.getOrNull(index)?.isStartCandidate == true
    }
    val preferredGoalIndices = filteredSelectionCandidateIndices.filterTo(linkedSetOf()) { index ->
        holds.getOrNull(index)?.isGoalCandidate == true
    }
    val preferredPairs = buildDistinctStartGoalPairs(
        startIndices = if (preferredStartIndices.isNotEmpty()) preferredStartIndices else filteredSelectionCandidateIndices,
        goalIndices = if (preferredGoalIndices.isNotEmpty()) preferredGoalIndices else filteredSelectionCandidateIndices
    )
    val candidatePairs = if (preferredPairs.isNotEmpty()) {
        preferredPairs
    } else {
        buildDistinctStartGoalPairs(
            startIndices = filteredSelectionCandidateIndices,
            goalIndices = filteredSelectionCandidateIndices
        )
    }
    if (candidatePairs.isEmpty()) return null

    candidatePairs
        .shuffled(Random.Default)
        .take(tuning.randomStartGoalPairLimit.coerceAtLeast(1))
        .forEach { (startIndex, goalIndex) ->
            val drawSourceIndices = selectionCandidateIndices.toMutableSet().apply {
                if (tuning.excludePreviouslyGeneratedHolds) {
                    removeAll(lastGeneratedIntermediateHoldIndices)
                }
                add(startIndex)
                add(goalIndex)
            }
            val orderedIndices = generateChallengeRouteWithRetries(
                holds = holds,
                sourceIndices = drawSourceIndices,
                startIndex = startIndex,
                goalIndex = goalIndex,
                targetCount = targetCount,
                tuning = tuning,
                reachCalibrationReference = reachCalibrationReference
            )
            if (orderedIndices != null) {
                return RandomStartGoalGenerationResult(
                    startIndex = startIndex,
                    goalIndex = goalIndex,
                    orderedIndices = orderedIndices
                )
            }
        }

    return null
}

internal fun buildDistinctStartGoalPairs(
    startIndices: Set<Int>,
    goalIndices: Set<Int>
): List<Pair<Int, Int>> {
    return buildList {
        startIndices.forEach { startIndex ->
            goalIndices.forEach { goalIndex ->
                if (startIndex != goalIndex) {
                    add(startIndex to goalIndex)
                }
            }
        }
    }
}
