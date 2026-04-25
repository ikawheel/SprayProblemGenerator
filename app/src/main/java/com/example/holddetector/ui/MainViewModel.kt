package com.example.holddetector.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.example.holddetector.domain.challenge.ChallengeRouteGenerator
import com.example.holddetector.domain.challenge.normalizeChallengeRouteOrder
import com.example.holddetector.domain.hold.AutoExtractionTuning
import com.example.holddetector.domain.challenge.RouteGenerationTuning
import com.example.holddetector.domain.hold.BinaryHoldExtractor
import com.example.holddetector.domain.hold.buildHoldScoringOrder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.holddetector.R
import com.example.holddetector.data.WallStorageRepository
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.DEFAULT_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.DEFAULT_REACH_REFERENCE_LENGTH_CM
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.model.MAX_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.MIN_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.ReachCalibrationReference
import com.example.holddetector.model.SavedWallSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallStorageRepository(application.applicationContext)
    private val appContext = application.applicationContext
    private var autoExtractionRequestId = 0L

    private val _uiState = MutableStateFlow(MainUiState())
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

    fun applyCapturedImageCrop(
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
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

            _uiState.value = buildReachCalibrationState(
                state = currentState,
                bitmap = croppedBitmap,
                capturedOrientation = orientationForBitmap(croppedBitmap),
                capturedRotationDegrees = 0
            )
        }
    }

    fun openManualHoldRegistrationAfterCapture() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.REACH_CALIBRATION,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.REACH_CALIBRATION
            ),
            holds = emptyList(),
            autoExtractedHolds = emptyList(),
            selectedHoldIndex = null,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = state.reachCalibrationReference == null,
            reachCalibrationReturnToHoldEditor = false,
            reachCalibrationReturnToAutoExtraction = false,
            message = if (state.reachCalibrationReference == null) {
                text(R.string.message_reach_first_point)
            } else {
                text(R.string.message_reach_confirm)
            }
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
        val state = _uiState.value
        _uiState.value = popScreenState(
            state = state.copy(
            holds = emptyList(),
            autoExtractedHolds = emptyList(),
            selectedHoldIndex = null,
            isAutoExtractionWallSamplingMode = false,
            isBusy = false,
            message = text(R.string.message_hold_registration_method_select)
            )
        )
    }

    fun onAutoExtractedHoldTapped(index: Int?) {
        val state = _uiState.value
        _uiState.value = state.copy(selectedHoldIndex = index)
    }

    fun onAutoExtractionTuningChanged(tuning: AutoExtractionTuning) {
        val state = _uiState.value
        val bitmap = state.capturedBitmap
        _uiState.value = state.copy(autoExtractionTuning = tuning)
        if (state.currentScreen == AppScreen.AUTO_HOLD_EXTRACTION && bitmap != null) {
            runAutoHoldExtraction(
                bitmap = bitmap,
                tuning = tuning,
                wallSamplePoints = state.autoExtractionWallSamplePoints
            )
        }
    }

    fun startAutoExtractionWallSampling() {
        val state = _uiState.value
        _uiState.value = state.copy(
            isAutoExtractionWallSamplingMode = true,
            message = text(R.string.message_auto_hold_extraction_wall_sample_mode)
        )
    }

    fun stopAutoExtractionWallSampling() {
        val state = _uiState.value
        _uiState.value = state.copy(
            isAutoExtractionWallSamplingMode = false,
            message = null
        )
    }

    fun onAutoExtractionWallSamplePointSelected(point: HoldPoint) {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: return
        if (state.currentScreen != AppScreen.AUTO_HOLD_EXTRACTION) return

        val updatedPoints = (state.autoExtractionWallSamplePoints + point)
            .distinct()
            .take(10)
        val keepSampling = updatedPoints.size < 10

        _uiState.value = state.copy(
            autoExtractionWallSamplePoints = updatedPoints,
            isAutoExtractionWallSamplingMode = keepSampling,
            message = text(
                R.string.message_auto_hold_extraction_wall_sample_added,
                updatedPoints.size
            )
        )

        runAutoHoldExtraction(
            bitmap = bitmap,
            tuning = state.autoExtractionTuning,
            wallSamplePoints = updatedPoints
        )
    }

    fun clearAutoExtractionWallSamplePoints() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap
        _uiState.value = state.copy(
            autoExtractionWallSamplePoints = emptyList(),
            isAutoExtractionWallSamplingMode = false,
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
        if (state.autoExtractedHolds.isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_auto_hold_extraction_empty))
            return
        }

        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.HOLD_EDITOR
            ),
            holds = state.autoExtractedHolds,
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            reachCalibrationReturnToHoldEditor = false,
            reachCalibrationReturnToAutoExtraction = false,
            isHoldEditorDirty = true,
            message = text(R.string.message_auto_hold_extraction_applied)
        )
    }

    fun openReachCalibrationScreen() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.REACH_CALIBRATION,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.REACH_CALIBRATION
            ),
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
                text(R.string.message_reach_first_point)
            } else {
                text(R.string.message_reach_confirm)
            }
        )
    }

    fun continueToHoldEditorFromReachCalibration() {
        val state = _uiState.value
        if (state.capturedBitmap == null) return
        val normalizedReference = requireConfiguredReachReference(state) ?: return

        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.HOLD_EDITOR
            ),
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            reachCalibrationReturnToHoldEditor = false,
            reachCalibrationReturnToAutoExtraction = false,
            autoExtractedHolds = emptyList(),
            reachCalibrationReference = normalizedReference,
            message = text(R.string.message_reach_go_hold_editor)
        )
    }

    fun continueToAutoHoldExtractionFromReachCalibration() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: return
        val normalizedReference = requireConfiguredReachReference(state) ?: return

        _uiState.value = state.copy(
            currentScreen = AppScreen.AUTO_HOLD_EXTRACTION,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.AUTO_HOLD_EXTRACTION
            ),
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
        runAutoHoldExtraction(
            bitmap = bitmap,
            tuning = state.autoExtractionTuning,
            wallSamplePoints = state.autoExtractionWallSamplePoints
        )
    }

    fun backFromReachCalibration() {
        val state = _uiState.value
        _uiState.value = popScreenState(
            state = state.copy(
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

    fun openSavedWallForChallenge(wallId: String) {
        openSavedWallForChallenge(wallId, null)
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
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                lastGeneratedIntermediateHoldIndices = emptySet(),
                challengeGenerationMethod = initialMethod,
                challengeFlowStep = if (initialMethod == null) {
                    ChallengeFlowStep.METHOD_SELECT
                } else {
                    ChallengeFlowStep.COMMON_SETTINGS
                },
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

    fun onHoldTapAreaSizeChanged(size: HoldTapAreaSize) {
        _uiState.value = _uiState.value.copy(
            holdTapAreaSize = size
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
        val normalizedInput = value.filter(Char::isDigit).take(3)
        val parsedLength = normalizedInput.toIntOrNull()?.takeIf { it > 0 }
        val updatedReference = if (parsedLength != null) {
            state.reachCalibrationReference?.copy(referenceLengthCm = parsedLength)
        } else {
            state.reachCalibrationReference
        }

        _uiState.value = state.copy(
            reachCalibrationLengthInput = normalizedInput,
            reachCalibrationReference = updatedReference,
            isHoldEditorDirty = state.isHoldEditorDirty ||
                (normalizedInput != state.reachCalibrationLengthInput) ||
                (updatedReference != state.reachCalibrationReference)
        )
    }

    fun clearReachCalibration() {
        val state = _uiState.value
        _uiState.value = state.copy(
            reachCalibrationReference = null,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            selectedHoldIndex = null,
            isHoldEditorDirty = true,
            message = text(R.string.message_reach_cleared)
        )
    }

    fun onReachCalibrationPointSelected(point: HoldPoint) {
        val state = _uiState.value
        if (!state.isReachCalibrationSelectionMode) return

        val firstPoint = state.pendingReachCalibrationPoint
        if (firstPoint == null) {
            _uiState.value = state.copy(
                pendingReachCalibrationPoint = point,
                selectedHoldIndex = null,
                message = text(R.string.message_reach_second_point)
            )
            return
        }

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

        _uiState.value = state.copy(
            reachCalibrationReference = ReachCalibrationReference(
                firstPoint = firstPoint,
                secondPoint = point,
                referenceLengthCm = referenceLengthCm
            ),
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            selectedHoldIndex = null,
            isHoldEditorDirty = true,
            message = text(R.string.message_reach_set)
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
        val state = _uiState.value
        if (state.isDrawTargetSelectionMode) return
        val selectionCandidateIndices = challengeSelectionCandidateIndices(state)

        when (state.routeSelectionMode) {
            RouteSelectionMode.SELECTING_START -> {
                if (index == null || index !in selectionCandidateIndices) {
                    _uiState.value = state.copy(message = text(R.string.message_select_start_from_candidates))
                    return
                }
                _uiState.value = state.copy(
                    selectedHoldIndex = index,
                    startHoldIndex = index,
                    goalHoldIndex = null,
                    challengeHoldIndices = emptySet(),
                    challengeOrderedHoldIndices = emptyList(),
                    lastGeneratedIntermediateHoldIndices = emptySet(),
                    routeSelectionMode = RouteSelectionMode.SELECTING_GOAL,
                    message = text(R.string.message_start_set_next_goal)
                )
            }

            RouteSelectionMode.SELECTING_GOAL -> {
                if (index == null || index !in selectionCandidateIndices) {
                    _uiState.value = state.copy(message = text(R.string.message_select_goal_from_candidates))
                    return
                }
                if (index == state.startHoldIndex) {
                    _uiState.value = state.copy(message = text(R.string.message_start_goal_must_differ))
                    return
                }
                _uiState.value = state.copy(
                    selectedHoldIndex = index,
                    goalHoldIndex = index,
                    challengeHoldIndices = emptySet(),
                    challengeOrderedHoldIndices = emptyList(),
                    lastGeneratedIntermediateHoldIndices = emptySet(),
                    routeSelectionMode = RouteSelectionMode.NONE,
                    message = text(R.string.message_goal_set)
                )
            }

            RouteSelectionMode.NONE -> {
                if (index == null) return
                if (state.challengeHoldIndices.isEmpty()) return
                if (index !in selectionCandidateIndices) return
                val updated = state.challengeHoldIndices.toMutableSet()
                val added = if (updated.contains(index)) {
                    updated.remove(index)
                    false
                } else {
                    updated.add(index)
                    true
                }
                val updatedOrder = normalizeChallengeRouteOrder(
                    challengeIndices = updated,
                    preferredOrder = state.challengeOrderedHoldIndices,
                    holds = state.holds,
                    startIndex = state.startHoldIndex,
                    goalIndex = state.goalHoldIndex
                )
                _uiState.value = state.copy(
                    selectedHoldIndex = index,
                    challengeHoldIndices = updated,
                    challengeOrderedHoldIndices = updatedOrder,
                    startHoldIndex = if (state.startHoldIndex == index && !added) null else state.startHoldIndex,
                    goalHoldIndex = if (state.goalHoldIndex == index && !added) null else state.goalHoldIndex,
                    message = if (added) {
                        text(R.string.message_challenge_hold_added)
                    } else {
                        text(R.string.message_challenge_hold_removed)
                    }
                )
            }
        }
    }

    fun onDrawCountChanged(value: String) {
        _uiState.value = _uiState.value.copy(drawCountInput = value.filter { it.isDigit() })
    }

    fun onChallengeDifficultyRangeChanged(start: Float, endInclusive: Float) {
        val state = _uiState.value
        val minScore = start.roundToInt().coerceIn(
            MIN_HOLD_DIFFICULTY_SCORE,
            MAX_HOLD_DIFFICULTY_SCORE
        )
        val maxScore = endInclusive.roundToInt().coerceIn(
            MIN_HOLD_DIFFICULTY_SCORE,
            MAX_HOLD_DIFFICULTY_SCORE
        )
        val normalizedMin = minOf(minScore, maxScore)
        val normalizedMax = maxOf(minScore, maxScore)
        val filteredChallengeIndices = filterChallengeEligibleIndices(
            holds = state.holds,
            indices = state.challengeHoldIndices,
            minScore = normalizedMin,
            maxScore = normalizedMax
        )
        val filteredStartIndex = state.startHoldIndex?.takeIf { index ->
            index in filterChallengeEligibleIndices(
                holds = state.holds,
                indices = setOf(index),
                minScore = normalizedMin,
                maxScore = normalizedMax
            )
        }
        val filteredGoalIndex = state.goalHoldIndex?.takeIf { index ->
            index in filterChallengeEligibleIndices(
                holds = state.holds,
                indices = setOf(index),
                minScore = normalizedMin,
                maxScore = normalizedMax
            )
        }
        val normalizedRouteSelectionMode = when (state.routeSelectionMode) {
            RouteSelectionMode.SELECTING_START -> RouteSelectionMode.SELECTING_START
            RouteSelectionMode.SELECTING_GOAL -> if (filteredStartIndex != null) {
                RouteSelectionMode.SELECTING_GOAL
            } else {
                RouteSelectionMode.SELECTING_START
            }
            RouteSelectionMode.NONE -> RouteSelectionMode.NONE
        }

        _uiState.value = state.copy(
            challengeDifficultyScoreMin = normalizedMin,
            challengeDifficultyScoreMax = normalizedMax,
            challengeHoldIndices = filteredChallengeIndices,
            challengeOrderedHoldIndices = normalizeChallengeRouteOrder(
                challengeIndices = filteredChallengeIndices,
                preferredOrder = state.challengeOrderedHoldIndices,
                holds = state.holds,
                startIndex = filteredStartIndex,
                goalIndex = filteredGoalIndex
            ),
            startHoldIndex = filteredStartIndex,
            goalHoldIndex = filteredGoalIndex,
            selectedHoldIndex = state.selectedHoldIndex?.takeIf { index ->
                index in filterChallengeEligibleIndices(
                    holds = state.holds,
                    indices = setOf(index),
                    minScore = normalizedMin,
                    maxScore = normalizedMax
                )
            },
            routeSelectionMode = normalizedRouteSelectionMode,
            lastGeneratedIntermediateHoldIndices = state.lastGeneratedIntermediateHoldIndices.filterTo(linkedSetOf()) { index ->
                index in filterChallengeEligibleIndices(
                    holds = state.holds,
                    indices = setOf(index),
                    minScore = normalizedMin,
                    maxScore = normalizedMax
                )
            }
        )
    }

    fun onDetourStrengthChanged(value: Float) {
        updateRouteTuning { copy(detourStrength = value.coerceIn(0f, 1f)) }
    }

    fun onRouteWavinessChanged(value: Float) {
        updateRouteTuning { copy(routeWaviness = value.coerceIn(0f, 1f)) }
    }

    fun onStepDistanceVarianceChanged(value: Float) {
        updateRouteTuning { copy(stepDistanceVariance = value.coerceIn(0f, 1f)) }
    }

    fun onCorridorWidthChanged(value: Float) {
        updateRouteTuning { copy(corridorWidth = value.coerceIn(0f, 1f)) }
    }

    fun onExcludePreviouslyGeneratedHoldsChanged(value: Boolean) {
        updateRouteTuning { copy(excludePreviouslyGeneratedHolds = value) }
    }

    fun selectManualStartGoalChallengeMethod() {
        selectChallengeGenerationMethod(ChallengeGenerationMethod.MANUAL_START_GOAL)
    }

    fun selectRandomStartGoalChallengeMethod() {
        selectChallengeGenerationMethod(ChallengeGenerationMethod.RANDOM_START_GOAL)
    }

    fun openChallengeMethodSelection() {
        val state = _uiState.value
        _uiState.value = state.copy(
            challengeFlowStep = ChallengeFlowStep.METHOD_SELECT,
            challengeFlowBackStack = pushedChallengeFlowBackStack(
                state = state,
                targetStep = ChallengeFlowStep.METHOD_SELECT
            ),
            selectedHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
        )
    }

    fun openChallengeCommonSettings() {
        val state = _uiState.value
        _uiState.value = state.copy(
            challengeFlowStep = ChallengeFlowStep.COMMON_SETTINGS,
            challengeFlowBackStack = pushedChallengeFlowBackStack(
                state = state,
                targetStep = ChallengeFlowStep.COMMON_SETTINGS
            ),
            selectedHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
        )
    }

    fun openChallengeGeneration() {
        val state = _uiState.value
        if (state.challengeGenerationMethod == null) {
            _uiState.value = state.copy(message = text(R.string.message_select_challenge_generation_method))
            return
        }
        _uiState.value = state.copy(
            challengeFlowStep = ChallengeFlowStep.GENERATION,
            challengeFlowBackStack = pushedChallengeFlowBackStack(
                state = state,
                targetStep = ChallengeFlowStep.GENERATION
            ),
            selectedHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
        )
    }

    fun openChallengeTuning() {
        val state = _uiState.value
        _uiState.value = state.copy(
            challengeFlowStep = ChallengeFlowStep.TUNING,
            challengeFlowBackStack = pushedChallengeFlowBackStack(
                state = state,
                targetStep = ChallengeFlowStep.TUNING
            ),
            selectedHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
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

        viewModelScope.launch {
            _uiState.value = state.copy(isBusy = true)

            val selectedOrderedIndices = withContext(Dispatchers.Default) {
                generateChallengeRouteWithRetries(
                    holds = state.holds,
                    sourceIndices = drawSourceIndices,
                    startIndex = startIndex,
                    goalIndex = goalIndex,
                    targetCount = requestedCount,
                    tuning = state.routeTuning,
                    reachCalibrationReference = state.reachCalibrationReference
                )
            }

            if (selectedOrderedIndices == null) {
                _uiState.value = state.copy(
                    isBusy = false,
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
                challengeHoldIndices = selectedIndices,
                challengeOrderedHoldIndices = selectedOrderedIndices,
                lastGeneratedIntermediateHoldIndices = selectedIndices
                    .filterNotTo(linkedSetOf()) { index -> index == startIndex || index == goalIndex },
                challengeFlowStep = ChallengeFlowStep.RESULT,
                challengeFlowBackStack = pushedChallengeFlowBackStack(
                    state = state,
                    targetStep = ChallengeFlowStep.RESULT
                ),
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

        viewModelScope.launch {
            _uiState.value = state.copy(isBusy = true)

            val generatedRoute = withContext(Dispatchers.Default) {
                generateChallengeRouteWithRandomStartGoal(
                    holds = state.holds,
                    selectionCandidateIndices = selectionCandidateIndices,
                    lastGeneratedIntermediateHoldIndices = state.lastGeneratedIntermediateHoldIndices,
                    targetCount = requestedCount,
                    tuning = state.routeTuning,
                    reachCalibrationReference = state.reachCalibrationReference
                )
            }

            if (generatedRoute == null) {
                _uiState.value = state.copy(
                    isBusy = false,
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
                selectedHoldIndex = null,
                startHoldIndex = generatedRoute.startIndex,
                goalHoldIndex = generatedRoute.goalIndex,
                challengeHoldIndices = selectedIndices,
                challengeOrderedHoldIndices = generatedRoute.orderedIndices,
                lastGeneratedIntermediateHoldIndices = selectedIndices
                    .filterNotTo(linkedSetOf()) { index ->
                        index == generatedRoute.startIndex || index == generatedRoute.goalIndex
                    },
                challengeFlowStep = ChallengeFlowStep.RESULT,
                challengeFlowBackStack = pushedChallengeFlowBackStack(
                    state = state,
                    targetStep = ChallengeFlowStep.RESULT
                ),
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                message = text(R.string.message_draw_generated_random_start_goal)
            )
        }
    }

    fun startDrawTargetSelection() {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            lastGeneratedIntermediateHoldIndices = emptySet(),
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = true,
            message = text(R.string.message_draw_target_instruction)
        )
    }

    fun applyDrawTargetSelection(indices: Set<Int>) {
        val state = _uiState.value
        val eligibleIndices = filterChallengeEligibleIndices(
            holds = state.holds,
            indices = indices,
            minScore = state.challengeDifficultyScoreMin,
            maxScore = state.challengeDifficultyScoreMax
        )
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            lastGeneratedIntermediateHoldIndices = emptySet(),
            drawTargetHoldIndices = indices,
            hasDrawTargetSelection = true,
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = if (eligibleIndices.isEmpty()) {
                text(R.string.message_draw_target_empty_after_filter)
            } else {
                text(R.string.message_draw_target_selected_count, eligibleIndices.size)
            }
        )
    }

    fun startChallengeStartGoalSelection() {
        val state = _uiState.value
        if (state.isDrawTargetSelectionMode) {
            _uiState.value = state.copy(message = text(R.string.message_finish_range_selection_first))
            return
        }
        if (challengeSelectionCandidateIndices(state).isEmpty()) {
            _uiState.value = state.copy(message = text(R.string.message_select_candidate_holds_first))
            return
        }
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            lastGeneratedIntermediateHoldIndices = emptySet(),
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.SELECTING_START,
            message = text(R.string.message_select_start_prompt)
        )
    }

    fun clearChallengeSelection() {
        val state = _uiState.value
        _uiState.value = state.copy(
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
            _uiState.value = state.copy(showDiscardDialog = true)
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

    fun dismissDiscardDialog() {
        _uiState.value = _uiState.value.copy(showDiscardDialog = false)
    }

    fun discardEditorAndReturnToList() {
        _uiState.value = popScreenState(
            state = _uiState.value.copy(showDiscardDialog = false)
        )
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
            drawCountInput = source.drawCountInput,
            holdTapAreaSize = source.holdTapAreaSize,
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

    private fun buildReachCalibrationState(
        state: MainUiState,
        bitmap: Bitmap,
        capturedOrientation: CapturedOrientation,
        capturedRotationDegrees: Int
    ): MainUiState {
        return state.copy(
            currentScreen = AppScreen.REACH_CALIBRATION,
            screenBackStack = pushedScreenBackStack(
                state = state,
                targetScreen = AppScreen.REACH_CALIBRATION
            ),
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
            isReachCalibrationSelectionMode = true,
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
            message = text(R.string.message_reach_first_point)
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

    private fun parseReachCalibrationLengthCentimeters(state: MainUiState): Int? {
        return state.reachCalibrationLengthInput.toIntOrNull()?.takeIf { it > 0 }
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

    private fun ReachCalibrationReference?.withCurrentLength(state: MainUiState): ReachCalibrationReference? {
        val parsedLength = parseReachCalibrationLengthCentimeters(state) ?: return this
        return this?.copy(referenceLengthCm = parsedLength)
    }

    private fun requireConfiguredReachReference(state: MainUiState): ReachCalibrationReference? {
        if (parseReachCalibrationLengthCentimeters(state) == null) {
            _uiState.value = state.copy(message = text(R.string.message_reach_input_length))
            return null
        }
        val normalizedReference = state.reachCalibrationReference.withCurrentLength(state)
        if (normalizedReference == null) {
            _uiState.value = state.copy(message = text(R.string.message_reach_set_required))
            return null
        }
        return normalizedReference
    }

    private fun orientationForBitmap(bitmap: Bitmap): CapturedOrientation {
        return if (bitmap.width > bitmap.height) {
            CapturedOrientation.LANDSCAPE
        } else {
            CapturedOrientation.PORTRAIT
        }
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

    private fun challengeSelectionCandidateIndices(state: MainUiState): Set<Int> {
        val baseIndices = if (state.hasDrawTargetSelection) {
            state.drawTargetHoldIndices
        } else {
            state.holds.indices.toSet()
        }
        return filterChallengeEligibleIndices(
            holds = state.holds,
            indices = baseIndices,
            minScore = state.challengeDifficultyScoreMin,
            maxScore = state.challengeDifficultyScoreMax
        )
    }

    private fun filterChallengeEligibleIndices(
        holds: List<Hold>,
        indices: Set<Int>,
        minScore: Int,
        maxScore: Int
    ): Set<Int> {
        return indices.filterTo(linkedSetOf()) { index ->
            val score = holds.getOrNull(index)?.difficultyScore ?: DEFAULT_HOLD_DIFFICULTY_SCORE
            score in minScore..maxScore
        }
    }

    private fun selectChallengeGenerationMethod(method: ChallengeGenerationMethod) {
        val state = _uiState.value
        _uiState.value = state.copy(
            challengeGenerationMethod = method,
            challengeFlowStep = ChallengeFlowStep.COMMON_SETTINGS,
            challengeFlowBackStack = pushedChallengeFlowBackStack(
                state = state,
                targetStep = ChallengeFlowStep.COMMON_SETTINGS
            ),
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            lastGeneratedIntermediateHoldIndices = emptySet(),
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
        )
    }

    private fun generateChallengeRouteWithRetries(
        holds: List<Hold>,
        sourceIndices: Set<Int>,
        startIndex: Int,
        goalIndex: Int,
        targetCount: Int?,
        tuning: RouteGenerationTuning,
        reachCalibrationReference: ReachCalibrationReference?
    ): List<Int>? {
        val maximumAttempts = 512

        repeat(maximumAttempts) {
            ChallengeRouteGenerator.generate(
                holds = holds,
                sourceIndices = sourceIndices,
                startIndex = startIndex,
                goalIndex = goalIndex,
                targetCount = targetCount,
                tuning = tuning,
                reachCalibrationReference = reachCalibrationReference
            )?.let { generatedRoute ->
                return generatedRoute
            }
        }

        return null
    }

    private fun generateChallengeRouteWithRandomStartGoal(
        holds: List<Hold>,
        selectionCandidateIndices: Set<Int>,
        lastGeneratedIntermediateHoldIndices: Set<Int>,
        targetCount: Int?,
        tuning: RouteGenerationTuning,
        reachCalibrationReference: ReachCalibrationReference?
    ): RandomStartGoalGenerationResult? {
        val filteredSelectionCandidateIndices = if (tuning.excludePreviouslyGeneratedHolds) {
            selectionCandidateIndices
                .filterNotTo(linkedSetOf()) { it in lastGeneratedIntermediateHoldIndices }
                .takeIf { it.size >= 2 }
                ?: selectionCandidateIndices
        } else {
            selectionCandidateIndices
        }

        val preferredStartIndices = filteredSelectionCandidateIndices.filterTo(linkedSetOf()) { index ->
            holds.getOrNull(index)?.isStartCandidate == true
        }
        val preferredGoalIndices = filteredSelectionCandidateIndices.filterTo(linkedSetOf()) { index ->
            holds.getOrNull(index)?.isGoalCandidate == true
        }
        val preferredPairs = buildDistinctStartGoalPairs(
            startIndices = if (preferredStartIndices.isNotEmpty()) preferredStartIndices else filteredSelectionCandidateIndices,
            goalIndices = if (preferredGoalIndices.isNotEmpty()) preferredGoalIndices else filteredSelectionCandidateIndices
        )
        val candidatePairs = if (preferredPairs.isNotEmpty()) {
            preferredPairs
        } else {
            buildDistinctStartGoalPairs(
                startIndices = filteredSelectionCandidateIndices,
                goalIndices = filteredSelectionCandidateIndices
            )
        }
        if (candidatePairs.isEmpty()) return null

        candidatePairs
            .shuffled(Random.Default)
            .take(64)
            .forEach { (startIndex, goalIndex) ->
                val drawSourceIndices = selectionCandidateIndices.toMutableSet().apply {
                    if (tuning.excludePreviouslyGeneratedHolds) {
                        removeAll(lastGeneratedIntermediateHoldIndices)
                    }
                    add(startIndex)
                    add(goalIndex)
                }
                val orderedIndices = generateChallengeRouteWithRetries(
                    holds = holds,
                    sourceIndices = drawSourceIndices,
                    startIndex = startIndex,
                    goalIndex = goalIndex,
                    targetCount = targetCount,
                    tuning = tuning,
                    reachCalibrationReference = reachCalibrationReference
                )
                if (orderedIndices != null) {
                    return RandomStartGoalGenerationResult(
                        startIndex = startIndex,
                        goalIndex = goalIndex,
                        orderedIndices = orderedIndices
                    )
                }
            }

        return null
    }

    private fun buildDistinctStartGoalPairs(
        startIndices: Set<Int>,
        goalIndices: Set<Int>
    ): List<Pair<Int, Int>> {
        return buildList {
            startIndices.forEach { startIndex ->
                goalIndices.forEach { goalIndex ->
                    if (startIndex != goalIndex) {
                        add(startIndex to goalIndex)
                    }
                }
            }
        }
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
                BinaryHoldExtractor.extract(
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

private data class RandomStartGoalGenerationResult(
    val startIndex: Int,
    val goalIndex: Int,
    val orderedIndices: List<Int>
)
