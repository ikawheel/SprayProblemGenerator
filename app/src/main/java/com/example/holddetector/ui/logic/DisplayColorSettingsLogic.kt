package com.example.holddetector.ui

internal fun buildDisplayColorSettingsScreenState(
    state: MainUiState,
    pushedScreenBackStack: (MainUiState, AppScreen) -> List<AppScreen>
): MainUiState {
    return state.copy(
        currentScreen = AppScreen.DISPLAY_COLOR_SETTINGS,
        screenBackStack = pushedScreenBackStack(state, AppScreen.DISPLAY_COLOR_SETTINGS),
        message = null
    )
}

internal fun buildUpdatedDisplayColorSettings(
    settings: DisplayColorSettings,
    target: DisplayColorTarget,
    color: EditableRgbColor
): DisplayColorSettings {
    val normalized = EditableRgbColor(
        red = color.normalizedRed,
        green = color.normalizedGreen,
        blue = color.normalizedBlue
    )
    return when (target) {
        DisplayColorTarget.HOLD_OUTLINE -> settings.copy(holdOutline = normalized)
        DisplayColorTarget.SELECTED_HOLD -> settings.copy(selectedHold = normalized)
        DisplayColorTarget.RANGE_SELECTION -> settings.copy(rangeSelection = normalized)
        DisplayColorTarget.START_GOAL_HOLD -> settings.copy(startGoalHold = normalized)
    }
}

internal fun buildUpdatedDisplayStrokeWidthSettings(
    settings: DisplayColorSettings,
    target: DisplayColorTarget,
    strokeWidth: Int
): DisplayColorSettings {
    val normalized = strokeWidth.coerceIn(1, 5)
    return when (target) {
        DisplayColorTarget.HOLD_OUTLINE -> settings.copy(holdOutlineStrokeWidth = normalized)
        DisplayColorTarget.SELECTED_HOLD -> settings.copy(selectedHoldStrokeWidth = normalized)
        DisplayColorTarget.RANGE_SELECTION -> settings.copy(rangeSelectionStrokeWidth = normalized)
        DisplayColorTarget.START_GOAL_HOLD -> settings.copy(startGoalHoldStrokeWidth = normalized)
    }
}
