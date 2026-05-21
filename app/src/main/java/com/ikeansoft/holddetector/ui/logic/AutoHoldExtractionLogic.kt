package com.ikeansoft.holddetector.ui

import android.graphics.Bitmap
import com.ikeansoft.holddetector.domain.hold.AutoExtractionTuning
import com.ikeansoft.holddetector.domain.hold.BinaryHoldExtractor
import com.ikeansoft.holddetector.model.CapturedOrientation
import com.ikeansoft.holddetector.model.DEFAULT_REACH_REFERENCE_LENGTH_CM
import com.ikeansoft.holddetector.model.Hold
import com.ikeansoft.holddetector.model.HoldPoint

internal const val AUTO_EXTRACTION_WALL_SAMPLE_TARGET_COUNT = 5

internal fun buildBackToHoldRegistrationMethodState(
    state: MainUiState,
    popScreenState: (MainUiState) -> MainUiState,
    message: String
): MainUiState {
    return popScreenState(
        state.copy(
            holds = emptyList(),
            autoExtractedHolds = emptyList(),
            selectedHoldIndex = null,
            isAutoExtractionWallSamplingMode = false,
            isBusy = false,
            message = message
        )
    )
}

internal fun buildAutoExtractionTuningUpdatedState(
    state: MainUiState,
    tuning: AutoExtractionTuning
): MainUiState {
    return state.copy(autoExtractionTuning = tuning)
}

internal fun buildAutoExtractionWallSamplingStartedState(
    state: MainUiState,
    message: String
): MainUiState {
    return state.copy(
        autoExtractionWallSamplePoints = emptyList(),
        isAutoExtractionWallSamplingMode = true,
        message = message
    )
}

internal fun buildAutoExtractionWallSamplingStoppedState(state: MainUiState): MainUiState {
    return state.copy(
        isAutoExtractionWallSamplingMode = false,
        message = null
    )
}

internal fun buildAutoExtractionWallSamplePointSelectedState(
    state: MainUiState,
    point: HoldPoint,
    message: String
): MainUiState {
    val updatedPoints = (state.autoExtractionWallSamplePoints + point)
        .distinct()
        .take(AUTO_EXTRACTION_WALL_SAMPLE_TARGET_COUNT)
    val keepSampling = updatedPoints.size < AUTO_EXTRACTION_WALL_SAMPLE_TARGET_COUNT
    return state.copy(
        autoExtractionWallSamplePoints = updatedPoints,
        isAutoExtractionWallSamplingMode = keepSampling,
        message = message
    )
}

internal fun buildClearedAutoExtractionWallSamplePointsState(
    state: MainUiState,
    message: String
): MainUiState {
    return state.copy(
        autoExtractionWallSamplePoints = emptyList(),
        isAutoExtractionWallSamplingMode = false,
        message = message
    )
}

internal fun buildHoldEditorStateFromAutoExtractedHolds(
    state: MainUiState,
    pushedScreenBackStack: (MainUiState, AppScreen) -> List<AppScreen>,
    message: String
): MainUiState {
    return state.copy(
        currentScreen = AppScreen.HOLD_EDITOR,
        screenBackStack = pushedScreenBackStack(state, AppScreen.HOLD_EDITOR),
        holds = state.autoExtractedHolds,
        selectedHoldIndex = null,
        holdScoringPosition = 0,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        reachCalibrationReturnToHoldEditor = false,
        reachCalibrationReturnToAutoExtraction = false,
        isHoldEditorDirty = true,
        message = message
    )
}

internal fun buildAutoExtractionState(
    state: MainUiState,
    bitmap: Bitmap,
    capturedOrientation: CapturedOrientation,
    capturedRotationDegrees: Int
): MainUiState {
    return state.copy(
        currentScreen = AppScreen.AUTO_HOLD_EXTRACTION,
        screenBackStack = if (state.currentScreen == AppScreen.AUTO_HOLD_EXTRACTION) {
            state.screenBackStack
        } else {
            state.screenBackStack + state.currentScreen
        },
        currentWallId = null,
        capturedBitmap = bitmap,
        capturedOrientation = capturedOrientation,
        capturedRotationDegrees = capturedRotationDegrees,
        holds = emptyList(),
        autoExtractedHolds = emptyList(),
        autoExtractionWallSamplePoints = emptyList(),
        isAutoExtractionWallSamplingMode = false,
        reachCalibrationReference = null,
        reachCalibrationLengthInput = DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        reachCalibrationReturnToHoldEditor = false,
        reachCalibrationReturnToAutoExtraction = false,
        selectedHoldIndex = null,
        challengeHoldIndices = emptySet(),
        challengeOrderedHoldIndices = emptyList(),
        lastGeneratedIntermediateHoldIndices = emptySet(),
        drawTargetHoldIndices = emptySet(),
        hasDrawTargetSelection = false,
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        isDrawTargetSelectionMode = false,
        isHoldEditorDirty = true,
        showDiscardDialog = false,
        isBusy = true,
        message = null
    )
}

internal suspend fun extractAutoHolds(
    bitmap: Bitmap,
    tuning: AutoExtractionTuning,
    wallSamplePoints: List<HoldPoint>
): List<Hold> {
    return BinaryHoldExtractor.extract(
        bitmap = bitmap,
        tuning = tuning,
        wallSamplePoints = wallSamplePoints
    )
}
