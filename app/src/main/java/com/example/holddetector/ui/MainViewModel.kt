package com.example.holddetector.ui

import android.app.Application
import android.graphics.Bitmap
import com.example.holddetector.domain.challenge.ChallengeRouteGenerator
import com.example.holddetector.domain.challenge.normalizeChallengeRouteOrder
import com.example.holddetector.domain.challenge.RouteGenerationTuning
import com.example.holddetector.domain.hold.buildHoldScoringOrder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.holddetector.data.WallStorageRepository
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.DEFAULT_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.model.MAX_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.MIN_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.ReachCalibrationReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WallStorageRepository(application.applicationContext)

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
            _uiState.value = MainUiState(
                currentScreen = AppScreen.LIST,
                savedWalls = summaries,
                drawCountInput = _uiState.value.drawCountInput,
                holdTapAreaSize = _uiState.value.holdTapAreaSize,
                challengeDifficultyScoreMin = _uiState.value.challengeDifficultyScoreMin,
                challengeDifficultyScoreMax = _uiState.value.challengeDifficultyScoreMax,
                routeTuning = _uiState.value.routeTuning,
                isBusy = false
            )
        }
    }

    fun startNewWall() {
        _uiState.value = MainUiState(
            currentScreen = AppScreen.CAMERA,
            savedWalls = _uiState.value.savedWalls,
            drawCountInput = _uiState.value.drawCountInput,
            holdTapAreaSize = _uiState.value.holdTapAreaSize,
            challengeDifficultyScoreMin = _uiState.value.challengeDifficultyScoreMin,
            challengeDifficultyScoreMax = _uiState.value.challengeDifficultyScoreMax,
            routeTuning = _uiState.value.routeTuning
        )
    }

    fun onPhotoCaptured(
        bitmap: Bitmap,
        capturedOrientation: CapturedOrientation,
        capturedRotationDegrees: Int
    ) {
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.REACH_CALIBRATION,
            currentWallId = null,
            wallTitle = defaultWallTitle(),
            capturedBitmap = bitmap,
            capturedOrientation = capturedOrientation,
            capturedRotationDegrees = capturedRotationDegrees,
            holds = emptyList(),
            reachCalibrationReference = null,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = true,
            reachCalibrationReturnToHoldEditor = false,
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            drawTargetHoldIndices = emptySet(),
            hasDrawTargetSelection = false,
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            isHoldEditorDirty = true,
            showDiscardDialog = false,
            message = "150cm基準の1点目をタップしてください"
        )
    }

    fun openReachCalibrationScreen() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.REACH_CALIBRATION,
            selectedHoldIndex = null,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = state.reachCalibrationReference == null,
            reachCalibrationReturnToHoldEditor = true,
            message = if (state.reachCalibrationReference == null) {
                "150cm基準の1点目をタップしてください"
            } else {
                "150cm基準を確認してください"
            }
        )
    }

    fun continueToHoldEditorFromReachCalibration() {
        val state = _uiState.value
        if (state.capturedBitmap == null) return
        if (state.reachCalibrationReference == null) {
            _uiState.value = state.copy(message = "150cm基準を設定してください")
            return
        }

        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            reachCalibrationReturnToHoldEditor = false,
            message = "ホールド登録へ進みます"
        )
    }

    fun backFromReachCalibration() {
        val state = _uiState.value
        if (state.reachCalibrationReturnToHoldEditor) {
            _uiState.value = state.copy(
                currentScreen = AppScreen.HOLD_EDITOR,
                selectedHoldIndex = null,
                holdScoringPosition = 0,
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                message = null
            )
        } else {
            val fallbackScreen = if (state.currentWallId != null) {
                AppScreen.LIST
            } else {
                AppScreen.CAMERA
            }
            _uiState.value = MainUiState(
                currentScreen = fallbackScreen,
                savedWalls = state.savedWalls,
                drawCountInput = state.drawCountInput,
                holdTapAreaSize = state.holdTapAreaSize,
                challengeDifficultyScoreMin = state.challengeDifficultyScoreMin,
                challengeDifficultyScoreMax = state.challengeDifficultyScoreMax,
                routeTuning = state.routeTuning
            )
        }
    }

    fun openSavedWall(wallId: String) {
        openSavedWallForEditing(wallId)
    }

    fun openSavedWallForEditing(wallId: String) {
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
                    routeTuning = _uiState.value.routeTuning,
                    message = "螢√ョ繝ｼ繧ｿ繧帝幕縺代∪縺帙ｓ縺ｧ縺励◆"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.EDIT_MENU,
                currentWallId = detail.id,
                wallTitle = detail.title,
                capturedBitmap = detail.bitmap,
                capturedOrientation = detail.capturedOrientation,
                capturedRotationDegrees = detail.capturedRotationDegrees,
                holds = detail.holds,
                reachCalibrationReference = detail.reachCalibrationReference,
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                selectedHoldIndex = null,
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                isHoldEditorDirty = false,
                holdScoringPosition = 0,
                showDiscardDialog = false,
                isBusy = false,
                message = null
            )
        }
    }

    fun openReachCalibrationFromEditMenu() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.REACH_CALIBRATION,
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = state.reachCalibrationReference == null,
            reachCalibrationReturnToHoldEditor = false,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = if (state.reachCalibrationReference == null) {
                "150cm基準の1点目をタップしてください"
            } else {
                "150cm基準を確認してください"
            }
        )
    }

    fun openHoldEditorFromEditMenu() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            reachCalibrationReturnToHoldEditor = false,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
        )
    }

    fun openHoldScoringFromEditMenu() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_SCORING,
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            reachCalibrationReturnToHoldEditor = false,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = null
        )
    }

    fun openSavedWallForChallenge(wallId: String) {
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
                    routeTuning = _uiState.value.routeTuning,
                    message = "螢√ョ繝ｼ繧ｿ繧帝幕縺代∪縺帙ｓ縺ｧ縺励◆"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.CHALLENGE_CREATOR,
                currentWallId = detail.id,
                wallTitle = detail.title,
                capturedBitmap = detail.bitmap,
                capturedOrientation = detail.capturedOrientation,
                capturedRotationDegrees = detail.capturedRotationDegrees,
                holds = detail.holds,
                reachCalibrationReference = detail.reachCalibrationReference,
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                selectedHoldIndex = null,
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                holdScoringPosition = 0,
                isBusy = false,
                showDiscardDialog = false,
                message = "隱ｲ鬘御ｽ懈・繧帝幕蟋九＠縺ｾ縺励◆"
            )
        }
    }

    fun onWallTitleChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            wallTitle = value,
            isHoldEditorDirty = true
        )
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
            message = "ホールドを追加しました: ${updatedHolds.size} 個"
        )
    }

    fun onEditorHoldTapped(index: Int?) {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedHoldIndex = index,
            message = index?.let {
                val hold = state.holds[it]
                "驕ｸ謚樔ｸｭ #$it 荳ｭ蠢・${hold.centerX}, ${hold.centerY}) 鬆らせ謨ｰ:${hold.points.size}"
            }
        )
    }

    fun removeSelectedHold() {
        val state = _uiState.value
        val selected = state.selectedHoldIndex ?: run {
            _uiState.value = state.copy(message = "蜑企勁縺吶ｋ繝帙・繝ｫ繝峨ｒ驕ｸ謚槭＠縺ｦ縺上□縺輔＞")
            return
        }

        _uiState.value = state.copy(
            holds = state.holds.toMutableList().apply { removeAt(selected) },
            selectedHoldIndex = null,
            isHoldEditorDirty = true,
            holdScoringPosition = 0,
            message = "繝帙・繝ｫ繝峨ｒ蜑企勁縺励∪縺励◆"
        )
    }

    fun openHoldScoring() {
        val state = _uiState.value
        if (state.holds.isEmpty()) {
            _uiState.value = state.copy(message = "ホールドを登録してください")
            return
        }

        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_SCORING,
            selectedHoldIndex = null,
            holdScoringPosition = 0,
            showDiscardDialog = false,
            message = null
        )
    }

    fun returnToHoldEditorFromScoring() {
        val state = _uiState.value
        _uiState.value = state.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            selectedHoldIndex = null,
            message = null
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
            _uiState.value = state.copy(message = "菫晏ｭ倥☆繧狗判蜒上′縺ゅｊ縺ｾ縺帙ｓ")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isBusy = true)
            val savedSummary = withContext(Dispatchers.IO) {
                repository.saveWall(
                    wallId = state.currentWallId,
                    title = state.wallTitle.ifBlank { defaultWallTitle() },
                    bitmap = bitmap,
                    holds = state.holds,
                    reachCalibrationReference = state.reachCalibrationReference,
                    capturedOrientation = state.capturedOrientation,
                    capturedRotationDegrees = state.capturedRotationDegrees
                )
            }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            _uiState.value = MainUiState(
                currentScreen = AppScreen.LIST,
                savedWalls = refreshed,
                drawCountInput = state.drawCountInput,
                holdTapAreaSize = state.holdTapAreaSize,
                challengeDifficultyScoreMin = state.challengeDifficultyScoreMin,
                challengeDifficultyScoreMax = state.challengeDifficultyScoreMax,
                routeTuning = state.routeTuning,
                message = "菫晏ｭ倥＠縺ｾ縺励◆: ${savedSummary.title}"
            )
        }
    }

    fun saveWallAndOpenChallenge() {
        val state = _uiState.value
        val bitmap = state.capturedBitmap ?: run {
            _uiState.value = state.copy(message = "菫晏ｭ倥☆繧狗判蜒上′縺ゅｊ縺ｾ縺帙ｓ")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isBusy = true)
            val savedSummary = withContext(Dispatchers.IO) {
                repository.saveWall(
                    wallId = state.currentWallId,
                    title = state.wallTitle.ifBlank { defaultWallTitle() },
                    bitmap = bitmap,
                    holds = state.holds,
                    reachCalibrationReference = state.reachCalibrationReference,
                    capturedOrientation = state.capturedOrientation,
                    capturedRotationDegrees = state.capturedRotationDegrees
                )
            }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            _uiState.value = state.copy(
                currentScreen = AppScreen.CHALLENGE_CREATOR,
                savedWalls = refreshed,
                currentWallId = savedSummary.id,
                wallTitle = savedSummary.title,
                selectedHoldIndex = null,
                challengeHoldIndices = emptySet(),
                challengeOrderedHoldIndices = emptyList(),
                drawTargetHoldIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                pendingReachCalibrationPoint = null,
                isReachCalibrationSelectionMode = false,
                reachCalibrationReturnToHoldEditor = false,
                isHoldEditorDirty = false,
                holdScoringPosition = 0,
                showDiscardDialog = false,
                isBusy = false,
                message = "螢√ｒ菫晏ｭ倥＠縺ｦ隱ｲ鬘御ｽ懈・繧帝幕縺阪∪縺励◆"
            )
        }
    }

    fun startReachCalibrationSelection() {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = true,
            message = "150cm基準の1点目をタップしてください"
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
            message = "150cm基準をクリアしました"
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
                message = "150cm基準の2点目をタップしてください"
            )
            return
        }

        if (firstPoint == point) {
            _uiState.value = state.copy(
                message = "1点目と別の位置をタップしてください"
            )
            return
        }

        _uiState.value = state.copy(
            reachCalibrationReference = ReachCalibrationReference(
                firstPoint = firstPoint,
                secondPoint = point
            ),
            pendingReachCalibrationPoint = null,
            isReachCalibrationSelectionMode = false,
            selectedHoldIndex = null,
            isHoldEditorDirty = true,
            message = "150cm基準を設定しました"
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
                message = "蜑企勁縺励∪縺励◆"
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
                    _uiState.value = state.copy(message = "隱ｲ鬘悟・縺ｮ繝帙・繝ｫ繝峨°繧峨せ繧ｿ繝ｼ繝医ｒ驕ｸ謚槭＠縺ｦ縺上□縺輔＞")
                    return
                }
                _uiState.value = state.copy(
                    selectedHoldIndex = index,
                    startHoldIndex = index,
                    goalHoldIndex = null,
                    challengeHoldIndices = emptySet(),
                    challengeOrderedHoldIndices = emptyList(),
                    routeSelectionMode = RouteSelectionMode.SELECTING_GOAL,
                    message = "繧ｹ繧ｿ繝ｼ繝医ｒ險ｭ螳壹＠縺ｾ縺励◆縲よｬ｡縺ｫ繧ｴ繝ｼ繝ｫ繧帝∈謚槭＠縺ｦ縺上□縺輔＞"
                )
            }

            RouteSelectionMode.SELECTING_GOAL -> {
                if (index == null || index !in selectionCandidateIndices) {
                    _uiState.value = state.copy(message = "隱ｲ鬘悟・縺ｮ繝帙・繝ｫ繝峨°繧峨ざ繝ｼ繝ｫ繧帝∈謚槭＠縺ｦ縺上□縺輔＞")
                    return
                }
                if (index == state.startHoldIndex) {
                    _uiState.value = state.copy(message = "繧ｹ繧ｿ繝ｼ繝医→繧ｴ繝ｼ繝ｫ縺ｯ蛻･縺ｮ繝帙・繝ｫ繝峨↓縺励※縺上□縺輔＞")
                    return
                }
                _uiState.value = state.copy(
                    selectedHoldIndex = index,
                    goalHoldIndex = index,
                    challengeHoldIndices = emptySet(),
                    challengeOrderedHoldIndices = emptyList(),
                    routeSelectionMode = RouteSelectionMode.NONE,
                    message = "繧ｴ繝ｼ繝ｫ繧定ｨｭ螳壹＠縺ｾ縺励◆"
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
                        "隱ｲ鬘後↓繝帙・繝ｫ繝峨ｒ霑ｽ蜉縺励∪縺励◆"
                    } else {
                        "隱ｲ鬘後°繧峨・繝ｼ繝ｫ繝峨ｒ螟悶＠縺ｾ縺励◆"
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
            routeSelectionMode = normalizedRouteSelectionMode
        )
    }

    fun onHoldCountVarianceChanged(value: Float) {
        updateRouteTuning { copy(holdCountVariance = value.coerceIn(0f, 1f)) }
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

    fun onCandidateSelectionRandomnessChanged(value: Float) {
        updateRouteTuning { copy(candidateSelectionRandomness = value.coerceIn(0f, 1f)) }
    }

    fun onFinalSelectionRandomnessChanged(value: Float) {
        updateRouteTuning { copy(finalSelectionRandomness = value.coerceIn(0f, 1f)) }
    }

    fun drawRandomChallengeHolds() {
        val state = _uiState.value
        if (state.holds.isEmpty()) {
            _uiState.value = state.copy(message = "繝帙・繝ｫ繝峨′縺ゅｊ縺ｾ縺帙ｓ")
            return
        }

        val requestedCount = state.drawCountInput.toIntOrNull()
        if (requestedCount == null || requestedCount < 2) {
            _uiState.value = state.copy(message = "謚ｽ驕ｸ縺吶ｋ繝帙・繝ｫ繝画焚繧貞・蜉帙＠縺ｦ縺上□縺輔＞")
            return
        }

        val startIndex = state.startHoldIndex ?: run {
            _uiState.value = state.copy(message = "繧ｹ繧ｿ繝ｼ繝医ｒ險ｭ螳壹＠縺ｦ縺上□縺輔＞")
            return
        }
        val goalIndex = state.goalHoldIndex ?: run {
            _uiState.value = state.copy(message = "繧ｴ繝ｼ繝ｫ繧定ｨｭ螳壹＠縺ｦ縺上□縺輔＞")
            return
        }
        if (startIndex == goalIndex) {
            _uiState.value = state.copy(message = "繧ｹ繧ｿ繝ｼ繝医→繧ｴ繝ｼ繝ｫ縺ｯ蛻･縺ｮ繝帙・繝ｫ繝峨↓縺励※縺上□縺輔＞")
            return
        }

        val drawSourceIndices = challengeSelectionCandidateIndices(state).toMutableSet().apply {
            add(startIndex)
            add(goalIndex)
        }
        if (drawSourceIndices.isEmpty()) {
            _uiState.value = state.copy(message = "謚ｽ驕ｸ蟇ｾ雎｡縺ｮ繝帙・繝ｫ繝峨′縺ゅｊ縺ｾ縺帙ｓ")
            return
        }

        val actualCount = requestedCount.coerceAtMost(drawSourceIndices.size)
        val selectedOrderedIndices = ChallengeRouteGenerator.generate(
            holds = state.holds,
            sourceIndices = drawSourceIndices,
            startIndex = startIndex,
            goalIndex = goalIndex,
            targetCount = actualCount,
            tuning = state.routeTuning,
            reachCalibrationReference = state.reachCalibrationReference
        ) ?: run {
            _uiState.value = state.copy(
                message = if (state.reachCalibrationReference != null) {
                    "150cm以内でつながる課題を生成できませんでした"
                } else {
                    "隱ｲ鬘後ｒ逕滓・縺ｧ縺阪∪縺帙ｓ縺ｧ縺励◆"
                }
            )
            return
        }
        val selectedIndices = selectedOrderedIndices.toSet()

        _uiState.value = state.copy(
            challengeHoldIndices = selectedIndices,
            challengeOrderedHoldIndices = selectedOrderedIndices,
            selectedHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = if (requestedCount > drawSourceIndices.size) {
                "蛟呵｣懈焚繧定ｶ・∴縺溘◆繧・${drawSourceIndices.size} 蛟九☆縺ｹ縺ｦ繧帝∈謚槭＠縺ｾ縺励◆"
            } else {
                "謚ｽ驕ｸ縺ｧ繝帙・繝ｫ繝峨ｒ驕ｸ謚槭＠縺ｾ縺励◆"
            }
        )
    }

    fun startDrawTargetSelection() {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = true,
            message = "繝輔Μ繝ｼ繝上Φ繝峨〒謚ｽ驕ｸ蟇ｾ雎｡縺ｮ遽・峇繧偵↑縺槭▲縺ｦ縺上□縺輔＞"
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
            drawTargetHoldIndices = indices,
            hasDrawTargetSelection = true,
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = if (eligibleIndices.isEmpty()) {
                "謚ｽ驕ｸ蟇ｾ雎｡縺ｫ蜈･繧九・繝ｼ繝ｫ繝峨′隕九▽縺九ｊ縺ｾ縺帙ｓ縺ｧ縺励◆"
            } else {
                "謚ｽ驕ｸ蟇ｾ雎｡繧・${eligibleIndices.size} 蛟九・繝帙・繝ｫ繝峨↓險ｭ螳壹＠縺ｾ縺励◆"
            }
        )
    }

    fun startChallengeStartGoalSelection() {
        val state = _uiState.value
        if (state.isDrawTargetSelectionMode) {
            _uiState.value = state.copy(message = "遽・峇驕ｸ謚槭ｒ邨ゅ∴縺ｦ縺九ｉ繧ｹ繧ｿ繝ｼ繝医→繧ｴ繝ｼ繝ｫ繧帝∈謚槭＠縺ｦ縺上□縺輔＞")
            return
        }
        if (challengeSelectionCandidateIndices(state).isEmpty()) {
            _uiState.value = state.copy(message = "蜈医↓隱ｲ鬘後↓蜷ｫ繧√ｋ繝帙・繝ｫ繝峨ｒ驕ｸ謚槭＠縺ｦ縺上□縺輔＞")
            return
        }
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.SELECTING_START,
            message = "隱ｲ鬘悟・縺ｮ繝帙・繝ｫ繝峨ｒ繧ｿ繝・・縺励※繧ｹ繧ｿ繝ｼ繝医ｒ驕ｸ謚槭＠縺ｦ縺上□縺輔＞"
        )
    }

    fun clearChallengeSelection() {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            challengeOrderedHoldIndices = emptyList(),
            drawTargetHoldIndices = emptySet(),
            hasDrawTargetSelection = false,
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isDrawTargetSelectionMode = false,
            message = "隱ｲ鬘碁∈謚槭ｒ繧ｯ繝ｪ繧｢縺励∪縺励◆"
        )
    }

    fun onBackPressed() {
        when (_uiState.value.currentScreen) {
            AppScreen.LIST -> Unit
            AppScreen.CAMERA -> _uiState.value = _uiState.value.copy(currentScreen = AppScreen.LIST)
            AppScreen.EDIT_MENU -> returnToList()
            AppScreen.REACH_CALIBRATION -> {
                if (_uiState.value.currentWallId != null && !_uiState.value.reachCalibrationReturnToHoldEditor) {
                    requestBackToList()
                } else {
                    backFromReachCalibration()
                }
            }
            AppScreen.HOLD_EDITOR -> requestBackToList()
            AppScreen.HOLD_SCORING -> {
                if (_uiState.value.currentWallId != null) {
                    requestBackToList()
                } else {
                    returnToHoldEditorFromScoring()
                }
            }
            AppScreen.CHALLENGE_CREATOR -> returnToList()
        }
    }

    fun requestBackToList() {
        val state = _uiState.value
        when (state.currentScreen) {
            AppScreen.EDIT_MENU -> returnToList()
            AppScreen.REACH_CALIBRATION,
            AppScreen.HOLD_EDITOR,
            AppScreen.HOLD_SCORING -> {
                if (state.isHoldEditorDirty) {
                    _uiState.value = state.copy(showDiscardDialog = true)
                } else {
                    discardEditorAndReturnToList()
                }
            }

            AppScreen.CAMERA,
            AppScreen.CHALLENGE_CREATOR -> returnToList()

            AppScreen.LIST -> Unit
        }
    }

    fun dismissDiscardDialog() {
        _uiState.value = _uiState.value.copy(showDiscardDialog = false)
    }

    fun discardEditorAndReturnToList() {
        _uiState.value = MainUiState(
            currentScreen = AppScreen.LIST,
            savedWalls = _uiState.value.savedWalls,
            drawCountInput = _uiState.value.drawCountInput,
            holdTapAreaSize = _uiState.value.holdTapAreaSize,
            challengeDifficultyScoreMin = _uiState.value.challengeDifficultyScoreMin,
            challengeDifficultyScoreMax = _uiState.value.challengeDifficultyScoreMax,
            routeTuning = _uiState.value.routeTuning
        )
    }

    fun returnToList() {
        _uiState.value = MainUiState(
            currentScreen = AppScreen.LIST,
            savedWalls = _uiState.value.savedWalls,
            drawCountInput = _uiState.value.drawCountInput,
            holdTapAreaSize = _uiState.value.holdTapAreaSize,
            challengeDifficultyScoreMin = _uiState.value.challengeDifficultyScoreMin,
            challengeDifficultyScoreMax = _uiState.value.challengeDifficultyScoreMax,
            routeTuning = _uiState.value.routeTuning
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun defaultWallTitle(): String {
        return "螢＼" + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date())
    }

    private fun updateRouteTuning(
        transform: RouteGenerationTuning.() -> RouteGenerationTuning
    ) {
        _uiState.value = _uiState.value.copy(
            routeTuning = _uiState.value.routeTuning.transform()
        )
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
}
