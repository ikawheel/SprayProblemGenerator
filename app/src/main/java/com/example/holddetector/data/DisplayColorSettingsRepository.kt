package com.example.holddetector.data

import android.content.Context
import com.example.holddetector.ui.DisplayColorSettings
import com.example.holddetector.ui.EditableRgbColor

class DisplayColorSettingsRepository(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): DisplayColorSettings {
        val defaults = DisplayColorSettings()
        return DisplayColorSettings(
            holdOutline = EditableRgbColor(
                red = preferences.getInt(KEY_HOLD_OUTLINE_RED, defaults.holdOutline.normalizedRed),
                green = preferences.getInt(KEY_HOLD_OUTLINE_GREEN, defaults.holdOutline.normalizedGreen),
                blue = preferences.getInt(KEY_HOLD_OUTLINE_BLUE, defaults.holdOutline.normalizedBlue)
            ),
            selectedHold = EditableRgbColor(
                red = preferences.getInt(KEY_SELECTED_HOLD_RED, defaults.selectedHold.normalizedRed),
                green = preferences.getInt(KEY_SELECTED_HOLD_GREEN, defaults.selectedHold.normalizedGreen),
                blue = preferences.getInt(KEY_SELECTED_HOLD_BLUE, defaults.selectedHold.normalizedBlue)
            ),
            rangeSelection = EditableRgbColor(
                red = preferences.getInt(KEY_RANGE_SELECTION_RED, defaults.rangeSelection.normalizedRed),
                green = preferences.getInt(KEY_RANGE_SELECTION_GREEN, defaults.rangeSelection.normalizedGreen),
                blue = preferences.getInt(KEY_RANGE_SELECTION_BLUE, defaults.rangeSelection.normalizedBlue)
            ),
            startGoalHold = EditableRgbColor(
                red = preferences.getInt(KEY_START_GOAL_HOLD_RED, defaults.startGoalHold.normalizedRed),
                green = preferences.getInt(KEY_START_GOAL_HOLD_GREEN, defaults.startGoalHold.normalizedGreen),
                blue = preferences.getInt(KEY_START_GOAL_HOLD_BLUE, defaults.startGoalHold.normalizedBlue)
            )
        )
    }

    fun save(settings: DisplayColorSettings) {
        preferences.edit()
            .putInt(KEY_HOLD_OUTLINE_RED, settings.holdOutline.normalizedRed)
            .putInt(KEY_HOLD_OUTLINE_GREEN, settings.holdOutline.normalizedGreen)
            .putInt(KEY_HOLD_OUTLINE_BLUE, settings.holdOutline.normalizedBlue)
            .putInt(KEY_SELECTED_HOLD_RED, settings.selectedHold.normalizedRed)
            .putInt(KEY_SELECTED_HOLD_GREEN, settings.selectedHold.normalizedGreen)
            .putInt(KEY_SELECTED_HOLD_BLUE, settings.selectedHold.normalizedBlue)
            .putInt(KEY_RANGE_SELECTION_RED, settings.rangeSelection.normalizedRed)
            .putInt(KEY_RANGE_SELECTION_GREEN, settings.rangeSelection.normalizedGreen)
            .putInt(KEY_RANGE_SELECTION_BLUE, settings.rangeSelection.normalizedBlue)
            .putInt(KEY_START_GOAL_HOLD_RED, settings.startGoalHold.normalizedRed)
            .putInt(KEY_START_GOAL_HOLD_GREEN, settings.startGoalHold.normalizedGreen)
            .putInt(KEY_START_GOAL_HOLD_BLUE, settings.startGoalHold.normalizedBlue)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "display_color_settings"

        const val KEY_HOLD_OUTLINE_RED = "hold_outline_red"
        const val KEY_HOLD_OUTLINE_GREEN = "hold_outline_green"
        const val KEY_HOLD_OUTLINE_BLUE = "hold_outline_blue"

        const val KEY_SELECTED_HOLD_RED = "selected_hold_red"
        const val KEY_SELECTED_HOLD_GREEN = "selected_hold_green"
        const val KEY_SELECTED_HOLD_BLUE = "selected_hold_blue"

        const val KEY_RANGE_SELECTION_RED = "range_selection_red"
        const val KEY_RANGE_SELECTION_GREEN = "range_selection_green"
        const val KEY_RANGE_SELECTION_BLUE = "range_selection_blue"

        const val KEY_START_GOAL_HOLD_RED = "start_goal_hold_red"
        const val KEY_START_GOAL_HOLD_GREEN = "start_goal_hold_green"
        const val KEY_START_GOAL_HOLD_BLUE = "start_goal_hold_blue"
    }
}
