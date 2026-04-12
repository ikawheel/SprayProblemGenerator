package com.example.holddetector.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.holddetector.data.WallStorageRepository
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.Hold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                isBusy = false
            )
        }
    }

    fun startNewWall() {
        _uiState.value = MainUiState(
            currentScreen = AppScreen.CAMERA,
            savedWalls = _uiState.value.savedWalls,
            drawCountInput = _uiState.value.drawCountInput
        )
    }

    fun onPhotoCaptured(
        bitmap: Bitmap,
        capturedOrientation: CapturedOrientation,
        capturedRotationDegrees: Int
    ) {
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.HOLD_EDITOR,
            currentWallId = null,
            wallTitle = defaultWallTitle(),
            capturedBitmap = bitmap,
            capturedOrientation = capturedOrientation,
            capturedRotationDegrees = capturedRotationDegrees,
            holds = emptyList(),
            selectedHoldIndex = null,
            challengeHoldIndices = emptySet(),
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            isHoldEditorDirty = true,
            showDiscardDialog = false,
            message = "蜀咏悄繧定ｪｭ縺ｿ霎ｼ縺ｿ縺ｾ縺励◆縲ゅち繝・・繧・ラ繝ｩ繝・げ縺ｧ繝帙・繝ｫ繝峨ｒ逋ｻ骭ｲ縺励※縺上□縺輔＞"
        )
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
                    message = "螢√ョ繝ｼ繧ｿ繧帝幕縺代∪縺帙ｓ縺ｧ縺励◆"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                currentScreen = AppScreen.HOLD_EDITOR,
                currentWallId = detail.id,
                wallTitle = detail.title,
                capturedBitmap = detail.bitmap,
                capturedOrientation = detail.capturedOrientation,
                capturedRotationDegrees = detail.capturedRotationDegrees,
                holds = detail.holds,
                selectedHoldIndex = null,
                challengeHoldIndices = emptySet(),
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isHoldEditorDirty = false,
                showDiscardDialog = false,
                isBusy = false,
                message = "螢√ョ繝ｼ繧ｿ繧定ｪｭ縺ｿ霎ｼ縺ｿ縺ｾ縺励◆"
            )
        }
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
                selectedHoldIndex = null,
                challengeHoldIndices = emptySet(),
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
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

    fun addManualHold(hold: Hold) {
        val state = _uiState.value
        val updatedHolds = state.holds + hold
        _uiState.value = state.copy(
            holds = updatedHolds,
            selectedHoldIndex = updatedHolds.lastIndex,
            isHoldEditorDirty = true,
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
            message = "繝帙・繝ｫ繝峨ｒ蜑企勁縺励∪縺励◆"
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
                    capturedOrientation = state.capturedOrientation,
                    capturedRotationDegrees = state.capturedRotationDegrees
                )
            }
            val refreshed = withContext(Dispatchers.IO) { repository.loadAllSummaries() }
            _uiState.value = MainUiState(
                currentScreen = AppScreen.LIST,
                savedWalls = refreshed,
                drawCountInput = state.drawCountInput,
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
                startHoldIndex = null,
                goalHoldIndex = null,
                routeSelectionMode = RouteSelectionMode.NONE,
                isHoldEditorDirty = false,
                showDiscardDialog = false,
                isBusy = false,
                message = "螢√ｒ菫晏ｭ倥＠縺ｦ隱ｲ鬘御ｽ懈・繧帝幕縺阪∪縺励◆"
            )
        }
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

        when (state.routeSelectionMode) {
            RouteSelectionMode.SELECTING_START -> {
                if (index == null || index !in state.challengeHoldIndices) {
                    _uiState.value = state.copy(message = "隱ｲ鬘悟・縺ｮ繝帙・繝ｫ繝峨°繧峨せ繧ｿ繝ｼ繝医ｒ驕ｸ謚槭＠縺ｦ縺上□縺輔＞")
                    return
                }
                _uiState.value = state.copy(
                    selectedHoldIndex = index,
                    startHoldIndex = index,
                    routeSelectionMode = RouteSelectionMode.SELECTING_GOAL,
                    message = "繧ｹ繧ｿ繝ｼ繝医ｒ險ｭ螳壹＠縺ｾ縺励◆縲よｬ｡縺ｫ繧ｴ繝ｼ繝ｫ繧帝∈謚槭＠縺ｦ縺上□縺輔＞"
                )
            }

            RouteSelectionMode.SELECTING_GOAL -> {
                if (index == null || index !in state.challengeHoldIndices) {
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
                    routeSelectionMode = RouteSelectionMode.NONE,
                    message = "繧ｴ繝ｼ繝ｫ繧定ｨｭ螳壹＠縺ｾ縺励◆"
                )
            }

            RouteSelectionMode.NONE -> {
                if (index == null) return
                val updated = state.challengeHoldIndices.toMutableSet()
                val added = if (updated.contains(index)) {
                    updated.remove(index)
                    false
                } else {
                    updated.add(index)
                    true
                }
                _uiState.value = state.copy(
                    selectedHoldIndex = index,
                    challengeHoldIndices = updated,
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

    fun drawRandomChallengeHolds() {
        val state = _uiState.value
        if (state.holds.isEmpty()) {
            _uiState.value = state.copy(message = "繝帙・繝ｫ繝峨′縺ゅｊ縺ｾ縺帙ｓ")
            return
        }

        val requestedCount = state.drawCountInput.toIntOrNull()
        if (requestedCount == null || requestedCount <= 0) {
            _uiState.value = state.copy(message = "謚ｽ驕ｸ縺吶ｋ繝帙・繝ｫ繝画焚繧貞・蜉帙＠縺ｦ縺上□縺輔＞")
            return
        }

        val actualCount = requestedCount.coerceAtMost(state.holds.size)
        val selectedIndices = state.holds.indices.shuffled().take(actualCount).sorted().toSet()

        _uiState.value = state.copy(
            challengeHoldIndices = selectedIndices,
            selectedHoldIndex = null,
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            message = if (requestedCount > state.holds.size) {
                "蛟呵｣懈焚繧定ｶ・∴縺溘◆繧・${state.holds.size} 蛟九☆縺ｹ縺ｦ繧帝∈謚槭＠縺ｾ縺励◆"
            } else {
                "謚ｽ驕ｸ縺ｧ繝帙・繝ｫ繝峨ｒ驕ｸ謚槭＠縺ｾ縺励◆"
            }
        )
    }

    fun startChallengeStartGoalSelection() {
        val state = _uiState.value
        if (state.challengeHoldIndices.isEmpty()) {
            _uiState.value = state.copy(message = "蜈医↓隱ｲ鬘後↓蜷ｫ繧√ｋ繝帙・繝ｫ繝峨ｒ驕ｸ謚槭＠縺ｦ縺上□縺輔＞")
            return
        }
        _uiState.value = state.copy(
            selectedHoldIndex = null,
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
            startHoldIndex = null,
            goalHoldIndex = null,
            routeSelectionMode = RouteSelectionMode.NONE,
            message = "隱ｲ鬘碁∈謚槭ｒ繧ｯ繝ｪ繧｢縺励∪縺励◆"
        )
    }

    fun onBackPressed() {
        when (_uiState.value.currentScreen) {
            AppScreen.LIST -> Unit
            AppScreen.CAMERA -> _uiState.value = _uiState.value.copy(currentScreen = AppScreen.LIST)
            AppScreen.HOLD_EDITOR -> requestBackToList()
            AppScreen.CHALLENGE_CREATOR -> returnToList()
        }
    }

    fun requestBackToList() {
        val state = _uiState.value
        when (state.currentScreen) {
            AppScreen.HOLD_EDITOR -> {
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
            drawCountInput = _uiState.value.drawCountInput
        )
    }

    fun returnToList() {
        _uiState.value = MainUiState(
            currentScreen = AppScreen.LIST,
            savedWalls = _uiState.value.savedWalls,
            drawCountInput = _uiState.value.drawCountInput
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun defaultWallTitle(): String {
        return "螢＼" + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date())
    }
}
