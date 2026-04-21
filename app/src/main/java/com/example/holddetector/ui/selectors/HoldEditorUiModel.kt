package com.example.holddetector.ui.selectors

import androidx.annotation.StringRes
import com.example.holddetector.R
import com.example.holddetector.ui.MainUiState

internal data class HoldEditorUiModel(
    @StringRes val titleResId: Int,
    @StringRes val saveButtonTextResId: Int,
    val reachReferenceLengthCm: Int?
)

internal fun deriveHoldEditorUiModel(state: MainUiState): HoldEditorUiModel {
    val reference = state.reachCalibrationReference

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
        reachReferenceLengthCm = reference?.referenceLengthCm
    )
}
