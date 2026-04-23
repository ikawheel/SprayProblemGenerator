package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.domain.hold.AutoExtractionTuning
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.AppBackgroundColor
import com.example.holddetector.ui.AppBusyOverlayColor
import com.example.holddetector.ui.HoldTapAreaSize
import com.example.holddetector.ui.AppScreen
import com.example.holddetector.ui.MainUiState

@Composable
fun HoldDetectorApp(
    state: MainUiState,
    isExternalBusy: Boolean,
    onOpenSavedWallForReachCalibration: (String) -> Unit,
    onOpenSavedWallForHoldEditor: (String) -> Unit,
    onOpenSavedWallForHoldAttributeEditor: (String) -> Unit,
    onOpenSavedWallForHoldScoring: (String) -> Unit,
    onOpenSavedWallForManualStartGoalChallenge: (String) -> Unit,
    onOpenSavedWallForRandomStartGoalChallenge: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenManualHoldRegistrationAfterCapture: () -> Unit,
    onOpenAutoHoldExtractionAfterCapture: () -> Unit,
    onBackToCameraFromHoldRegistrationMethod: () -> Unit,
    onBackToHoldRegistrationMethodSelection: () -> Unit,
    onAutoExtractedHoldTapped: (Int?) -> Unit,
    onEstimateAutoExtractionWallSamplePoints: () -> Unit,
    onStartAutoExtractionWallSampling: () -> Unit,
    onStopAutoExtractionWallSampling: () -> Unit,
    onAutoExtractionWallSamplePointSelected: (HoldPoint) -> Unit,
    onClearAutoExtractionWallSamplePoints: () -> Unit,
    onAutoExtractionTuningChange: (AutoExtractionTuning) -> Unit,
    onApplyAutoExtractedHoldsAndContinue: () -> Unit,
    onBackToList: () -> Unit,
    onSaveWall: () -> Unit,
    onOpenHoldAttributeEditor: () -> Unit,
    onBackFromHoldAttributeEditor: () -> Unit,
    onOpenHoldScoring: () -> Unit,
    onBackFromHoldScoring: () -> Unit,
    onDifficultyScoreSelected: (Int) -> Unit,
    onSaveWallAndOpenChallenge: () -> Unit,
    onWallTitleChanged: (String) -> Unit,
    onHoldTapAreaSizeChange: (HoldTapAreaSize) -> Unit,
    onDeleteSelectedHold: () -> Unit,
    onEditorHoldTapped: (Int?) -> Unit,
    onAssignHoldAsStartCandidate: (Int?) -> Unit,
    onAssignHoldAsGoalCandidate: (Int?) -> Unit,
    onClearHoldAttributes: (Int?) -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    onContinueToHoldEditorFromReachCalibration: () -> Unit,
    onBackFromReachCalibration: () -> Unit,
    onStartReachCalibrationSelection: () -> Unit,
    onReachCalibrationLengthInputChange: (String) -> Unit,
    onClearReachCalibration: () -> Unit,
    onReachCalibrationPointSelected: (HoldPoint) -> Unit,
    onSelectManualStartGoalChallengeMethod: () -> Unit,
    onSelectRandomStartGoalChallengeMethod: () -> Unit,
    onOpenChallengeCommonSettings: () -> Unit,
    onOpenChallengeGeneration: () -> Unit,
    onOpenChallengeTuning: () -> Unit,
    onStartGoalSelection: () -> Unit,
    onDrawWithRandomStartGoal: () -> Unit,
    onStartDrawTargetSelection: () -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawClick: () -> Unit,
    onRerunCurrentChallengeGeneration: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onExcludePreviouslyGeneratedHoldsChange: (Boolean) -> Unit,
    onClearChallenge: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onDiscardChanges: () -> Unit
) {
    val contentPadding = 16.dp
    val backgroundColor = AppBackgroundColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (state.currentScreen) {
                AppScreen.LIST -> {
                    WallListScreen(
                        savedWalls = state.savedWalls,
                        onTakePhoto = onTakePhoto,
                        onPickPhoto = onPickPhoto,
                        onOpenSavedWallForReachCalibration = onOpenSavedWallForReachCalibration,
                        onOpenSavedWallForHoldEditor = onOpenSavedWallForHoldEditor,
                        onOpenSavedWallForHoldAttributeEditor = onOpenSavedWallForHoldAttributeEditor,
                        onOpenSavedWallForHoldScoring = onOpenSavedWallForHoldScoring,
                        onOpenSavedWallForManualStartGoalChallenge = onOpenSavedWallForManualStartGoalChallenge,
                        onOpenSavedWallForRandomStartGoalChallenge = onOpenSavedWallForRandomStartGoalChallenge,
                        onDeleteSavedWall = onDeleteSavedWall,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    )
                }

                AppScreen.CAMERA -> {
                    CameraFullscreenScreen(
                        onTakePhoto = onTakePhoto,
                        onPickPhoto = onPickPhoto,
                        onBackToList = onBackToList,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    )
                }

                AppScreen.HOLD_REGISTRATION_METHOD -> {
                    HoldRegistrationMethodScreen(
                        bitmap = state.capturedBitmap,
                        onBackToCamera = onBackToCameraFromHoldRegistrationMethod,
                        onOpenManualRegistration = onOpenManualHoldRegistrationAfterCapture,
                        onOpenAutoExtraction = onOpenAutoHoldExtractionAfterCapture,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppScreen.AUTO_HOLD_EXTRACTION -> {
                    AutoHoldExtractionScreen(
                        bitmap = state.capturedBitmap,
                        extractedHolds = state.autoExtractedHolds,
                        tuning = state.autoExtractionTuning,
                        selectedHoldIndex = state.selectedHoldIndex,
                        wallSamplePoints = state.autoExtractionWallSamplePoints,
                        isWallSamplingMode = state.isAutoExtractionWallSamplingMode,
                        onHoldTapped = onAutoExtractedHoldTapped,
                        onEstimateWallSamplePoints = onEstimateAutoExtractionWallSamplePoints,
                        onStartWallSampling = onStartAutoExtractionWallSampling,
                        onStopWallSampling = onStopAutoExtractionWallSampling,
                        onWallSamplePointSelected = onAutoExtractionWallSamplePointSelected,
                        onClearWallSamplePoints = onClearAutoExtractionWallSamplePoints,
                        onTuningChange = onAutoExtractionTuningChange,
                        onBackToMethodSelection = onBackToHoldRegistrationMethodSelection,
                        onApplyExtraction = onApplyAutoExtractedHoldsAndContinue,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppScreen.REACH_CALIBRATION -> {
                    ReachCalibrationScreen(
                        state = state,
                        onBack = onBackFromReachCalibration,
                        onExitWithoutSaving = onBackToList,
                        onSaveAndExit = onSaveWall,
                        onStartReachCalibrationSelection = onStartReachCalibrationSelection,
                        onReachCalibrationLengthInputChange = onReachCalibrationLengthInputChange,
                        onClearReachCalibration = onClearReachCalibration,
                        onReachCalibrationPointSelected = onReachCalibrationPointSelected,
                        onContinue = onContinueToHoldEditorFromReachCalibration,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppScreen.HOLD_EDITOR -> {
                    HoldEditorScreen(
                        state = state,
                        onWallTitleChanged = onWallTitleChanged,
                        onHoldTapAreaSizeChange = onHoldTapAreaSizeChange,
                        onSaveWall = onSaveWall,
                        onOpenHoldAttributeEditor = onOpenHoldAttributeEditor,
                        onBackToList = onBackToList,
                        onDeleteSelectedHold = onDeleteSelectedHold,
                        onEditorHoldTapped = onEditorHoldTapped,
                        onManualHoldCreated = onManualHoldCreated,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppScreen.HOLD_ATTRIBUTE_EDITOR -> {
                    HoldAttributeEditorScreen(
                        state = state,
                        onBackToHoldEditor = onBackFromHoldAttributeEditor,
                        onExitWithoutSaving = onBackToList,
                        onSaveAndExit = onSaveWall,
                        onOpenHoldScoring = onOpenHoldScoring,
                        onAssignHoldAsStartCandidate = onAssignHoldAsStartCandidate,
                        onAssignHoldAsGoalCandidate = onAssignHoldAsGoalCandidate,
                        onClearHoldAttributes = onClearHoldAttributes,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppScreen.HOLD_SCORING -> {
                    HoldScoringScreen(
                        state = state,
                        onBackToHoldEditor = onBackFromHoldScoring,
                        onExitWithoutSaving = onBackToList,
                        onSaveAndExit = onSaveWall,
                        onDifficultyScoreSelected = onDifficultyScoreSelected,
                        onOpenChallenge = onSaveWallAndOpenChallenge,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                AppScreen.CHALLENGE_CREATOR -> {
                    ChallengeCreatorScreen(
                        state = state,
                        onSelectManualStartGoalChallengeMethod = onSelectManualStartGoalChallengeMethod,
                        onSelectRandomStartGoalChallengeMethod = onSelectRandomStartGoalChallengeMethod,
                        onOpenChallengeCommonSettings = onOpenChallengeCommonSettings,
                        onOpenChallengeGeneration = onOpenChallengeGeneration,
                        onChallengeHoldTapped = onChallengeHoldTapped,
                        onStartGoalSelection = onStartGoalSelection,
                        onDrawWithRandomStartGoal = onDrawWithRandomStartGoal,
                        onStartDrawTargetSelection = onStartDrawTargetSelection,
                        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                        onDrawClick = onDrawClick,
                        onRerunCurrentChallengeGeneration = onRerunCurrentChallengeGeneration,
                        onDrawCountChange = onDrawCountChange,
                        onChallengeDifficultyRangeChange = onChallengeDifficultyRangeChange,
                        onDetourStrengthChange = onDetourStrengthChange,
                        onRouteWavinessChange = onRouteWavinessChange,
                        onStepDistanceVarianceChange = onStepDistanceVarianceChange,
                        onCorridorWidthChange = onCorridorWidthChange,
                        onExcludePreviouslyGeneratedHoldsChange = onExcludePreviouslyGeneratedHoldsChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    )
                }
            }

        if (state.isBusy || isExternalBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBusyOverlayColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscardDialog,
            title = { Text(stringResource(R.string.back_to_list)) },
            text = { Text(stringResource(R.string.discard_dialog_message)) },
            confirmButton = {
                AppButton(onClick = onDiscardChanges) {
                    Text(stringResource(R.string.discard_dialog_confirm))
                }
            },
            dismissButton = {
                AppOutlinedButton(onClick = onDismissDiscardDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
