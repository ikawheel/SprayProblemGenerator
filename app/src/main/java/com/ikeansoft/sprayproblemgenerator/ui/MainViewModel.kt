package com.ikeansoft.sprayproblemgenerator.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.ikeansoft.sprayproblemgenerator.domain.hold.AutoExtractionTuning
import com.ikeansoft.sprayproblemgenerator.domain.challenge.RouteGenerationTuning
import com.ikeansoft.sprayproblemgenerator.domain.hold.buildHoldScoringOrder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.data.DisplayColorSettingsRepository
import com.ikeansoft.sprayproblemgenerator.data.WallStorageRepository
import com.ikeansoft.sprayproblemgenerator.model.CapturedOrientation
import com.ikeansoft.sprayproblemgenerator.model.DEFAULT_REACH_REFERENCE_LENGTH_CM
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.model.HoldPoint
import com.ikeansoft.sprayproblemgenerator.model.SavedWallSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class PostCropDestination {
    MANUAL,
    AUTO
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallStorageRepository(application.applicationContext)
    private val displayColorSettingsRepository =
        DisplayColorSettingsRepository(application.applicationContext)
    private val appContext = application.applicationContext
    private var autoExtractionRequestId = 0L

    private val _uiState = MutableStateFlow(
        MainUiState(
            displayColorSettings = displayColorSettingsRepository.load()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        reloadSavedWalls()
    }

    fun reloadSavedWalls() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val summaries = withContext(Dispatchers.IO) {
                repository.loadAllSummaries()
            }
            _uiState.value = buildListState(
                source = _uiState.value,
                savedWalls = summaries
            ).copy(isBusy = false)
        }
    }

    fun startNewWall() {
        _uiState.value = MainUiState(
            currentScreen = AppScreen.CAMERA,
            screenBackStack = listOf(AppScreen.LIST),
            savedWalls = _uiState.value.savedWalls,
            drawCountInput = _uiState.value.drawCountInput,
            holdTapAreaSize = _uiState.value.holdTapAreaSize,
            displayColorSettings = _uiState.value.displayColorSettings,
            challengeDifficultyScoreMin = _uiState.value.challengeDifficultyScoreMin,
            challengeDifficultyScoreMax = _uiState.value.challengeDifficultyScoreMax,
            autoExtractionTuning = _uiState.value.autoExtractionTuning,
            routeTuning = _uiState.value.routeTuning
        )
    }

    fun onPhotoCaptured(
        bitmap: Bitmap,
        capturedOrientation: CapturedOrientation,
        capturedRotationDegrees: Int
    ) {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.IMAGE_CROP,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.IMAGE_CROP
            ),
            currentWallId = null,
            capturedBitmap = bitmap,
            replacementBitmap = null,
            capturedOrientation = capturedOrientation,
            capturedRotationDegrees = capturedRotationDegrees,
            holds = emptyList(),
            autoExtractedHolds = emptyList(),
            autoExtractionWallSamplePoints = emptyList(),
            isAutoExtractionWallSamplingMode = false,
            reachCalibrationReference = null,
            reachCalibrationLengthInput = DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = true,
            reachCalibrationReturnToHoldEditor = false,
            reachCalibrationReturnToAutoExtraction = false,
            selectedHoldIndex = null,
            holdEditorTool = HoldEditorTool.ADD,
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
            message = null
        )
    }

    fun applyCapturedImageCropAndOpenManual(
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ) {
        applyCapturedImageCropInternal(
            leftFraction = leftFraction,
            topFraction = topFraction,
            rightFraction = rightFraction,
            bottomFraction = bottomFraction,
            destination = PostCropDestination.MANUAL
        )
    }

    fun applyCapturedImageCropAndOpenAuto(
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ) {
        applyCapturedImageCropInternal(
            leftFraction = leftFraction,
            topFraction = topFraction,
            rightFraction = rightFraction,
            bottomFraction = bottomFraction,
            destination = PostCropDestination.AUTO
        )
    }

    private fun applyCapturedImageCropInternal(
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float,
        destination: PostCropDestination
    ) {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(
                isBusy = true,
                message = null
            )

            val croppedBitmap = withContext(Dispatchers.Default) {
                cropBitmap(
                    bitmap = bitmap,
                    leftFraction = leftFraction,
                    topFraction = topFraction,
                    rightFraction = rightFraction,
                    bottomFraction = bottomFraction
                )
            }

            val currentState = _uiState.value
            if (croppedBitmap == null) {
                _uiState.value = currentState.copy(
                    isBusy = false,
                    message = text(R.string.message_image_crop_failed)
                )
                return@launch
            }

            val croppedOrientation = orientationForBitmap(croppedBitmap)
            when (destination) {
                PostCropDestination.MANUAL -> {
                    _uiState.value = buildManualHoldEditorState(
                        state = currentState,
                        bitmap = croppedBitmap,
                        capturedOrientation = croppedOrientation,
                        capturedRotationDegrees = 0
                    )
                }
                PostCropDestination.AUTO -> {
                    _uiState.value = buildAutoExtractionState(
                        state = currentState,
                        bitmap = croppedBitmap,
                        capturedOrientation = croppedOrientation,
                        capturedRotationDegrees = 0
                    )
                    runAutoHoldExtraction(
                        bitmap = croppedBitmap,
                        tuning = currentState.autoExtractionTuning,
                        wallSamplePoints = emptyList()
                    )
                }
            }
        }
    }

    fun openManualHoldRegistrationAfterCapture() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.HOLD_EDITOR
            ),
            holds = emptyList(),
            autoExtractedHolds = emptyList(),
            selectedHoldIndex = null,
            holdEditorTool = HoldEditorTool.ADD,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            reachCalibrationReturnToHoldEditor = false,
            reachCalibrationReturnToAutoExtraction = false,
            message = null
        )
    }

    fun openAutoHoldExtractionAfterCapture() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: run {
            _uiState.value = state.copy(message = text(R.string.message_image_missing_to_save))
            return
        }

        _uiState.value = state.copy(
            currentScreen = AppScreen.AUTO_HOLD_EXTRACTION,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.AUTO_HOLD_EXTRACTION
            ),
            holds = emptyList(),
            selectedHoldIndex = null,
            autoExtractedHolds = emptyList(),
            isAutoExtractionWallSamplingMode = false,
            reachCalibrationReturnToHoldEditor = false,
            reachCalibrationReturnToAutoExtraction = false,
            isBusy = true,
            message = null
        )
        runAutoHoldExtraction(
            bitmap = bitmap,
            tuning = state.autoExtractionTuning,
            wallSamplePoints = state.autoExtractionWallSamplePoints
        )
    }

    fun backToHoldRegistrationMethodSelection() {
        _uiState.value = buildBackToHoldRegistrationMethodState(
            state = _uiState.value,
            popScreenState = ::popScreenState,
            message = text(R.string.message_hold_registration_method_select)
        )
    }

    fun onAutoExtractedHoldTapped(index: Int?) {
        val state = _uiState.value
        _uiState.value = state.copy(selectedHoldIndex = index)
    }

    fun onAutoExtractionTuningChanged(tuning: AutoExtractionTuning) {
        val state = _uiState.value
        val bitmap = state.capturedBitmap
        _uiState.value = buildAutoExtractionTuningUpdatedState(
            state = state,
            tuning = tuning
        )
        if (state.currentScreen == AppScreen.AUTO_HOLD_EXTRACTION && bitmap != null) {
            runAutoHoldExtraction(
                bitmap = bitmap,
                tuning = tuning,
                wallSamplePoints = state.autoExtractionWallSamplePoints
            )
        }
    }

    fun startAutoExtractionWallSampling() {
        _uiState.value = buildAutoExtractionWallSamplingStartedState(
            state = _uiState.value,
            message = text(R.string.message_auto_hold_extraction_wall_sample_mode)
        )
    }

    fun stopAutoExtractionWallSampling() {
        _uiState.value = buildAutoExtractionWallSamplingStoppedState(
            state = _uiState.value
        )
    }

    fun onAutoExtractionWallSamplePointSelected(point: HoldPoint) {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: return
        if (state.currentScreen != AppScreen.AUTO_HOLD_EXTRACTION) return
        if (!state.isAutoExtractionWallSamplingMode) return

        val updatedPoints = (state.autoExtractionWallSamplePoints + point)
            .distinct()
            .take(AUTO_EXTRACTION_WALL_SAMPLE_TARGET_COUNT)

        _uiState.value = buildAutoExtractionWallSamplePointSelectedState(
            state = state,
            point = point,
            message = text(
                R.string.message_auto_hold_extraction_wall_sample_added,
                updatedPoints.size
            )
        )

        if (updatedPoints.size == AUTO_EXTRACTION_WALL_SAMPLE_TARGET_COUNT) {
            runAutoHoldExtraction(
                bitmap = bitmap,
                tuning = state.autoExtractionTuning,
                wallSamplePoints = updatedPoints
            )
        }
    }

    fun clearAutoExtractionWallSamplePoints() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap
        _uiState.value = buildClearedAutoExtractionWallSamplePointsState(
            state = state,
            message = text(R.string.message_auto_hold_extraction_wall_sample_cleared)
        )
        if (state.currentScreen == AppScreen.AUTO_HOLD_EXTRACTION && bitmap != null) {
            runAutoHoldExtraction(
                bitmap = bitmap,
                tuning = state.autoExtractionTuning,
                wallSamplePoints = emptyList()
            )
        }
    }

    fun applyAutoExtractedHoldsAndContinue() {
        val state = _uiState.value
        _uiState.value = buildHoldEditorStateFromAutoExtractedHolds(
            state = state,
            pushedScreenBackStack = ::pushedScreenBackStack,
            message = text(R.string.message_auto_hold_extraction_applied)
        )
    }

    fun openReachCalibrationScreen() {
        _uiState.value = buildReachCalibrationScreenState(
            state = _uiState.value,
            pushedScreenBackStack = ::pushedScreenBackStack,
            firstPointMessage = text(R.string.message_reach_first_point),
            confirmMessage = text(R.string.message_reach_confirm)
        )
    }

    fun openDisplayColorSettings() {
        _uiState.value = buildDisplayColorSettingsScreenState(
            state = _uiState.value,
            pushedScreenBackStack = ::pushedScreenBackStack
        )
    }

    fun openLicenses() {
        _uiState.value = buildLicensesScreenState(
            state = _uiState.value,
            pushedScreenBackStack = ::pushedScreenBackStack
        )
    }

    fun updateDisplayColor(target: DisplayColorTarget, color: EditableRgbColor) {
        val state = _uiState.value
        val updatedSettings = buildUpdatedDisplayColorSettings(
            settings = state.displayColorSettings,
            target = target,
            color = color
        )
        if (updatedSettings == state.displayColorSettings) return

        _uiState.value = state.copy(displayColorSettings = updatedSettings)
        viewModelScope.launch(Dispatchers.IO) {
            displayColorSettingsRepository.save(updatedSettings)
        }
    }

    fun updateDisplayStrokeWidth(target: DisplayColorTarget, strokeWidth: Int) {
        val state = _uiState.value
        val updatedSettings = buildUpdatedDisplayStrokeWidthSettings(
            settings = state.displayColorSettings,
            target = target,
            strokeWidth = strokeWidth
        )
        if (updatedSettings == state.displayColorSettings) return

        _uiState.value = state.copy(displayColorSettings = updatedSettings)
        viewModelScope.launch(Dispatchers.IO) {
            displayColorSettingsRepository.save(updatedSettings)
        }
    }

    fun resetDisplayColorSettings() {
        val defaults = DisplayColorSettings()
        _uiState.value = _uiState.value.copy(displayColorSettings = defaults)
        viewModelScope.launch(Dispatchers.IO) {
            displayColorSettingsRepository.save(defaults)
        }
    }

    fun continueToHoldEditorFromReachCalibration() {
        val state = _uiState.value
        if (state.capturedBitmap == null) return
        val validation = validateConfiguredReachReference(
            state = state,
            inputLengthMessage = text(R.string.message_reach_input_length),
            setRequiredMessage = text(R.string.message_reach_set_required)
        )
        val normalizedReference = validation.reference
        if (normalizedReference == null) {
            _uiState.value = state.copy(message = validation.message)
            return
        }

        _uiState.value = buildHoldAttributeEditorStateFromReachCalibration(
            state = state,
            normalizedReference = normalizedReference,
            pushedScreenBackStack = ::pushedScreenBackStack,
            message = text(R.string.message_reach_go_hold_editor)
        )
    }

    fun continueToAutoHoldExtractionFromReachCalibration() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: return
        val validation = validateConfiguredReachReference(
            state = state,
            inputLengthMessage = text(R.string.message_reach_input_length),
            setRequiredMessage = text(R.string.message_reach_set_required)
        )
        val normalizedReference = validation.reference
        if (normalizedReference == null) {
            _uiState.value = state.copy(message = validation.message)
            return
        }

        _uiState.value = buildAutoExtractionStateFromReachCalibration(
            state = state,
            normalizedReference = normalizedReference,
            pushedScreenBackStack = ::pushedScreenBackStack
        )
        runAutoHoldExtraction(
            bitmap = bitmap,
            tuning = state.autoExtractionTuning,
            wallSamplePoints = state.autoExtractionWallSamplePoints
        )
    }

    fun backFromReachCalibration() {
        _uiState.value = buildBackFromReachCalibrationState(
            state = _uiState.value,
            popScreenState = ::popScreenState
        )
    }

    fun openSavedWall(wallId: String) {
        openSavedWallForHoldEditor(wallId)
    }

    fun openSavedWallForEditing(wallId: String) {
        openSavedWallForHoldEditor(wallId)
    }

    fun openSavedWallForReachCalibration(wallId: String) {
        openSavedWallIntoScreen(
            wallId = wallId,
            targetScreen = AppScreen.REACH_CALIBRATION
        )
    }

    fun openSavedWallForHoldEditor(wallId: String) {
        openSavedWallIntoScreen(
            wallId = wallId,
            targetScreen = AppScreen.HOLD_EDITOR
        )
    }

    fun openSavedWallForHoldAttributeEditor(wallId: String) {
        openSavedWallIntoScreen(
            wallId = wallId,
            targetScreen = AppScreen.HOLD_ATTRIBUTE_EDITOR
        )
    }

    fun openSavedWallForHoldScoring(wallId: String) {
        openSavedWallIntoScreen(
            wallId = wallId,
            targetScreen = AppScreen.HOLD_SCORING
        )
    }

    fun openWallImageReplacement(
        wallId: String,
        replacementBitmap: Bitmap
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(isBusy = true)
            val detail = withContext(Dispatchers.IO) { repository.loadWall(wallId) }
            if (detail == null) {
                val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
                _uiState.value = buildListState(
                    source = currentState,
                    savedWalls = refreshed,
                    message = text(R.string.message_open_wall_failed)
                )
                return@launch
            }

            _uiState.value = currentState.copy(
                currentScreen = AppScreen.WALL_IMAGE_REPLACEMENT,
                screenBackStack = pushedScreenBackStack(
                    state = currentState,
                    targetScreen = AppScreen.WALL_IMAGE_REPLACEMENT
                ),
                currentWallId = detail.id,
                capturedBitmap = detail.bitmap,
                replacementBitmap = replacementBitmap,
                capturedOrientation = detail.capturedOrientation,
                capturedRotationDegrees = detail.capturedRotationDegrees,
                holds = detail.holds,
                autoExtractedHolds = emptyList(),
                reachCalibrationReference = detail.reachCalibrationReference,
                reachCalibrationLengthInput = detail.reachCalibrationReference
                    ?.referenceLengthCm
                    ?.toString()
                    ?: DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                reachCalibrationReturnToAutoExtraction = false,
                selectedHoldIndex = null,
                savedChallenges = emptyList(),
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                lastGeneratedIntermediateHoldIndices = emptySet(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                isHoldEditorDirty = false,
                holdScoringPosition = 0,
                challengeFlowBackStack = emptyList(),
                showDiscardDialog = false,
                isBusy = false,
                message = null
            )
        }
    }

    fun openSavedWallForChallenge(wallId: String) {
        openSavedWallForChallenge(wallId, null)
    }

    fun openSavedWallChallenges(wallId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(isBusy = true)
            val wallDetail = withContext(Dispatchers.IO) { repository.loadWall(wallId) }
            val savedChallenges = withContext(Dispatchers.IO) {
                repository.loadChallengeSummaries(wallId)
            }

            if (wallDetail == null) {
                _uiState.value = currentState.copy(
                    isBusy = false,
                    message = text(R.string.message_open_wall_failed)
                )
                return@launch
            }

            _uiState.value = currentState.copy(
                currentScreen = AppScreen.SAVED_CHALLENGE_LIST,
                screenBackStack = pushedScreenBackStack(
                    state = currentState,
                    targetScreen = AppScreen.SAVED_CHALLENGE_LIST
                ),
                currentWallId = wallDetail.id,
                capturedBitmap = wallDetail.bitmap,
                replacementBitmap = null,
                capturedOrientation = wallDetail.capturedOrientation,
                capturedRotationDegrees = wallDetail.capturedRotationDegrees,
                holds = wallDetail.holds,
                reachCalibrationReference = wallDetail.reachCalibrationReference,
                reachCalibrationLengthInput = wallDetail.reachCalibrationReference
                    ?.referenceLengthCm
                    ?.toString()
                    ?: DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
                savedChallenges = savedChallenges,
                selectedHoldIndex = null,
                isBusy = false,
                message = null
            )
        }
    }

    fun openSavedChallenge(wallId: String, challengeId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(isBusy = true)

            val wallDetail = withContext(Dispatchers.IO) { repository.loadWall(wallId) }
            val challengeDetail = withContext(Dispatchers.IO) {
                repository.loadChallenge(wallId, challengeId)
            }

            if (wallDetail == null || challengeDetail == null) {
                val savedChallenges = withContext(Dispatchers.IO) {
                    repository.loadChallengeSummaries(wallId)
                }
                _uiState.value = currentState.copy(
                    currentScreen = AppScreen.SAVED_CHALLENGE_LIST,
                    screenBackStack = currentState.screenBackStack,
                    currentWallId = wallId,
                    savedChallenges = savedChallenges,
                    isBusy = false,
                    message = text(R.string.message_open_saved_challenge_failed)
                )
                return@launch
            }

            _uiState.value = currentState.copy(
                currentScreen = AppScreen.SAVED_CHALLENGE_DETAIL,
                screenBackStack = pushedScreenBackStack(
                    state = currentState,
                    targetScreen = AppScreen.SAVED_CHALLENGE_DETAIL
                ),
                currentWallId = wallDetail.id,
                capturedBitmap = wallDetail.bitmap,
                replacementBitmap = null,
                capturedOrientation = wallDetail.capturedOrientation,
                capturedRotationDegrees = wallDetail.capturedRotationDegrees,
                holds = wallDetail.holds,
                autoExtractedHolds = emptyList(),
                reachCalibrationReference = wallDetail.reachCalibrationReference,
                reachCalibrationLengthInput = wallDetail.reachCalibrationReference
                    ?.referenceLengthCm
                    ?.toString()
                    ?: DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                reachCalibrationReturnToAutoExtraction = false,
                selectedHoldIndex = null,
                challengeHoldIndices = challengeDetail.challengeHoldIndices,
                challengeOrderedHoldIndices = challengeDetail.challengeOrderedHoldIndices,
                challengeGenerationMethod = challengeDetail.generationMethodName
                    ?.let { methodName ->
                        runCatching { ChallengeGenerationMethod.valueOf(methodName) }.getOrNull()
                    },
                challengeFlowBackStack = emptyList(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = challengeDetail.startHoldIndex,
                goalHoldIndex = challengeDetail.goalHoldIndex,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                isBusy = false,
                message = null
            )
        }
    }

    fun deleteSavedChallenge(challengeId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val wallId = currentState.currentWallId
            if (wallId.isNullOrBlank()) {
                _uiState.value = currentState.copy(
                    message = text(R.string.message_delete_saved_challenge_failed)
                )
                return@launch
            }

            _uiState.value = currentState.copy(isBusy = true)

            val savedChallenges = withContext(Dispatchers.IO) {
                runCatching {
                    repository.deleteChallenge(wallId, challengeId)
                    repository.loadChallengeSummaries(wallId)
                }.getOrNull()
            }

            if (savedChallenges == null) {
                _uiState.value = currentState.copy(
                    isBusy = false,
                    message = text(R.string.message_delete_saved_challenge_failed)
                )
                return@launch
            }

            _uiState.value = currentState.copy(
                savedChallenges = savedChallenges,
                isBusy = false,
                message = text(R.string.message_saved_challenge_deleted)
            )
        }
    }

    fun openSavedWallForManualStartGoalChallenge(wallId: String) {
        openSavedWallForChallenge(wallId, ChallengeGenerationMethod.MANUAL_START_GOAL)
    }

    fun openSavedWallForRandomStartGoalChallenge(wallId: String) {
        openSavedWallForChallenge(wallId, ChallengeGenerationMethod.RANDOM_START_GOAL)
    }

    private fun openSavedWallForChallenge(
        wallId: String,
        initialMethod: ChallengeGenerationMethod?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val detail = withContext(Dispatchers.IO) { repository.loadWall(wallId) }
            if (detail == null) {
                val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
                _uiState.value = MainUiState(
                    currentScreen = AppScreen.LIST,
                    savedWalls = refreshed,
                    drawCountInput = _uiState.value.drawCountInput,
                    holdTapAreaSize = _uiState.value.holdTapAreaSize,
                    challengeDifficultyScoreMin = _uiState.value.challengeDifficultyScoreMin,
                    challengeDifficultyScoreMax = _uiState.value.challengeDifficultyScoreMax,
                    autoExtractionTuning = _uiState.value.autoExtractionTuning,
                    routeTuning = _uiState.value.routeTuning,
                    message = text(R.string.message_open_wall_failed)
                )
                return@launch
            }

            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                currentScreen = AppScreen.CHALLENGE_CREATOR,
                screenBackStack = pushedScreenBackStack(
                    state = currentState,
                    targetScreen = AppScreen.CHALLENGE_CREATOR
                ),
                currentWallId = detail.id,
                capturedBitmap = detail.bitmap,
                replacementBitmap = null,
                capturedOrientation = detail.capturedOrientation,
                capturedRotationDegrees = detail.capturedRotationDegrees,
                holds = detail.holds,
                autoExtractedHolds = emptyList(),
                reachCalibrationReference = detail.reachCalibrationReference,
                reachCalibrationLengthInput = detail.reachCalibrationReference
                    ?.referenceLengthCm
                    ?.toString()
                    ?: DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                reachCalibrationReturnToAutoExtraction = false,
                selectedHoldIndex = null,
                savedChallenges = emptyList(),
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                lastGeneratedIntermediateHoldIndices = emptySet(),
                challengeGenerationMethod = initialMethod,
                challengeFlowStep = ChallengeFlowStep.COMMON_SETTINGS,
                challengeFlowBackStack = emptyList(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                holdScoringPosition = 0,
                isBusy = false,
                showDiscardDialog = false,
                message = text(R.string.message_open_challenge_creator)
            )
        }
    }

    fun saveCurrentChallenge() {
        val state = _uiState.value
        val wallId = state.currentWallId
        val startHoldIndex = state.startHoldIndex
        val goalHoldIndex = state.goalHoldIndex
        if (wallId.isNullOrBlank()) {
            _uiState.value = state.copy(message = text(R.string.message_save_challenge_failed))
            return
        }
        if (startHoldIndex == null || goalHoldIndex == null || state.challengeOrderedHoldIndices.isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_no_generated_challenge_to_save))
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isBusy = true)
            val savedChallenge = withContext(Dispatchers.IO) {
                runCatching {
                    repository.saveChallenge(
                        wallId = wallId,
                        generationMethodName = state.challengeGenerationMethod?.name,
                        startHoldIndex = startHoldIndex,
                        goalHoldIndex = goalHoldIndex,
                        challengeHoldIndices = state.challengeHoldIndices,
                        challengeOrderedHoldIndices = state.challengeOrderedHoldIndices
                    )
                }.getOrNull()
            }

            _uiState.value = state.copy(
                isBusy = false,
                message = if (savedChallenge != null) {
                    text(R.string.message_saved_challenge)
                } else {
                    text(R.string.message_save_challenge_failed)
                }
            )
        }
    }

    fun onHoldTapAreaSizeChanged(size: HoldTapAreaSize) {
        _uiState.value = _uiState.value.copy(
            holdTapAreaSize = size
        )
    }

    fun onHoldEditorToolChanged(tool: HoldEditorTool) {
        val state = _uiState.value
        _uiState.value = state.copy(
            holdEditorTool = tool,
            message = null
        )
    }

    fun openHoldEditOperation(tool: HoldEditorTool) {
        val state = _uiState.value
        if (state.currentScreen != AppScreen.HOLD_EDITOR) return
        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_EDIT_OPERATION,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.HOLD_EDIT_OPERATION
            ),
            holdEditorTool = tool,
            message = null
        )
    }

    fun addManualHold(hold: Hold) {
        val state = _uiState.value
        val updatedHolds = state.holds + hold
        _uiState.value = state.copy(
            holds = updatedHolds,
            selectedHoldIndex = updatedHolds.lastIndex,
            isHoldEditorDirty = true,
            holdScoringPosition = 0,
            message = text(R.string.message_hold_added, updatedHolds.size)
        )
    }

    fun replaceHoldWithEditedHolds(targetIndex: Int, replacementHolds: List<Hold>) {
        val state = _uiState.value
        if (targetIndex !in state.holds.indices) return

        val updatedHolds = buildList {
            addAll(state.holds.take(targetIndex))
            addAll(replacementHolds)
            addAll(state.holds.drop(targetIndex + 1))
        }

        val message = when {
            replacementHolds.isEmpty() -> text(R.string.message_hold_deleted)
            replacementHolds.size == 1 -> text(R.string.message_hold_updated)
            else -> text(R.string.message_hold_split, replacementHolds.size)
        }

        _uiState.value = state.copy(
            holds = updatedHolds,
            selectedHoldIndex = replacementHolds.firstOrNull()?.let { targetIndex },
            isHoldEditorDirty = true,
            holdScoringPosition = 0,
            message = message
        )
    }

    fun replaceAllHolds(updatedHolds: List<Hold>, selectedIndex: Int?) {
        val state = _uiState.value
        val normalizedSelectedIndex = selectedIndex?.takeIf { it in updatedHolds.indices }
        _uiState.value = state.copy(
            holds = updatedHolds,
            selectedHoldIndex = normalizedSelectedIndex,
            isHoldEditorDirty = state.isHoldEditorDirty || updatedHolds != state.holds,
            holdScoringPosition = 0,
            message = null
        )
    }

    fun saveEditedHoldsInHoldEditor(updatedHolds: List<Hold>, selectedIndex: Int?) {
        val state = _uiState.value
        if (state.currentScreen != AppScreen.HOLD_EDITOR) return

        val normalizedSelectedIndex = selectedIndex?.takeIf { it in updatedHolds.indices }
        if (state.currentWallId == null) {
            _uiState.value = state.copy(
                holds = updatedHolds,
                selectedHoldIndex = normalizedSelectedIndex,
                isHoldEditorDirty = true,
                holdScoringPosition = 0,
                message = null
            )
            return
        }

        val bitmap = state.capturedBitmap ?: run {
            _uiState.value = state.copy(message = text(R.string.message_image_missing_to_save))
            return
        }
        val normalizedReference = state.reachCalibrationReference.withCurrentLength(state)

        viewModelScope.launch {
            _uiState.value = state.copy(
                holds = updatedHolds,
                selectedHoldIndex = normalizedSelectedIndex,
                reachCalibrationReference = normalizedReference,
                isBusy = true
            )

            val savedSummary = withContext(Dispatchers.IO) {
                repository.saveWall(
                    wallId = state.currentWallId,
                    bitmap = bitmap,
                    holds = updatedHolds,
                    reachCalibrationReference = normalizedReference,
                    capturedOrientation = state.capturedOrientation,
                    capturedRotationDegrees = state.capturedRotationDegrees
                )
            }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            val latestState = _uiState.value
            _uiState.value = buildListState(
                source = latestState.copy(
                    currentWallId = savedSummary.id,
                    holds = updatedHolds,
                    selectedHoldIndex = normalizedSelectedIndex,
                    reachCalibrationReference = normalizedReference,
                    isHoldEditorDirty = false,
                    holdScoringPosition = 0,
                    isBusy = false
                ),
                savedWalls = refreshed,
                message = text(R.string.message_saved_wall)
            )
        }
    }

    fun applyEditedHoldsAndReturnToHoldEditor(updatedHolds: List<Hold>, selectedIndex: Int?) {
        val state = _uiState.value
        if (state.currentScreen != AppScreen.HOLD_EDIT_OPERATION) return

        if (state.currentWallId != null) {
            val bitmap = state.capturedBitmap ?: run {
                _uiState.value = state.copy(message = text(R.string.message_image_missing_to_save))
                return
            }
            val normalizedSelectedIndex = selectedIndex?.takeIf { it in updatedHolds.indices }
            val normalizedReference = state.reachCalibrationReference.withCurrentLength(state)

            viewModelScope.launch {
                _uiState.value = state.copy(
                    holds = updatedHolds,
                    selectedHoldIndex = normalizedSelectedIndex,
                    reachCalibrationReference = normalizedReference,
                    isBusy = true
                )

                val savedSummary = withContext(Dispatchers.IO) {
                    repository.saveWall(
                        wallId = state.currentWallId,
                        bitmap = bitmap,
                        holds = updatedHolds,
                        reachCalibrationReference = normalizedReference,
                        capturedOrientation = state.capturedOrientation,
                        capturedRotationDegrees = state.capturedRotationDegrees
                    )
                }
                val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
                val latestState = _uiState.value
                _uiState.value = popScreenState(
                    state = latestState.copy(
                        currentWallId = savedSummary.id,
                        savedWalls = refreshed,
                        holds = updatedHolds,
                        selectedHoldIndex = normalizedSelectedIndex,
                        reachCalibrationReference = normalizedReference,
                        isHoldEditorDirty = false,
                        isBusy = false,
                        message = text(R.string.message_saved_wall)
                    )
                )
            }
            return
        }

        val normalizedSelectedIndex = selectedIndex?.takeIf { it in updatedHolds.indices }
        _uiState.value = popScreenState(
            state = state.copy(
                holds = updatedHolds,
                selectedHoldIndex = normalizedSelectedIndex,
                isHoldEditorDirty = state.isHoldEditorDirty || updatedHolds != state.holds,
                holdScoringPosition = 0,
                message = null
            )
        )
    }

    fun onEditorHoldTapped(index: Int?) {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedHoldIndex = index,
            message = index?.let {
                val hold = state.holds[it]
                text(
                    R.string.selected_hold_debug,
                    it,
                    hold.centerX,
                    hold.centerY,
                    hold.points.size
                )
            }
        )
    }

    fun toggleSelectedHoldStartCandidate() {
        updateSelectedHoldAttribute { hold ->
            hold.copy(isStartCandidate = !hold.isStartCandidate)
        }
    }

    fun toggleSelectedHoldGoalCandidate() {
        updateSelectedHoldAttribute { hold ->
            hold.copy(isGoalCandidate = !hold.isGoalCandidate)
        }
    }

    fun assignHoldAsStartCandidate(index: Int?) {
        updateHoldAttribute(index) { hold ->
            hold.copy(isStartCandidate = true)
        }
    }

    fun assignHoldAsGoalCandidate(index: Int?) {
        updateHoldAttribute(index) { hold ->
            hold.copy(isGoalCandidate = true)
        }
    }

    fun clearHoldAttributes(index: Int?) {
        updateHoldAttribute(index) { hold ->
            hold.copy(
                isStartCandidate = false,
                isGoalCandidate = false
            )
        }
    }

    fun removeSelectedHold() {
        val state = _uiState.value
        val selected = state.selectedHoldIndex ?: run {
            _uiState.value = state.copy(message = text(R.string.message_select_hold_to_delete))
            return
        }

        _uiState.value = state.copy(
            holds = state.holds.toMutableList().apply { removeAt(selected) },
            selectedHoldIndex = null,
            isHoldEditorDirty = true,
            holdScoringPosition = 0,
            message = text(R.string.message_hold_deleted)
        )
    }

    fun openHoldAttributeEditor() {
        val state = _uiState.value
        if (state.holds.isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_register_hold_first))
            return
        }

        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_ATTRIBUTE_EDITOR,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.HOLD_ATTRIBUTE_EDITOR
            ),
            selectedHoldIndex = null,
            showDiscardDialog = false,
            message = null
        )
    }

    fun returnToHoldEditorFromAttributeEditor() {
        val state = _uiState.value
        _uiState.value = popScreenState(
            state = state.copy(
                selectedHoldIndex = null,
                message = null
            )
        )
    }

    fun openHoldScoring() {
        val state = _uiState.value
        if (state.holds.isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_register_hold_first))
            return
        }

        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_SCORING,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.HOLD_SCORING
            ),
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            showDiscardDialog = false,
            message = null
        )
    }

    fun returnToHoldAttributeEditorFromScoring() {
        val state = _uiState.value
        _uiState.value = popScreenState(
            state = state.copy(
                selectedHoldIndex = null,
                message = null
            )
        )
    }

    fun setCurrentHoldDifficultyScore(score: Int) {
        val state = _uiState.value
        val orderedIndices = buildHoldScoringOrder(state.holds)
        val currentIndex = orderedIndices.getOrNull(state.holdScoringPosition) ?: return
        val updatedHolds = state.holds.toMutableList().apply {
            this[currentIndex] = this[currentIndex].copy(difficultyScore = score)
        }

        _uiState.value = state.copy(
            holds = updatedHolds,
            isHoldEditorDirty = true,
            holdScoringPosition = (state.holdScoringPosition + 1).coerceAtMost(orderedIndices.size),
            message = null
        )
    }

    fun saveWallAndReturnToList() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: run {
            _uiState.value = state.copy(message = text(R.string.message_image_missing_to_save))
            return
        }
        if (state.currentScreen == AppScreen.REACH_CALIBRATION &&
            parseReachCalibrationLengthCentimeters(state) == null
        ) {
            _uiState.value = state.copy(message = text(R.string.message_reach_input_length))
            return
        }

        viewModelScope.launch {
            val normalizedReference = state.reachCalibrationReference.withCurrentLength(state)
            _uiState.value = state.copy(
                isBusy = true,
                reachCalibrationReference = normalizedReference
            )
            val savedSummary = withContext(Dispatchers.IO) {
                repository.saveWall(
                    wallId = state.currentWallId,
                    bitmap = bitmap,
                    holds = state.holds,
                    reachCalibrationReference = normalizedReference,
                    capturedOrientation = state.capturedOrientation,
                    capturedRotationDegrees = state.capturedRotationDegrees
                )
            }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            _uiState.value = buildListState(
                source = state.copy(
                    currentWallId = savedSummary.id,
                    reachCalibrationReference = normalizedReference,
                    isHoldEditorDirty = false,
                    showDiscardDialog = false,
                    isBusy = false
                ),
                savedWalls = refreshed,
                message = text(R.string.message_saved_wall)
            )
        }
    }

    fun saveWallImageReplacement(replacementBitmap: Bitmap) {
        val state = _uiState.value
        val wallId = state.currentWallId ?: run {
            _uiState.value = state.copy(message = text(R.string.message_replace_wall_image_failed))
            return
        }
        if (state.currentScreen != AppScreen.WALL_IMAGE_REPLACEMENT) return

        viewModelScope.launch {
            val normalizedReference = state.reachCalibrationReference.withCurrentLength(state)
            _uiState.value = state.copy(
                isBusy = true,
                reachCalibrationReference = normalizedReference
            )
            val savedSummary = withContext(Dispatchers.IO) {
                repository.saveWall(
                    wallId = wallId,
                    bitmap = replacementBitmap,
                    holds = state.holds,
                    reachCalibrationReference = normalizedReference,
                    capturedOrientation = state.capturedOrientation,
                    capturedRotationDegrees = state.capturedRotationDegrees
                )
            }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            _uiState.value = buildListState(
                source = state.copy(
                    currentWallId = savedSummary.id,
                    capturedBitmap = replacementBitmap,
                    replacementBitmap = null,
                    reachCalibrationReference = normalizedReference,
                    isHoldEditorDirty = false,
                    showDiscardDialog = false,
                    isBusy = false
                ),
                savedWalls = refreshed,
                message = text(R.string.message_wall_image_replaced)
            )
        }
    }

    fun saveWallAndOpenChallenge() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: run {
            _uiState.value = state.copy(message = text(R.string.message_image_missing_to_save))
            return
        }

        viewModelScope.launch {
            val normalizedReference = state.reachCalibrationReference.withCurrentLength(state)
            _uiState.value = state.copy(
                isBusy = true,
                reachCalibrationReference = normalizedReference
            )
            val savedSummary = withContext(Dispatchers.IO) {
                repository.saveWall(
                    wallId = state.currentWallId,
                    bitmap = bitmap,
                    holds = state.holds,
                    reachCalibrationReference = normalizedReference,
                    capturedOrientation = state.capturedOrientation,
                    capturedRotationDegrees = state.capturedRotationDegrees
                )
            }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            _uiState.value = state.copy(
                currentScreen = AppScreen.CHALLENGE_CREATOR,
                screenBackStack = pushedScreenBackStack(
                    state = state,
                    targetScreen = AppScreen.CHALLENGE_CREATOR
                ),
                savedWalls = refreshed,
                currentWallId = savedSummary.id,
                selectedHoldIndex = null,
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                lastGeneratedIntermediateHoldIndices = emptySet(),
                challengeGenerationMethod = null,
                challengeFlowStep = ChallengeFlowStep.METHOD_SELECT,
                challengeFlowBackStack = emptyList(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                reachCalibrationReturnToAutoExtraction = false,
                autoExtractedHolds = emptyList(),
                isHoldEditorDirty = false,
                holdScoringPosition = 0,
                showDiscardDialog = false,
                isBusy = false,
                message = text(R.string.message_saved_and_open_challenge)
            )
        }
    }

    fun startReachCalibrationSelection() {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = true,
            message = text(R.string.message_reach_first_point)
        )
    }

    fun onReachCalibrationLengthInputChanged(value: String) {
        val state = _uiState.value
        val updatedState = buildReachCalibrationLengthInputChangedState(
            state = state,
            value = value
        )
        _uiState.value = updatedState.copy(
            isHoldEditorDirty = state.isHoldEditorDirty ||
                (updatedState.reachCalibrationLengthInput != state.reachCalibrationLengthInput) ||
                (updatedState.reachCalibrationReference != state.reachCalibrationReference)
        )
    }

    fun clearReachCalibration() {
        _uiState.value = buildClearedReachCalibrationState(
            state = _uiState.value,
            message = text(R.string.message_reach_cleared)
        )
    }

    fun onReachCalibrationPointSelected(point: HoldPoint) {
        val state = _uiState.value
        if (!state.isReachCalibrationSelectionMode) return

        val firstPoint = state.pendingReachCalibrationPoint
        if (firstPoint == point) {
            _uiState.value = state.copy(
                message = text(R.string.message_reach_select_different_second_point)
            )
            return
        }

        val referenceLengthCm = parseReachCalibrationLengthCentimeters(state)
        if (referenceLengthCm == null) {
            _uiState.value = state.copy(message = text(R.string.message_reach_input_length))
            return
        }

        _uiState.value = buildReachCalibrationPointSelectedState(
            state = state.copy(
                reachCalibrationReference = state.reachCalibrationReference?.copy(
                    referenceLengthCm = referenceLengthCm
                )
            ),
            point = point,
            messageSelectSecondPoint = text(R.string.message_reach_second_point),
            messageSet = text(R.string.message_reach_set)
        )
    }

    fun deleteSavedWall(wallId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            withContext(Dispatchers.IO) { repository.deleteWall(wallId) }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.LIST,
                savedWalls = refreshed,
                isBusy = false,
                message = text(R.string.message_wall_deleted)
            )
        }
    }

    fun onChallengeHoldTapped(index: Int?) {
        val result = buildChallengeHoldTappedResult(
            state = _uiState.value,
            index = index,
            text = { resId, args -> text(resId, *args) }
        )
        _uiState.value = result.state.copy(message = result.message ?: result.state.message)
    }

    fun onDrawCountChanged(value: String) {
        _uiState.value = _uiState.value.copy(drawCountInput = value.filter { it.isDigit() })
    }

    fun onChallengeDifficultyRangeChanged(start: Float, endInclusive: Float) {
        _uiState.value = buildChallengeDifficultyRangeState(
            state = _uiState.value,
            start = start,
            endInclusive = endInclusive
        )
    }

    fun onExcludePreviouslyGeneratedHoldsChanged(value: Boolean) {
        updateRouteTuning { copy(excludePreviouslyGeneratedHolds = value) }
    }

    fun onRandomStartGoalPairLimitChanged(value: Int) {
        updateRouteTuning { copy(randomStartGoalPairLimit = value.coerceIn(1, 64)) }
    }

    fun onRouteGenerationAttemptLimitChanged(value: Int) {
        updateRouteTuning { copy(routeGenerationAttemptLimit = value.coerceIn(1, 512)) }
    }

    fun selectManualStartGoalChallengeMethod() {
        selectChallengeGenerationMethod(ChallengeGenerationMethod.MANUAL_START_GOAL)
    }

    fun selectRandomStartGoalChallengeMethod() {
        val shouldGenerateImmediately =
            _uiState.value.challengeFlowStep == ChallengeFlowStep.COMMON_SETTINGS
        selectChallengeGenerationMethod(ChallengeGenerationMethod.RANDOM_START_GOAL)
        if (shouldGenerateImmediately) {
            drawRandomChallengeWithRandomStartGoal()
        }
    }

    fun openChallengeMethodSelection() {
        _uiState.value = buildChallengeFlowState(
            state = _uiState.value,
            targetStep = ChallengeFlowStep.METHOD_SELECT,
            pushedChallengeFlowBackStack = ::pushedChallengeFlowBackStack
        )
    }

    fun openChallengeCommonSettings() {
        _uiState.value = buildChallengeFlowState(
            state = _uiState.value,
            targetStep = ChallengeFlowStep.COMMON_SETTINGS,
            pushedChallengeFlowBackStack = ::pushedChallengeFlowBackStack
        )
    }

    fun openChallengeGeneration() {
        val result = buildChallengeGenerationStateResult(
            state = _uiState.value,
            pushedChallengeFlowBackStack = ::pushedChallengeFlowBackStack,
            selectMethodMessage = text(R.string.message_select_challenge_generation_method)
        )
        _uiState.value = result.state.copy(message = result.message ?: result.state.message)
    }

    fun openChallengeTuning() {
        _uiState.value = buildChallengeFlowState(
            state = _uiState.value,
            targetStep = ChallengeFlowStep.TUNING,
            pushedChallengeFlowBackStack = ::pushedChallengeFlowBackStack
        )
    }

    fun rerunCurrentChallengeGeneration() {
        when (_uiState.value.challengeGenerationMethod) {
            ChallengeGenerationMethod.MANUAL_START_GOAL -> drawRandomChallengeHolds()
            ChallengeGenerationMethod.RANDOM_START_GOAL -> drawRandomChallengeWithRandomStartGoal()
            null -> {
                val state = _uiState.value
                _uiState.value = state.copy(message = text(R.string.message_select_challenge_generation_method))
            }
        }
    }

    fun drawRandomChallengeHolds() {
        val state = _uiState.value
        if (state.holds.isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_no_holds))
            return
        }

        val requestedCountInput = state.drawCountInput.toIntOrNull()
        val requestedCount = requestedCountInput?.takeIf { it >= 2 }
        if (
            state.drawCountInput.isNotBlank() &&
            (requestedCountInput == null || requestedCountInput == 1)
        ) {
            _uiState.value = state.copy(message = text(R.string.message_invalid_draw_count))
            return
        }

        val startIndex = state.startHoldIndex ?: run {
            _uiState.value = state.copy(message = text(R.string.message_set_start))
            return
        }
        val goalIndex = state.goalHoldIndex ?: run {
            _uiState.value = state.copy(message = text(R.string.message_set_goal))
            return
        }
        if (startIndex == goalIndex) {
            _uiState.value = state.copy(message = text(R.string.message_start_goal_must_differ))
            return
        }

        val drawSourceIndices = challengeSelectionCandidateIndices(state).toMutableSet().apply {
            if (state.routeTuning.excludePreviouslyGeneratedHolds) {
                removeAll(state.lastGeneratedIntermediateHoldIndices)
            }
            add(startIndex)
            add(goalIndex)
        }
        if (drawSourceIndices.isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_no_draw_source))
            return
        }

        val randomizedRouteTuning = state.routeTuning.randomizedRouteShape()

        viewModelScope.launch {
            _uiState.value = state.copy(
                isBusy = true,
                routeTuning = randomizedRouteTuning
            )

            val selectedOrderedIndices = withContext(Dispatchers.Default) {
                generateChallengeRouteWithRetries(
                    holds = state.holds,
                    sourceIndices = drawSourceIndices,
                    startIndex = startIndex,
                    goalIndex = goalIndex,
                    targetCount = requestedCount,
                    tuning = randomizedRouteTuning,
                    reachCalibrationReference = state.reachCalibrationReference
                )
            }

            if (selectedOrderedIndices == null) {
                _uiState.value = state.copy(
                    isBusy = false,
                    routeTuning = randomizedRouteTuning,
                    message = if (state.reachCalibrationReference != null) {
                        text(R.string.message_unable_generate_with_reach)
                    } else {
                        text(R.string.message_unable_generate)
                    }
                )
                return@launch
            }

            val selectedIndices = selectedOrderedIndices.toSet()
            _uiState.value = state.copy(
                isBusy = false,
                routeTuning = randomizedRouteTuning,
                challengeHoldIndices = selectedIndices,
                challengeOrderedHoldIndices = selectedOrderedIndices,
                lastGeneratedIntermediateHoldIndices = selectedIndices
                    .filterNotTo(linkedSetOf()) { index -> index == startIndex || index == goalIndex },
                selectedHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                message = text(R.string.message_draw_generated)
            )
        }
    }

    fun drawRandomChallengeWithRandomStartGoal() {
        val state = _uiState.value
        if (state.holds.isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_no_holds))
            return
        }
        if (state.isDrawTargetSelectionMode) {
            _uiState.value = state.copy(message = text(R.string.message_finish_range_selection_first))
            return
        }

        val requestedCountInput = state.drawCountInput.toIntOrNull()
        val requestedCount = requestedCountInput?.takeIf { it >= 2 }
        if (
            state.drawCountInput.isNotBlank() &&
            (requestedCountInput == null || requestedCountInput == 1)
        ) {
            _uiState.value = state.copy(message = text(R.string.message_invalid_draw_count))
            return
        }

        val selectionCandidateIndices = challengeSelectionCandidateIndices(state)
        if (selectionCandidateIndices.size < 2) {
            _uiState.value = state.copy(message = text(R.string.message_random_start_goal_candidates_required))
            return
        }

        val randomizedRouteTuning = state.routeTuning.randomizedRouteShape()

        viewModelScope.launch {
            _uiState.value = state.copy(
                isBusy = true,
                routeTuning = randomizedRouteTuning
            )

            val generatedRoute = withContext(Dispatchers.Default) {
                generateChallengeRouteWithRandomStartGoal(
                    holds = state.holds,
                    selectionCandidateIndices = selectionCandidateIndices,
                    lastGeneratedIntermediateHoldIndices = state.lastGeneratedIntermediateHoldIndices,
                    targetCount = requestedCount,
                    tuning = randomizedRouteTuning,
                    reachCalibrationReference = state.reachCalibrationReference
                )
            }

            if (generatedRoute == null) {
                _uiState.value = state.copy(
                    isBusy = false,
                    routeTuning = randomizedRouteTuning,
                    message = if (state.reachCalibrationReference != null) {
                        text(R.string.message_unable_generate_with_reach)
                    } else {
                        text(R.string.message_unable_generate)
                    }
                )
                return@launch
            }

            val selectedIndices = generatedRoute.orderedIndices.toSet()
            _uiState.value = state.copy(
                isBusy = false,
                routeTuning = randomizedRouteTuning,
                selectedHoldIndex = null,
                startHoldIndex = generatedRoute.startIndex,
                goalHoldIndex = generatedRoute.goalIndex,
                challengeHoldIndices = selectedIndices,
                challengeOrderedHoldIndices = generatedRoute.orderedIndices,
                lastGeneratedIntermediateHoldIndices = selectedIndices
                    .filterNotTo(linkedSetOf()) { index ->
                        index == generatedRoute.startIndex || index == generatedRoute.goalIndex
                    },
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                message = text(R.string.message_draw_generated_random_start_goal)
            )
        }
    }

    fun startDrawTargetSelection() {
        _uiState.value = buildDrawTargetSelectionState(
            state = _uiState.value,
            message = text(R.string.message_draw_target_instruction)
        )
    }

    fun applyDrawTargetSelection(indices: Set<Int>) {
        _uiState.value = buildAppliedDrawTargetSelectionState(
            state = _uiState.value,
            indices = indices,
            emptyMessage = text(R.string.message_draw_target_empty_after_filter),
            selectedCountMessage = { count ->
                text(R.string.message_draw_target_selected_count, count)
            }
        )
    }

    fun startChallengeStartGoalSelection() {
        val result = buildChallengeStartGoalSelectionResult(
            state = _uiState.value,
            finishRangeSelectionFirstMessage = text(R.string.message_finish_range_selection_first),
            selectCandidateHoldsFirstMessage = text(R.string.message_select_candidate_holds_first),
            selectStartPromptMessage = text(R.string.message_select_start_prompt)
        )
        _uiState.value = result.state.copy(message = result.message ?: result.state.message)
    }

    fun clearChallengeSelection() {
        _uiState.value = buildClearedChallengeSelectionState(
            state = _uiState.value,
            message = text(R.string.message_challenge_cleared)
        )
    }

    fun onBackPressed() {
        val state = _uiState.value
        if (state.showDiscardDialog) {
            dismissDiscardDialog()
            return
        }

        if (state.currentScreen == AppScreen.LIST) {
            return
        }

        if (
            state.currentScreen in setOf(
                AppScreen.REACH_CALIBRATION,
                AppScreen.HOLD_EDITOR,
                AppScreen.HOLD_ATTRIBUTE_EDITOR,
                AppScreen.HOLD_SCORING
            ) &&
            state.currentWallId != null &&
            state.isHoldEditorDirty
        ) {
            _uiState.value = state.copy(
                showDiscardDialog = true,
                discardReturnToList = false
            )
            return
        }

        if (state.currentScreen == AppScreen.CHALLENGE_CREATOR) {
            when {
                state.isDrawTargetSelectionMode -> {
                    _uiState.value = state.copy(
                        isDrawTargetSelectionMode = false,
                        message = null
                    )
                }

                state.routeSelectionMode != RouteSelectionMode.NONE -> {
                    _uiState.value = state.copy(
                        routeSelectionMode = RouteSelectionMode.NONE,
                        message = null
                    )
                }

                state.challengeFlowBackStack.isNotEmpty() -> {
                    _uiState.value = popChallengeFlowState(state)
                }

                else -> {
                    _uiState.value = popScreenState(state)
                }
            }
            return
        }

        _uiState.value = popScreenState(state)
    }

    fun requestBackToList() {
        onBackPressed()
    }

    fun requestReturnToList() {
        val state = _uiState.value
        if (
            state.currentScreen in setOf(
                AppScreen.REACH_CALIBRATION,
                AppScreen.HOLD_EDITOR,
                AppScreen.HOLD_ATTRIBUTE_EDITOR,
                AppScreen.HOLD_SCORING
            ) &&
            state.currentWallId != null &&
            state.isHoldEditorDirty
        ) {
            _uiState.value = state.copy(
                showDiscardDialog = true,
                discardReturnToList = true
            )
            return
        }

        _uiState.value = buildListState(state)
    }

    fun dismissDiscardDialog() {
        _uiState.value = _uiState.value.copy(
            showDiscardDialog = false,
            discardReturnToList = false
        )
    }

    fun discardEditorAndReturnToList() {
        val currentState = _uiState.value
        val state = currentState.copy(
            showDiscardDialog = false,
            discardReturnToList = false
        )
        _uiState.value = if (currentState.discardReturnToList) {
            buildListState(state)
        } else {
            popScreenState(state = state)
        }
    }

    fun returnToList() {
        _uiState.value = buildListState(_uiState.value)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun text(@StringRes resId: Int, vararg args: Any): String {
        return appContext.getString(resId, *args)
    }

    private fun buildListState(
        source: MainUiState,
        savedWalls: List<SavedWallSummary> = source.savedWalls,
        message: String? = null
    ): MainUiState {
        return MainUiState(
            currentScreen = AppScreen.LIST,
            screenBackStack = emptyList(),
            savedWalls = savedWalls,
            savedChallenges = emptyList(),
            drawCountInput = source.drawCountInput,
            holdTapAreaSize = source.holdTapAreaSize,
            displayColorSettings = source.displayColorSettings,
            challengeDifficultyScoreMin = source.challengeDifficultyScoreMin,
            challengeDifficultyScoreMax = source.challengeDifficultyScoreMax,
            autoExtractionTuning = source.autoExtractionTuning,
            routeTuning = source.routeTuning,
            message = message
        )
    }

    private fun pushedScreenBackStack(
        state: MainUiState,
        targetScreen: AppScreen
    ): List<AppScreen> {
        return if (state.currentScreen == targetScreen) {
            state.screenBackStack
        } else {
            state.screenBackStack + state.currentScreen
        }
    }

    private fun pushedChallengeFlowBackStack(
        state: MainUiState,
        targetStep: ChallengeFlowStep
    ): List<ChallengeFlowStep> {
        return if (state.challengeFlowStep == targetStep) {
            state.challengeFlowBackStack
        } else {
            state.challengeFlowBackStack + state.challengeFlowStep
        }
    }

    private fun popScreenState(
        state: MainUiState
    ): MainUiState {
        val previousScreen = state.screenBackStack.lastOrNull()
        return if (previousScreen == null || previousScreen == AppScreen.LIST) {
            buildListState(state)
        } else {
            state.copy(
                currentScreen = previousScreen,
                screenBackStack = state.screenBackStack.dropLast(1),
                challengeFlowBackStack = emptyList(),
                showDiscardDialog = false,
                message = null
            )
        }
    }

    private fun popChallengeFlowState(
        state: MainUiState
    ): MainUiState {
        val previousStep = state.challengeFlowBackStack.lastOrNull() ?: return state
        return state.copy(
            challengeFlowStep = previousStep,
            challengeFlowBackStack = state.challengeFlowBackStack.dropLast(1),
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
        )
    }

    private fun updateRouteTuning(
        transform: RouteGenerationTuning.() -> RouteGenerationTuning
    ) {
        _uiState.value = _uiState.value.copy(
            routeTuning = _uiState.value.routeTuning.transform()
        )
    }

    private fun buildManualHoldEditorState(
        state: MainUiState,
        bitmap: Bitmap,
        capturedOrientation: CapturedOrientation,
        capturedRotationDegrees: Int
    ): MainUiState {
        return state.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.HOLD_EDITOR
            ),
            currentWallId = null,
            capturedBitmap = bitmap,
            replacementBitmap = null,
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
            isBusy = false,
            message = null
        )
    }

    private fun openSavedWallIntoScreen(
        wallId: String,
        targetScreen: AppScreen
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val detail = withContext(Dispatchers.IO) { repository.loadWall(wallId) }
            if (detail == null) {
                val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
                _uiState.value = buildListState(
                    source = _uiState.value,
                    savedWalls = refreshed,
                    message = text(R.string.message_open_wall_failed)
                )
                return@launch
            }

            val currentState = _uiState.value
            _uiState.value = currentState.copy(
                currentScreen = targetScreen,
                screenBackStack = pushedScreenBackStack(
                    state = currentState,
                    targetScreen = targetScreen
                ),
                currentWallId = detail.id,
                capturedBitmap = detail.bitmap,
                replacementBitmap = null,
                capturedOrientation = detail.capturedOrientation,
                capturedRotationDegrees = detail.capturedRotationDegrees,
                holds = detail.holds,
                autoExtractedHolds = emptyList(),
                reachCalibrationReference = detail.reachCalibrationReference,
                reachCalibrationLengthInput = detail.reachCalibrationReference
                    ?.referenceLengthCm
                    ?.toString()
                    ?: DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = targetScreen == AppScreen.REACH_CALIBRATION &&
                    detail.reachCalibrationReference == null,
                reachCalibrationReturnToHoldEditor = false,
                reachCalibrationReturnToAutoExtraction = false,
                selectedHoldIndex = null,
                holdEditorTool = if (targetScreen == AppScreen.HOLD_EDITOR) {
                    HoldEditorTool.ADD
                } else {
                    currentState.holdEditorTool
                },
                savedChallenges = emptyList(),
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                lastGeneratedIntermediateHoldIndices = emptySet(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                isHoldEditorDirty = false,
                holdScoringPosition = 0,
                challengeFlowBackStack = emptyList(),
                showDiscardDialog = false,
                isBusy = false,
                message = when (targetScreen) {
                    AppScreen.REACH_CALIBRATION -> if (detail.reachCalibrationReference == null) {
                        text(R.string.message_reach_first_point)
                    } else {
                        text(R.string.message_reach_confirm)
                    }
                    else -> null
                }
            )
        }
    }

    private fun updateSelectedHoldAttribute(
        transform: (Hold) -> Hold
    ) {
        val state = _uiState.value
        val selectedIndex = state.selectedHoldIndex ?: return
        updateHoldAttribute(selectedIndex, transform)
    }

    private fun updateHoldAttribute(
        index: Int?,
        transform: (Hold) -> Hold
    ) {
        val state = _uiState.value
        val selectedIndex = index ?: return
        val selectedHold = state.holds.getOrNull(selectedIndex) ?: return
        val updatedHolds = state.holds.toMutableList().apply {
            this[selectedIndex] = transform(selectedHold)
        }

        _uiState.value = state.copy(
            selectedHoldIndex = selectedIndex,
            holds = updatedHolds,
            isHoldEditorDirty = true,
            message = null
        )
    }

    private fun cropBitmap(
        bitmap: Bitmap,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ): Bitmap? {
        val normalizedLeft = leftFraction.coerceIn(0f, 1f)
        val normalizedTop = topFraction.coerceIn(0f, 1f)
        val normalizedRight = rightFraction.coerceIn(0f, 1f)
        val normalizedBottom = bottomFraction.coerceIn(0f, 1f)
        if (normalizedRight <= normalizedLeft || normalizedBottom <= normalizedTop) {
            return null
        }

        val cropLeft = (bitmap.width * normalizedLeft).roundToInt().coerceIn(0, bitmap.width - 1)
        val cropTop = (bitmap.height * normalizedTop).roundToInt().coerceIn(0, bitmap.height - 1)
        val cropRight = (bitmap.width * normalizedRight).roundToInt().coerceIn(cropLeft + 1, bitmap.width)
        val cropBottom = (bitmap.height * normalizedBottom).roundToInt().coerceIn(cropTop + 1, bitmap.height)
        val cropWidth = cropRight - cropLeft
        val cropHeight = cropBottom - cropTop
        if (cropWidth <= 0 || cropHeight <= 0) {
            return null
        }

        return Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
    }

    private fun selectChallengeGenerationMethod(method: ChallengeGenerationMethod) {
        _uiState.value = buildSelectedChallengeGenerationMethodState(
            state = _uiState.value,
            method = method,
            pushedChallengeFlowBackStack = ::pushedChallengeFlowBackStack
        )
    }

    private fun runAutoHoldExtraction(
        bitmap: Bitmap,
        tuning: AutoExtractionTuning,
        wallSamplePoints: List<HoldPoint>
    ) {
        autoExtractionRequestId += 1
        val requestId = autoExtractionRequestId
        _uiState.value = _uiState.value.copy(
            isBusy = true,
            message = null
        )

        viewModelScope.launch {
            val extractedHolds = withContext(Dispatchers.Default) {
                extractAutoHolds(
                    bitmap = bitmap,
                    tuning = tuning,
                    wallSamplePoints = wallSamplePoints
                )
            }
            val currentState = _uiState.value
            if (requestId != autoExtractionRequestId) {
                return@launch
            }
            if (currentState.currentScreen != AppScreen.AUTO_HOLD_EXTRACTION) {
                _uiState.value = currentState.copy(isBusy = false)
                return@launch
            }

            _uiState.value = currentState.copy(
                autoExtractedHolds = extractedHolds,
                selectedHoldIndex = extractedHolds.indices.firstOrNull(),
                isBusy = false,
                message = if (extractedHolds.isEmpty()) {
                    text(R.string.message_auto_hold_extraction_empty)
                } else {
                    text(R.string.message_auto_hold_extraction_completed, extractedHolds.size)
                }
            )
        }
    }
}
