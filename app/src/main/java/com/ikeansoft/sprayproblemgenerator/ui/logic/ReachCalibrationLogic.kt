package com.ikeansoft.sprayproblemgenerator.ui

import android.graphics.Bitmap
import com.ikeansoft.sprayproblemgenerator.model.CapturedOrientation
import com.ikeansoft.sprayproblemgenerator.model.ReachCalibrationReference
import com.ikeansoft.sprayproblemgenerator.model.HoldPoint

internal data class ReachCalibrationValidationResult(
    val reference: ReachCalibrationReference? = null,
    val message: String? = null
)

internal fun buildReachCalibrationScreenState(
    state: MainUiState,
    pushedScreenBackStack: (MainUiState, AppScreen) -> List<AppScreen>,
    firstPointMessage: String,
    confirmMessage: String
): MainUiState {
    return state.copy(
        currentScreen = AppScreen.REACH_CALIBRATION,
        screenBackStack = pushedScreenBackStack(state, AppScreen.REACH_CALIBRATION),
        selectedHoldIndex = null,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = state.reachCalibrationReference == null,
        reachCalibrationReturnToHoldEditor = true,
        reachCalibrationReturnToAutoExtraction = false,
        reachCalibrationLengthInput = state.reachCalibrationReference
            ?.referenceLengthCm
            ?.toString()
            ?: state.reachCalibrationLengthInput,
        message = if (state.reachCalibrationReference == null) {
            firstPointMessage
        } else {
            confirmMessage
        }
    )
}

internal fun buildHoldAttributeEditorStateFromReachCalibration(
    state: MainUiState,
    normalizedReference: ReachCalibrationReference,
    pushedScreenBackStack: (MainUiState, AppScreen) -> List<AppScreen>,
    message: String
): MainUiState {
    return state.copy(
        currentScreen = AppScreen.HOLD_ATTRIBUTE_EDITOR,
        screenBackStack = pushedScreenBackStack(state, AppScreen.HOLD_ATTRIBUTE_EDITOR),
        selectedHoldIndex = null,
        holdScoringPosition = 0,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        reachCalibrationReturnToHoldEditor = false,
        reachCalibrationReturnToAutoExtraction = false,
        autoExtractedHolds = emptyList(),
        reachCalibrationReference = normalizedReference,
        message = message
    )
}

internal fun buildAutoExtractionStateFromReachCalibration(
    state: MainUiState,
    normalizedReference: ReachCalibrationReference,
    pushedScreenBackStack: (MainUiState, AppScreen) -> List<AppScreen>
): MainUiState {
    return state.copy(
        currentScreen = AppScreen.AUTO_HOLD_EXTRACTION,
        screenBackStack = pushedScreenBackStack(state, AppScreen.AUTO_HOLD_EXTRACTION),
        holds = emptyList(),
        selectedHoldIndex = null,
        holdScoringPosition = 0,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        reachCalibrationReturnToHoldEditor = false,
        reachCalibrationReturnToAutoExtraction = false,
        autoExtractedHolds = emptyList(),
        reachCalibrationReference = normalizedReference,
        isAutoExtractionWallSamplingMode = false,
        isBusy = true,
        message = null
    )
}

internal fun buildBackFromReachCalibrationState(
    state: MainUiState,
    popScreenState: (MainUiState) -> MainUiState
): MainUiState {
    return popScreenState(
        state.copy(
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            reachCalibrationReturnToHoldEditor = false,
            reachCalibrationReturnToAutoExtraction = false,
            message = null
        )
    )
}

internal fun parseReachCalibrationLengthCentimeters(state: MainUiState): Int? {
    return state.reachCalibrationLengthInput.toIntOrNull()?.takeIf { it > 0 }
}

internal fun buildReachCalibrationLengthInputChangedState(
    state: MainUiState,
    value: String
): MainUiState {
    val digitsOnly = value.filter { it.isDigit() }.take(3)
    return state.copy(
        reachCalibrationLengthInput = digitsOnly,
        reachCalibrationReference = state.reachCalibrationReference?.let { existing ->
            digitsOnly.toIntOrNull()?.takeIf { it > 0 }?.let { parsed ->
                existing.copy(referenceLengthCm = parsed)
            } ?: existing
        }
    )
}

internal fun buildClearedReachCalibrationState(
    state: MainUiState,
    message: String
): MainUiState {
    return state.copy(
        reachCalibrationReference = null,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        selectedHoldIndex = null,
        isHoldEditorDirty = true,
        message = message
    )
}

internal fun buildReachCalibrationPointSelectedState(
    state: MainUiState,
    point: HoldPoint,
    messageSelectSecondPoint: String,
    messageSet: String
): MainUiState {
    val pendingPoint = state.pendingReachCalibrationPoint
    val referenceLengthCm = parseReachCalibrationLengthCentimeters(state)
        ?: state.reachCalibrationReference?.referenceLengthCm
        ?: return state
    return if (pendingPoint == null) {
        state.copy(
            pendingReachCalibrationPoint = point,
            selectedHoldIndex = null,
            message = messageSelectSecondPoint
        )
    } else {
        state.copy(
            reachCalibrationReference = ReachCalibrationReference(
                firstPoint = pendingPoint,
                secondPoint = point,
                referenceLengthCm = referenceLengthCm
            ),
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            selectedHoldIndex = null,
            isHoldEditorDirty = true,
            message = messageSet
        )
    }
}

internal fun ReachCalibrationReference?.withCurrentLength(state: MainUiState): ReachCalibrationReference? {
    val parsedLength = parseReachCalibrationLengthCentimeters(state) ?: return this
    return this?.copy(referenceLengthCm = parsedLength)
}

internal fun validateConfiguredReachReference(
    state: MainUiState,
    inputLengthMessage: String,
    setRequiredMessage: String
): ReachCalibrationValidationResult {
    if (parseReachCalibrationLengthCentimeters(state) == null) {
        return ReachCalibrationValidationResult(message = inputLengthMessage)
    }
    val normalizedReference = state.reachCalibrationReference.withCurrentLength(state)
    if (normalizedReference == null) {
        return ReachCalibrationValidationResult(message = setRequiredMessage)
    }
    return ReachCalibrationValidationResult(reference = normalizedReference)
}

internal fun orientationForBitmap(bitmap: Bitmap): CapturedOrientation {
    return if (bitmap.width > bitmap.height) {
        CapturedOrientation.LANDSCAPE
    } else {
        CapturedOrientation.PORTRAIT
    }
}
