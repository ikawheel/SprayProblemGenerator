package com.ikeansoft.sprayproblemgenerator.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.domain.hold.AutoExtractionTuning
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.model.HoldPoint
import com.ikeansoft.sprayproblemgenerator.ui.AppBackgroundColor
import com.ikeansoft.sprayproblemgenerator.ui.AppBusyOverlayColor
import com.ikeansoft.sprayproblemgenerator.ui.AppScreen
import com.ikeansoft.sprayproblemgenerator.ui.AppSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.DisplayColorTarget
import com.ikeansoft.sprayproblemgenerator.ui.EditableRgbColor
import com.ikeansoft.sprayproblemgenerator.ui.HoldEditorTool
import com.ikeansoft.sprayproblemgenerator.ui.HoldTapAreaSize
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppConfirmDialog
import kotlinx.coroutines.launch

@Composable
fun SprayProblemGeneratorApp(
    state: MainUiState,
    isExternalBusy: Boolean,
    onOpenSavedWallForReachCalibration: (String) -> Unit,
    onOpenSavedWallForHoldEditor: (String) -> Unit,
    onOpenSavedWallForHoldAttributeEditor: (String) -> Unit,
    onOpenSavedWallForHoldScoring: (String) -> Unit,
    onOpenSavedWallForChallenge: (String) -> Unit,
    onOpenSavedWallChallenges: (String) -> Unit,
    onOpenSavedChallenge: (String, String) -> Unit,
    onSaveSavedChallengeImage: (android.graphics.Bitmap?) -> Unit,
    onDeleteSavedChallenge: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    onOpenDisplayColorSettings: () -> Unit,
    onOpenLicenses: () -> Unit,
    onUpdateDisplayColor: (DisplayColorTarget, EditableRgbColor) -> Unit,
    onUpdateDisplayStrokeWidth: (DisplayColorTarget, Int) -> Unit,
    onResetDisplayColorSettings: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onApplyCapturedImageCropManual: (Float, Float, Float, Float) -> Unit,
    onApplyCapturedImageCropAuto: (Float, Float, Float, Float) -> Unit,
    onOpenManualHoldRegistrationAfterCapture: () -> Unit,
    onOpenAutoHoldExtractionAfterCapture: () -> Unit,
    onBackToCameraFromHoldRegistrationMethod: () -> Unit,
    onBackToHoldRegistrationMethodSelection: () -> Unit,
    onAutoExtractedHoldTapped: (Int?) -> Unit,
    onStartAutoExtractionWallSampling: () -> Unit,
    onStopAutoExtractionWallSampling: () -> Unit,
    onAutoExtractionWallSamplePointSelected: (HoldPoint) -> Unit,
    onAutoExtractionTuningChange: (AutoExtractionTuning) -> Unit,
    onApplyAutoExtractedHoldsAndContinue: () -> Unit,
    onBackToList: () -> Unit,
    onReturnToList: () -> Unit,
    onSaveWall: () -> Unit,
    onOpenReachCalibrationScreen: () -> Unit,
    onSaveEditedHoldsInHoldEditor: (List<Hold>, Int?) -> Unit,
    onBackFromHoldAttributeEditor: () -> Unit,
    onOpenHoldScoring: () -> Unit,
    onBackFromHoldScoring: () -> Unit,
    onDifficultyScoreSelected: (Int) -> Unit,
    onSaveWallAndOpenChallenge: () -> Unit,
    onHoldTapAreaSizeChange: (HoldTapAreaSize) -> Unit,
    onOpenHoldEditOperation: (HoldEditorTool) -> Unit,
    onDeleteSelectedHold: () -> Unit,
    onEditorHoldTapped: (Int?) -> Unit,
    onAssignHoldAsStartCandidate: (Int?) -> Unit,
    onAssignHoldAsGoalCandidate: (Int?) -> Unit,
    onClearHoldAttributes: (Int?) -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onApplyEditedHoldsAndReturn: (List<Hold>, Int?) -> Unit,
    onBackFromHoldEditOperation: () -> Unit,
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
    onSaveCurrentChallenge: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onExcludePreviouslyGeneratedHoldsChange: (Boolean) -> Unit,
    onRandomStartGoalPairLimitChange: (Int) -> Unit,
    onRouteGenerationAttemptLimitChange: (Int) -> Unit,
    onClearChallenge: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onDiscardChanges: () -> Unit
) {
    val contentPadding = 16.dp
    val backgroundColor = AppBackgroundColor
    val primaryColor = MaterialTheme.colorScheme.primary
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    LaunchedEffect(state.currentScreen) {
        if (
            state.currentScreen != AppScreen.LIST &&
            state.currentScreen != AppScreen.DISPLAY_COLOR_SETTINGS &&
            state.currentScreen != AppScreen.LICENSES &&
            drawerState.isOpen
        ) {
            drawerState.close()
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = state.currentScreen == AppScreen.LIST ||
            state.currentScreen == AppScreen.DISPLAY_COLOR_SETTINGS ||
            state.currentScreen == AppScreen.LICENSES,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_menu_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTextColor
                    )
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppTextColor.copy(alpha = 0.10f))
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.display_color_settings_menu)) },
                    selected = state.currentScreen == AppScreen.DISPLAY_COLOR_SETTINGS,
                    onClick = {
                        drawerScope.launch { drawerState.close() }
                        onOpenDisplayColorSettings()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.licenses_menu)) },
                    selected = state.currentScreen == AppScreen.LICENSES,
                    onClick = {
                        drawerScope.launch { drawerState.close() }
                        onOpenLicenses()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarTopPadding)
                    .background(primaryColor)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTopPadding)
            ) {
                when (state.currentScreen) {
                    AppScreen.LIST -> {
                        WallListScreen(
                            savedWalls = state.savedWalls,
                            onOpenMenu = {
                                drawerScope.launch {
                                    drawerState.open()
                                }
                            },
                            onTakePhoto = onTakePhoto,
                            onPickPhoto = onPickPhoto,
                            onOpenSavedWallForReachCalibration = onOpenSavedWallForReachCalibration,
                            onOpenSavedWallForHoldEditor = onOpenSavedWallForHoldEditor,
                            onOpenSavedWallForHoldAttributeEditor = onOpenSavedWallForHoldAttributeEditor,
                            onOpenSavedWallForHoldScoring = onOpenSavedWallForHoldScoring,
                            onOpenSavedWallForChallenge = onOpenSavedWallForChallenge,
                            onOpenSavedWallChallenges = onOpenSavedWallChallenges,
                            onDeleteSavedWall = onDeleteSavedWall,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppScreen.DISPLAY_COLOR_SETTINGS -> {
                        DisplayColorSettingsScreen(
                            settings = state.displayColorSettings,
                            onUpdateColor = onUpdateDisplayColor,
                            onUpdateStrokeWidth = onUpdateDisplayStrokeWidth,
                            onResetToDefaults = onResetDisplayColorSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppScreen.LICENSES -> {
                        LicensesScreen(
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppScreen.SAVED_CHALLENGE_LIST -> {
                        SavedChallengeListScreen(
                            savedChallenges = state.savedChallenges,
                            bitmap = state.capturedBitmap,
                            holds = state.holds,
                            displayColorSettings = state.displayColorSettings,
                            onOpenSavedChallenge = { challengeId ->
                                state.currentWallId?.let { wallId ->
                                    onOpenSavedChallenge(wallId, challengeId)
                                }
                            },
                            onDeleteSavedChallenge = onDeleteSavedChallenge,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppScreen.SAVED_CHALLENGE_DETAIL -> {
                        SavedChallengeDetailScreen(
                            state = state,
                            onSaveChallengeImage = onSaveSavedChallengeImage,
                            modifier = Modifier.fillMaxSize()
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

                    AppScreen.IMAGE_CROP -> {
                        ImageCropScreen(
                            bitmap = state.capturedBitmap,
                            message = state.message,
                            onApplyCropManual = onApplyCapturedImageCropManual,
                            onApplyCropAuto = onApplyCapturedImageCropAuto,
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
                            wallSamplePoints = state.autoExtractionWallSamplePoints,
                            isWallSamplingMode = state.isAutoExtractionWallSamplingMode,
                            isBusy = state.isBusy,
                            displayColorSettings = state.displayColorSettings,
                            onHoldTapped = onAutoExtractedHoldTapped,
                            onStartWallSampling = onStartAutoExtractionWallSampling,
                            onStopWallSampling = onStopAutoExtractionWallSampling,
                            onWallSamplePointSelected = onAutoExtractionWallSamplePointSelected,
                            onTuningChange = onAutoExtractionTuningChange,
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
                            onContinueToHoldEditor = onContinueToHoldEditorFromReachCalibration,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppScreen.HOLD_EDITOR -> {
                        HoldEditorScreen(
                            state = state,
                            onReturnToList = onReturnToList,
                            onOpenReachCalibration = onOpenReachCalibrationScreen,
                            onHoldTapAreaSizeChange = onHoldTapAreaSizeChange,
                            onSaveEditedHolds = onSaveEditedHoldsInHoldEditor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AppScreen.HOLD_EDIT_OPERATION -> {
                        HoldEditOperationScreen(
                            mode = state.holdEditorTool,
                            bitmap = state.capturedBitmap,
                            initialHolds = state.holds,
                            initialSelectedIndex = state.selectedHoldIndex,
                            holdTapAreaSize = state.holdTapAreaSize,
                            displayColorSettings = state.displayColorSettings,
                            isEditingExistingWall = state.currentWallId != null,
                            onHoldTapAreaSizeChange = onHoldTapAreaSizeChange,
                            onConfirm = onApplyEditedHoldsAndReturn,
                            onRequestBack = onBackFromHoldEditOperation,
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
                            onSaveChallenge = onSaveCurrentChallenge,
                            onDrawCountChange = onDrawCountChange,
                            onChallengeDifficultyRangeChange = onChallengeDifficultyRangeChange,
                            onDetourStrengthChange = onDetourStrengthChange,
                            onRouteWavinessChange = onRouteWavinessChange,
                            onStepDistanceVarianceChange = onStepDistanceVarianceChange,
                            onCorridorWidthChange = onCorridorWidthChange,
                            onExcludePreviouslyGeneratedHoldsChange = onExcludePreviouslyGeneratedHoldsChange,
                            onRandomStartGoalPairLimitChange = onRandomStartGoalPairLimitChange,
                            onRouteGenerationAttemptLimitChange = onRouteGenerationAttemptLimitChange,
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

                if (state.isBusy || isExternalBusy) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(state.isBusy, isExternalBusy) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false).consume()
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                        if (event.changes.none { it.pressed }) {
                                            break
                                        }
                                    }
                                }
                            }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(navigationBarBottomPadding)
                    .background(AppSurfaceColor)
            )
        }
    }

    if (state.showDiscardDialog) {
        AppConfirmDialog(
            title = stringResource(R.string.back_to_list),
            message = stringResource(R.string.discard_dialog_message),
            confirmText = stringResource(R.string.discard_dialog_confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = onDiscardChanges,
            onDismissRequest = onDismissDiscardDialog
        )
    }
}
