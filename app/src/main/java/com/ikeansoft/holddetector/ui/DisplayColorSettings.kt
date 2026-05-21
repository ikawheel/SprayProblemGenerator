package com.ikeansoft.holddetector.ui

import androidx.compose.ui.graphics.Color

enum class DisplayColorTarget {
    HOLD_OUTLINE,
    SELECTED_HOLD,
    RANGE_SELECTION,
    START_GOAL_HOLD
}

data class EditableRgbColor(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    val normalizedRed: Int = red.coerceIn(0, 255)
    val normalizedGreen: Int = green.coerceIn(0, 255)
    val normalizedBlue: Int = blue.coerceIn(0, 255)

    fun toComposeColor(): Color = Color(
        red = normalizedRed,
        green = normalizedGreen,
        blue = normalizedBlue
    )
}

data class DisplayColorSettings(
    val holdOutline: EditableRgbColor = EditableRgbColor(red = 0, green = 255, blue = 0),
    val selectedHold: EditableRgbColor = EditableRgbColor(red = 255, green = 0, blue = 0),
    val rangeSelection: EditableRgbColor = EditableRgbColor(red = 0, green = 255, blue = 255),
    val startGoalHold: EditableRgbColor = EditableRgbColor(red = 59, green = 130, blue = 246),
    val holdOutlineStrokeWidth: Int = 1,
    val selectedHoldStrokeWidth: Int = 1,
    val rangeSelectionStrokeWidth: Int = 1,
    val startGoalHoldStrokeWidth: Int = 1
) {
    val holdOutlineColor: Color
        get() = holdOutline.toComposeColor()

    val selectedHoldColor: Color
        get() = selectedHold.toComposeColor()

    val rangeSelectionColor: Color
        get() = rangeSelection.toComposeColor()

    val startGoalHoldColor: Color
        get() = startGoalHold.toComposeColor()

    val normalizedHoldOutlineStrokeWidth: Int
        get() = holdOutlineStrokeWidth.coerceIn(1, 5)

    val normalizedSelectedHoldStrokeWidth: Int
        get() = selectedHoldStrokeWidth.coerceIn(1, 5)

    val normalizedRangeSelectionStrokeWidth: Int
        get() = rangeSelectionStrokeWidth.coerceIn(1, 5)

    val normalizedStartGoalHoldStrokeWidth: Int
        get() = startGoalHoldStrokeWidth.coerceIn(1, 5)
}
