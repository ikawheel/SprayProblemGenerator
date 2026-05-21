package com.ikeansoft.sprayproblemgenerator.ui.selectors

import androidx.annotation.StringRes
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState

internal data class HoldEditorUiModel(
    @StringRes val titleResId: Int,
    @StringRes val saveButtonTextResId: Int,
    val reachReferenceLengthCm: Int?,
    val hasSelectedHold: Boolean,
    val selectedHoldIsStartCandidate: Boolean,
    val selectedHoldIsGoalCandidate: Boolean
)

internal fun deriveHoldEditorUiModel(state: MainUiState): HoldEditorUiModel {
    val reference = state.reachCalibrationReference
    val selectedHold = state.selectedHoldIndex?.let(state.holds::getOrNull)

    return HoldEditorUiModel(
        titleResId = if (state.currentWallId == null) {
            R.string.hold_editor_title_create
        } else {
            R.string.hold_editor_title_edit
        },
        saveButtonTextResId = if (state.currentWallId == null) {
            R.string.save
        } else {
            R.string.overwrite_save
        },
        reachReferenceLengthCm = reference?.referenceLengthCm,
        hasSelectedHold = selectedHold != null,
        selectedHoldIsStartCandidate = selectedHold?.isStartCandidate == true,
        selectedHoldIsGoalCandidate = selectedHold?.isGoalCandidate == true
    )
}
