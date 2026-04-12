package com.example.holddetector.ui

import android.graphics.Bitmap
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.Hold
import com.example.holddetector.model.SavedWallSummary

enum class AppScreen {
    LIST,
    CAMERA,
    HOLD_EDITOR,
    CHALLENGE_CREATOR
}

enum class RouteSelectionMode {
    NONE,
    SELECTING_START,
    SELECTING_GOAL
}

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.LIST,
    val savedWalls: List<SavedWallSummary> = emptyList(),
    val isBusy: Boolean = false,
    val currentWallId: String? = null,
    val wallTitle: String = "",
    val capturedBitmap: Bitmap? = null,
    val capturedOrientation: CapturedOrientation = CapturedOrientation.PORTRAIT,
    val capturedRotationDegrees: Int = 0,
    val holds: List<Hold> = emptyList(),
    val selectedHoldIndex: Int? = null,
    val challengeHoldIndices: Set<Int> = emptySet(),
    val startHoldIndex: Int? = null,
    val goalHoldIndex: Int? = null,
    val routeSelectionMode: RouteSelectionMode = RouteSelectionMode.NONE,
    val drawCountInput: String = "",
    val isHoldEditorDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val message: String? = null
)
