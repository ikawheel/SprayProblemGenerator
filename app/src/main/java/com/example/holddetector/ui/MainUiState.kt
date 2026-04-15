package com.example.holddetector.ui

import android.graphics.Bitmap
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.model.ReachCalibrationReference
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

data class RouteGenerationTuning(
    val holdCountVariance: Float = 0.75f,
    val detourStrength: Float = 0.75f,
    val routeWaviness: Float = 0.75f,
    val stepDistanceVariance: Float = 0.75f,
    val corridorWidth: Float = 0.75f,
    val candidateSelectionRandomness: Float = 0.75f,
    val finalSelectionRandomness: Float = 0.75f
)

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
    val reachCalibrationReference: ReachCalibrationReference? = null,
    val pendingReachCalibrationPoint: HoldPoint? = null,
    val isReachCalibrationSelectionMode: Boolean = false,
    val selectedHoldIndex: Int? = null,
    val challengeHoldIndices: Set<Int> = emptySet(),
    val drawTargetHoldIndices: Set<Int> = emptySet(),
    val hasDrawTargetSelection: Boolean = false,
    val startHoldIndex: Int? = null,
    val goalHoldIndex: Int? = null,
    val routeSelectionMode: RouteSelectionMode = RouteSelectionMode.NONE,
    val isDrawTargetSelectionMode: Boolean = false,
    val drawCountInput: String = "",
    val routeTuning: RouteGenerationTuning = RouteGenerationTuning(),
    val isHoldEditorDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val message: String? = null
)
