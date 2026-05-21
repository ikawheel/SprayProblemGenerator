package com.ikeansoft.sprayproblemgenerator.ui.selectors

import androidx.annotation.StringRes
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.domain.challenge.ChallengeDifficultyCalculator
import com.ikeansoft.sprayproblemgenerator.domain.challenge.normalizeChallengeRouteOrder
import com.ikeansoft.sprayproblemgenerator.model.DEFAULT_HOLD_DIFFICULTY_SCORE
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState
import com.ikeansoft.sprayproblemgenerator.ui.RouteSelectionMode
import kotlin.math.hypot
import kotlin.math.roundToInt

internal data class ChallengeDebugSummaryRow(
    val stepNumber: Int,
    val totalDifficulty: Double,
    val distanceCentimeters: Int?,
    val previousHoldDifficulty: Int,
    val nextHoldDifficulty: Int,
    val distanceMultiplier: Double,
    val isCore: Boolean
)

internal data class ChallengeCreatorUiModel(
    val selectionCandidateIndices: Set<Int>,
    val orderedChallengeIndices: List<Int>,
    val isReadyToGenerate: Boolean,
    val canAutoGenerateWithRandomStartGoal: Boolean,
    val canStartGoalSelection: Boolean,
    @StringRes val helpTextResId: Int,
    @StringRes val drawTargetButtonTextResId: Int,
    @StringRes val startGoalButtonTextResId: Int,
    @StringRes val startStatusResId: Int,
    @StringRes val goalStatusResId: Int,
    val drawTargetStatus: DrawTargetStatus,
    val challengeDifficultyScore: Double?,
    val coreMoveDifficulty: Double?,
    val coreChallengeHoldIndex: Int?,
    val challengeDebugSummaryRows: List<ChallengeDebugSummaryRow>
)

internal sealed interface DrawTargetStatus {
    object Selecting : DrawTargetStatus
    object All : DrawTargetStatus
    data class Count(val count: Int) : DrawTargetStatus
}

