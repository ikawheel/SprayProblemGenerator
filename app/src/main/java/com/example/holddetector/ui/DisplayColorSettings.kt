package com.example.holddetector.ui

import androidx.compose.ui.graphics.Color

enum class DisplayColorTarget {
    HOLD_OUTLINE,
    SELECTED_HOLD,
    RANGE_SELECTION
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
    val rangeSelection: EditableRgbColor = EditableRgbColor(red = 0, green = 255, blue = 255)
) {
    val holdOutlineColor: Color
        get() = holdOutline.toComposeColor()

    val selectedHoldColor: Color
        get() = selectedHold.toComposeColor()

    val rangeSelectionColor: Color
        get() = rangeSelection.toComposeColor()
}
