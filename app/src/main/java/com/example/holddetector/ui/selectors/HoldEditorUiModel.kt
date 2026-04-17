package com.example.holddetector.ui.selectors

import androidx.annotation.StringRes
import com.example.holddetector.R
import com.example.holddetector.ui.MainUiState

private const val ReachStatusConfigured = "150cm\u57fa\u6e96: \u8a2d\u5b9a\u6e08\u307f"
private const val ReachStatusUnset = "150cm\u57fa\u6e96: \u672a\u8a2d\u5b9a"
private const val ReachButtonSetup = "150cm\u8a2d\u5b9a\u3078"
private const val ReachButtonOpenScreen = "150cm\u8a2d\u5b9a\u753b\u9762\u3092\u958b\u304f"

internal data class HoldEditorUiModel(
    @StringRes val titleResId: Int,
    @StringRes val saveButtonTextResId: Int,
    val reachStatusText: String,
    val reachButtonText: String
)

internal fun deriveHoldEditorUiModel(state: MainUiState): HoldEditorUiModel {
    val isConfigured = state.reachCalibrationReference != null

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
        reachStatusText = if (isConfigured) {
            ReachStatusConfigured
        } else {
            ReachStatusUnset
        },
        reachButtonText = if (isConfigured) {
            ReachButtonOpenScreen
        } else {
            ReachButtonSetup
        }
    )
}
