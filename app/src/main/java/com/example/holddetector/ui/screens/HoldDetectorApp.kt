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
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.AppBackgroundColor
import com.example.holddetector.ui.AppBusyOverlayColor
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
    onOpenSavedWallForEditing: (String) -> Unit,
    onOpenSavedWallForChallenge: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    onCaptureClick: () -> Unit,
    onBindPreview: (PreviewView) -> Unit,
    onBackToList: () -> Unit,
    onSaveWall: () -> Unit,
    onOpenHoldScoring: () -> Unit,
    onBackFromHoldScoring: () -> Unit,
    onDifficultyScoreSelected: (Int) -> Unit,
    onSaveWallAndOpenChallenge: () -> Unit,
    onWallTitleChanged: (String) -> Unit,
    onDeleteSelectedHold: () -> Unit,
    onEditorHoldTapped: (Int?) -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    onOpenReachCalibrationScreen: () -> Unit,
    onContinueToHoldEditorFromReachCalibration: () -> Unit,
    onBackFromReachCalibration: () -> Unit,
    onStartReachCalibrationSelection: () -> Unit,
    onClearReachCalibration: () -> Unit,
    onReachCalibrationPointSelected: (HoldPoint) -> Unit,
    onStartGoalSelection: () -> Unit,
    onStartDrawTargetSelection: () -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawClick: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit,
    onHoldCountVarianceChange: (Float) -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onCandidateSelectionRandomnessChange: (Float) -> Unit,
    onFinalSelectionRandomnessChange: (Float) -> Unit,
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
            .padding(contentPadding)
    ) {
        when (state.currentScreen) {
            AppScreen.LIST -> {
                WallListScreen(
                    savedWalls = state.savedWalls,
                    onNewWallClick = onNewWallClick,
                    onOpenSavedWallForEditing = onOpenSavedWallForEditing,
                    onOpenSavedWallForChallenge = onOpenSavedWallForChallenge,
                    onDeleteSavedWall = onDeleteSavedWall,
                    modifier = Modifier.fillMaxSize()
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

            AppScreen.REACH_CALIBRATION -> {
                ReachCalibrationScreen(
                    state = state,
                    onBack = onBackFromReachCalibration,
                    onStartReachCalibrationSelection = onStartReachCalibrationSelection,
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
                    onSaveWall = onSaveWall,
                    onOpenHoldScoring = onOpenHoldScoring,
                    onBackToList = onBackToList,
                    onDeleteSelectedHold = onDeleteSelectedHold,
                    onEditorHoldTapped = onEditorHoldTapped,
                    onManualHoldCreated = onManualHoldCreated,
                    onOpenReachCalibrationScreen = onOpenReachCalibrationScreen,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.HOLD_SCORING -> {
                HoldScoringScreen(
                    state = state,
                    onBackToHoldEditor = onBackFromHoldScoring,
                    onDifficultyScoreSelected = onDifficultyScoreSelected,
                    onOpenChallenge = onSaveWallAndOpenChallenge,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.CHALLENGE_CREATOR -> {
                ChallengeCreatorScreen(
                    state = state,
                    onBackToList = onBackToList,
                    onChallengeHoldTapped = onChallengeHoldTapped,
                    onStartGoalSelection = onStartGoalSelection,
                    onStartDrawTargetSelection = onStartDrawTargetSelection,
                    onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                    onDrawClick = onDrawClick,
                    onDrawCountChange = onDrawCountChange,
                    onChallengeDifficultyRangeChange = onChallengeDifficultyRangeChange,
                    onHoldCountVarianceChange = onHoldCountVarianceChange,
                    onDetourStrengthChange = onDetourStrengthChange,
                    onRouteWavinessChange = onRouteWavinessChange,
                    onStepDistanceVarianceChange = onStepDistanceVarianceChange,
                    onCorridorWidthChange = onCorridorWidthChange,
                    onCandidateSelectionRandomnessChange = onCandidateSelectionRandomnessChange,
                    onFinalSelectionRandomnessChange = onFinalSelectionRandomnessChange,
                    onClearChallenge = onClearChallenge,
                    modifier = Modifier.fillMaxSize()
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