internal fun deriveChallengeCreatorUiModel(state: MainUiState): ChallengeCreatorUiModel {
    val baseSelectionCandidateIndices = if (state.hasDrawTargetSelection) {
        state.drawTargetHoldIndices
    } else {
        state.holds.indices.toSet()
    }
    val selectionCandidateIndices = baseSelectionCandidateIndices.filterTo(linkedSetOf()) { index ->
        val score = state.holds.getOrNull(index)?.difficultyScore ?: DEFAULT_HOLD_DIFFICULTY_SCORE
        score in state.challengeDifficultyScoreMin..state.challengeDifficultyScoreMax
    }
    val orderedChallengeIndices = normalizeChallengeRouteOrder(
        challengeIndices = state.challengeHoldIndices,
        preferredOrder = state.challengeOrderedHoldIndices,
        holds = state.holds,
        startIndex = state.startHoldIndex,
        goalIndex = state.goalHoldIndex
    )
    val challengeDifficulty = ChallengeDifficultyCalculator.calculate(
        holds = state.holds,
        orderedIndices = orderedChallengeIndices,
        reachCalibrationReference = state.reachCalibrationReference
    )
    val challengeDebugSummaryRows = buildChallengeDebugSummaryRows(
        state = state,
        challengeDifficultyScore = challengeDifficulty
    )

    val requestedDrawCount = state.drawCountInput.toIntOrNull()
    val hasValidRequestedDrawCount = state.drawCountInput.isBlank() ||
        requestedDrawCount == 0 ||
        (requestedDrawCount ?: 0) >= 2

    val isReadyToGenerate = !state.isDrawTargetSelectionMode &&
        state.routeSelectionMode == RouteSelectionMode.NONE &&
        state.startHoldIndex != null &&
        state.goalHoldIndex != null &&
        hasValidRequestedDrawCount
    val canAutoGenerateWithRandomStartGoal = !state.isDrawTargetSelectionMode &&
        state.routeSelectionMode == RouteSelectionMode.NONE &&
        selectionCandidateIndices.size >= 2

    val helpTextResId = when {
        state.isDrawTargetSelectionMode -> R.string.draw_target_status_selecting
        state.routeSelectionMode == RouteSelectionMode.SELECTING_START ->
            R.string.challenge_route_help_select_start
        state.routeSelectionMode == RouteSelectionMode.SELECTING_GOAL ->
            R.string.challenge_route_help_select_goal
        state.startHoldIndex != null && state.goalHoldIndex != null ->
            R.string.challenge_route_help_ready_generate
        else -> R.string.challenge_route_help_none
    }

    val drawTargetStatus = when {
        state.isDrawTargetSelectionMode -> DrawTargetStatus.Selecting
        !state.hasDrawTargetSelection -> DrawTargetStatus.All
        else -> DrawTargetStatus.Count(selectionCandidateIndices.size)
    }

    val startGoalButtonTextResId = when (state.routeSelectionMode) {
        RouteSelectionMode.NONE -> R.string.start_goal_select
        RouteSelectionMode.SELECTING_START -> R.string.selecting_start
        RouteSelectionMode.SELECTING_GOAL -> R.string.selecting_goal
    }

    return ChallengeCreatorUiModel(
        selectionCandidateIndices = selectionCandidateIndices,
        orderedChallengeIndices = orderedChallengeIndices,
        isReadyToGenerate = isReadyToGenerate,
        canAutoGenerateWithRandomStartGoal = canAutoGenerateWithRandomStartGoal,
        canStartGoalSelection = !state.isDrawTargetSelectionMode && selectionCandidateIndices.isNotEmpty(),
        helpTextResId = helpTextResId,
        drawTargetButtonTextResId = if (!state.hasDrawTargetSelection) {
            R.string.draw_target_select
        } else {
            R.string.draw_target_reselect
        },
        startGoalButtonTextResId = startGoalButtonTextResId,
        startStatusResId = if (state.startHoldIndex != null) {
            R.string.status_set
        } else {
            R.string.status_unset
        },
        goalStatusResId = if (state.goalHoldIndex != null) {
            R.string.status_set
        } else {
            R.string.status_unset
        },
        drawTargetStatus = drawTargetStatus,
        challengeDifficultyScore = challengeDifficulty?.totalDifficulty,
        coreMoveDifficulty = challengeDifficulty?.coreMoveDifficulty,
        coreChallengeHoldIndex = challengeDifficulty
            ?.moveDetails
            ?.getOrNull(challengeDifficulty.coreMoveIndex)
            ?.nextIndex,
        challengeDebugSummaryRows = challengeDebugSummaryRows
    )
}

private fun buildChallengeDebugSummaryRows(
    state: MainUiState,
    challengeDifficultyScore: com.ikeansoft.sprayproblemgenerator.domain.challenge.ChallengeDifficultyResult?
): List<ChallengeDebugSummaryRow> {
    val pixelsPerCentimeter = state.reachCalibrationReference?.pixelsPerCentimeterOrNull()

    return challengeDifficultyScore?.moveDetails?.mapIndexed { index, move ->
        val previousHold = state.holds.getOrNull(move.previousIndex)
        val nextHold = state.holds.getOrNull(move.nextIndex)
        val distanceCentimeters = if (
            pixelsPerCentimeter != null &&
            previousHold != null &&
            nextHold != null
        ) {
            val distanceCentimeters = hypot(
                (nextHold.centerX - previousHold.centerX).toDouble(),
                (nextHold.centerY - previousHold.centerY).toDouble()
            ) / pixelsPerCentimeter
            distanceCentimeters.roundToInt()
        } else {
            null
        }

        ChallengeDebugSummaryRow(
            stepNumber = index + 2,
            totalDifficulty = move.totalDifficulty,
            distanceCentimeters = distanceCentimeters,
            previousHoldDifficulty = move.previousHoldDifficulty,
            nextHoldDifficulty = move.nextHoldDifficulty,
            distanceMultiplier = move.distanceMultiplier,
            isCore = index == challengeDifficultyScore.coreMoveIndex
        )
    } ?: emptyList()
}

private fun com.ikeansoft.sprayproblemgenerator.model.ReachCalibrationReference.pixelsPerCentimeterOrNull(): Double? {
    val calibrationDistancePx = hypot(
        (secondPoint.x - firstPoint.x).toDouble(),
        (secondPoint.y - firstPoint.y).toDouble()
    )
    if (calibrationDistancePx <= 0.0) return null
    return calibrationDistancePx / referenceLengthCm.toDouble()
}
