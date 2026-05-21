package com.ikeansoft.sprayproblemgenerator.ui.selectors

import com.ikeansoft.sprayproblemgenerator.domain.hold.buildHoldScoringOrder
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState

internal data class HoldScoringUiModel(
    val orderedIndices: List<Int>,
    val currentHoldIndex: Int?,
    val currentDifficultyScore: Int?,
    val currentPosition: Int,
    val totalCount: Int,
    val isCompleted: Boolean
)

internal fun deriveHoldScoringUiModel(state: MainUiState): HoldScoringUiModel {
    val orderedIndices = buildHoldScoringOrder(state.holds)
    val totalCount = orderedIndices.size
    val isCompleted = totalCount == 0 || state.holdScoringPosition >= totalCount
    val currentHoldIndex = if (isCompleted) {
        orderedIndices.lastOrNull()
    } else {
        orderedIndices.getOrNull(state.holdScoringPosition)
    }

    return HoldScoringUiModel(
        orderedIndices = orderedIndices,
        currentHoldIndex = currentHoldIndex,
        currentDifficultyScore = currentHoldIndex
            ?.let { index -> state.holds.getOrNull(index) }
            ?.difficultyScore,
        currentPosition = if (totalCount == 0) 0 else (state.holdScoringPosition + 1).coerceAtMost(totalCount),
        totalCount = totalCount,
        isCompleted = isCompleted
    )
}
