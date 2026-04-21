package com.example.holddetector.ui.screens

import androidx.camera.view.PreviewView
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
import com.example.holddetector.ui.CameraScreenBackgroundColor
import com.example.holddetector.ui.MainUiState

@Composable
fun HoldDetectorApp(
    state: MainUiState,
    isExternalBusy: Boolean,
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onNewWallClick: () -> Unit,
    onOpenSavedWallForReachCalibration: (String) -> Unit,
    onOpenSavedWallForHoldEditor: (String) -> Unit,
    onOpenSavedWallForHoldAttributeEditor: (String) -> Unit,
    onOpenSavedWallForHoldScoring: (String) -> Unit,
    onOpenSavedWallForChallenge: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    onCaptureClick: () -> Unit,
    onBindPreview: (PreviewView) -> Unit,
    onOpenManualHoldRegistrationAfterCapture: () -> Unit,
    onOpenAutoHoldExtractionAfterCapture: () -> Unit,
    onBackToCameraFromHoldRegistrationMethod: () -> Unit,
    onBackToHoldRegistrationMethodSelection: () -> Unit,
    onAutoExtractedHoldTapped: (Int?) -> Unit,
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
    onOpenChallengeMethodSelection: () -> Unit,
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
    onClearChallenge: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onDiscardChanges: () -> Unit
) {
    val contentPadding = if (state.currentScreen == AppScreen.CAMERA) 0.dp else 16.dp
    val backgroundColor = if (state.currentScreen == AppScreen.CAMERA) {
        CameraScreenBackgroundColor
    } else {
        AppBackgroundColor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (state.currentScreen) {
            AppScreen.LIST -> {
                WallListScreen(
                    savedWalls = state.savedWalls,
                    onNewWallClick = onNewWallClick,
                    onOpenSavedWallForReachCalibration = onOpenSavedWallForReachCalibration,
                    onOpenSavedWallForHoldEditor = onOpenSavedWallForHoldEditor,
                    onOpenSavedWallForHoldAttributeEditor = onOpenSavedWallForHoldAttributeEditor,
                    onOpenSavedWallForHoldScoring = onOpenSavedWallForHoldScoring,
                    onOpenSavedWallForChallenge = onOpenSavedWallForChallenge,
                    onDeleteSavedWall = onDeleteSavedWall,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            }

            AppScreen.CAMERA -> {
                CameraFullscreenScreen(
                    cameraPermissionGranted = cameraPermissionGranted,
                    onRequestCameraPermission = onRequestCameraPermission,
                    onCaptureClick = onCaptureClick,
                    onBindPreview = onBindPreview,
                    onBackToList = onBackToList,
                    modifier = Modifier.fillMaxSize()
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
                    onHoldTapped = onAutoExtractedHoldTapped,
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
                    onBackToList = onBackToList,
                    onSelectManualStartGoalChallengeMethod = onSelectManualStartGoalChallengeMethod,
                    onSelectRandomStartGoalChallengeMethod = onSelectRandomStartGoalChallengeMethod,
                    onOpenChallengeMethodSelection = onOpenChallengeMethodSelection,
                    onOpenChallengeCommonSettings = onOpenChallengeCommonSettings,
                    onOpenChallengeGeneration = onOpenChallengeGeneration,
                    onOpenChallengeTuning = onOpenChallengeTuning,
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
