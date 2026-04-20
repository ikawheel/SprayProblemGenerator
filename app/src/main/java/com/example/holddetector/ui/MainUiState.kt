package com.example.holddetector.ui

import android.graphics.Bitmap
import com.example.holddetector.domain.challenge.RouteGenerationTuning
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.DEFAULT_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.model.MAX_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.MIN_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.ReachCalibrationReference
import com.example.holddetector.model.SavedWallSummary

enum class AppScreen {
    LIST,
    CAMERA,
    EDIT_MENU,
    REACH_CALIBRATION,
    HOLD_EDITOR,
    HOLD_SCORING,
    CHALLENGE_CREATOR
}

enum class RouteSelectionMode {
    NONE,
    SELECTING_START,
    SELECTING_GOAL
}

enum class HoldTapAreaSize {
    SMALL,
    MEDIUM,
    LARGE
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
    val reachCalibrationReference: ReachCalibrationReference? = null,
    val pendingReachCalibrationPoint: HoldPoint? = null,
    val isReachCalibrationSelectionMode: Boolean = false,
    val reachCalibrationReturnToHoldEditor: Boolean = false,
    val selectedHoldIndex: Int? = null,
    val challengeHoldIndices: Set<Int> = emptySet(),
    val challengeOrderedHoldIndices: List<Int> = emptyList(),
    val drawTargetHoldIndices: Set<Int> = emptySet(),
    val hasDrawTargetSelection: Boolean = false,
    val startHoldIndex: Int? = null,
    val goalHoldIndex: Int? = null,
    val routeSelectionMode: RouteSelectionMode = RouteSelectionMode.NONE,
    val isDrawTargetSelectionMode: Boolean = false,
    val drawCountInput: String = "",
    val holdTapAreaSize: HoldTapAreaSize = HoldTapAreaSize.MEDIUM,
    val challengeDifficultyScoreMin: Int = MIN_HOLD_DIFFICULTY_SCORE,
    val challengeDifficultyScoreMax: Int = DEFAULT_HOLD_DIFFICULTY_SCORE.coerceAtMost(MAX_HOLD_DIFFICULTY_SCORE),
    val routeTuning: RouteGenerationTuning = RouteGenerationTuning(),
    val isHoldEditorDirty: Boolean = false,
    val holdScoringPosition: Int = 0,
    val showDiscardDialog: Boolean = false,
    val message: String? = null
)
