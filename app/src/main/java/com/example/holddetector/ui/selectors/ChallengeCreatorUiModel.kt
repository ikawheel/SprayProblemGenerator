package com.example.holddetector.ui.selectors

import androidx.annotation.StringRes
import com.example.holddetector.R
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.RouteSelectionMode

internal data class ChallengeCreatorUiModel(
    val selectionCandidateIndices: Set<Int>,
    val isReadyToGenerate: Boolean,
    val canStartGoalSelection: Boolean,
    @StringRes val helpTextResId: Int,
    @StringRes val drawTargetButtonTextResId: Int,
    @StringRes val startGoalButtonTextResId: Int,
    @StringRes val startStatusResId: Int,
    @StringRes val goalStatusResId: Int,
    val drawTargetStatus: DrawTargetStatus
)

internal sealed interface DrawTargetStatus {
    object Selecting : DrawTargetStatus
    object All : DrawTargetStatus
    data class Count(val count: Int) : DrawTargetStatus
}

internal fun deriveChallengeCreatorUiModel(state: MainUiState): ChallengeCreatorUiModel {
    val selectionCandidateIndices = if (state.hasDrawTargetSelection) {
        state.drawTargetHoldIndices
    } else {
        state.holds.indices.toSet()
    }

    val isReadyToGenerate = !state.isDrawTargetSelectionMode &&
        state.routeSelectionMode == RouteSelectionMode.NONE &&
        state.startHoldIndex != null &&
        state.goalHoldIndex != null &&
        (state.drawCountInput.toIntOrNull() ?: 0) >= 2

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
        else -> DrawTargetStatus.Count(state.drawTargetHoldIndices.size)
    }

    val startGoalButtonTextResId = when (state.routeSelectionMode) {
        RouteSelectionMode.NONE -> R.string.start_goal_select
        RouteSelectionMode.SELECTING_START -> R.string.selecting_start
        RouteSelectionMode.SELECTING_GOAL -> R.string.selecting_goal
    }

    return ChallengeCreatorUiModel(
        selectionCandidateIndices = selectionCandidateIndices,
        isReadyToGenerate = isReadyToGenerate,
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
        drawTargetStatus = drawTargetStatus
    )
}
