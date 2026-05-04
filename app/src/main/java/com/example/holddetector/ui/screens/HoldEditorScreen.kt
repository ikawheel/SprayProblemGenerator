package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.model.Hold
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSectionSurfaceColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.HoldEditorTool
import com.example.holddetector.ui.HoldTapAreaSize
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.RouteSelectionMode
import com.example.holddetector.ui.canvas.HoldCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppConfirmDialog
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.BottomActionBar
import com.example.holddetector.ui.components.ScreenHeader
import com.example.holddetector.ui.selectors.deriveHoldEditorUiModel

private data class HoldEditorSnapshot(
    val holds: List<Hold>,
    val selectedIndex: Int?
)

@Composable
fun HoldEditorScreen(
    state: MainUiState,
    onReturnToList: () -> Unit,
    onOpenReachCalibration: () -> Unit,
    onHoldTapAreaSizeChange: (HoldTapAreaSize) -> Unit,
    onSaveEditedHolds: (List<Hold>, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap ?: return
    val uiModel = deriveHoldEditorUiModel(state)
    val isEditingExistingWall = state.currentWallId != null
    var activeTool by remember { mutableStateOf(HoldEditorTool.ADD) }
    var draftHolds by remember(state.holds) { mutableStateOf(state.holds) }
    var selectedIndex by remember(state.holds, state.selectedHoldIndex) {
        mutableStateOf(state.selectedHoldIndex?.takeIf { it in state.holds.indices })
    }
    var undoStack by remember(state.holds, state.selectedHoldIndex) {
        mutableStateOf<List<HoldEditorSnapshot>>(emptyList())
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isAutoMergeEnabled by remember { mutableStateOf(true) }
    val hasUnsavedDraftChanges = draftHolds != state.holds
    val isDeleteMode = activeTool == HoldEditorTool.DELETE
    val compactButtonContentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    val compactButtonMinHeight = 32.dp
    val compactSpacing = 8.dp
    val compactControlSpacing = 1.dp
    val compactSectionSpacing = 2.dp
    val holdEditorImageAspectRatio = remember(bitmap.width, bitmap.height) {
        val baseAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        if (bitmap.width > bitmap.height) {
            baseAspectRatio / 1.5f
        } else {
            baseAspectRatio
        }
    }

    BackHandler {
        when {
            showDiscardDialog -> showDiscardDialog = false
            isEditingExistingWall && hasUnsavedDraftChanges -> showDiscardDialog = true
            else -> onReturnToList()
        }
    }

    fun updateDraft(updatedHolds: List<Hold>, updatedSelectedIndex: Int?) {
        if (updatedHolds == draftHolds && updatedSelectedIndex == selectedIndex) return
        undoStack = undoStack + HoldEditorSnapshot(
            holds = draftHolds,
            selectedIndex = selectedIndex
        )
        draftHolds = updatedHolds
        selectedIndex = updatedSelectedIndex?.takeIf { it in updatedHolds.indices }
    }

    fun saveDraft() {
        onSaveEditedHolds(draftHolds, selectedIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        ScreenHeader(
            title = stringResource(uiModel.titleResId),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(compactSpacing)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(holdEditorImageAspectRatio)
                    .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                    .clipToBounds()
            ) {
                HoldCanvasScreen(
                    bitmap = bitmap,
                    holds = draftHolds,
                    selectedIndex = selectedIndex,
                    challengeHoldIndices = emptySet(),
                    startCandidateHoldIndices = draftHolds.withIndex()
                        .filter { it.value.isStartCandidate }
                        .mapTo(linkedSetOf()) { it.index },
                    goalCandidateHoldIndices = draftHolds.withIndex()
                        .filter { it.value.isGoalCandidate }
                        .mapTo(linkedSetOf()) { it.index },
                    startHoldIndex = null,
                    goalHoldIndex = null,
                    routeSelectionMode = RouteSelectionMode.NONE,
                    reachCalibrationReference = null,
                    pendingReachCalibrationPoint = null,
                    isReachCalibrationSelectionMode = false,
                    displayColorSettings = state.displayColorSettings,
                    holdTapAreaSize = state.holdTapAreaSize,
                    holdEditorTool = activeTool,
                    isAutoMergeEnabled = isAutoMergeEnabled,
                    isSelectionOnly = isDeleteMode,
                    onHoldTapped = { index ->
                        if (isDeleteMode) {
                            if (index != null && index in draftHolds.indices) {
                                val updatedHolds = draftHolds.toMutableList().apply {
                                    removeAt(index)
                                }
                                updateDraft(updatedHolds, null)
                            }
                        } else {
                            selectedIndex = index
                        }
                    },
                    onReachCalibrationPointSelected = {},
                    onManualHoldCreated = { hold ->
                        val updatedHolds = draftHolds + hold
                        updateDraft(updatedHolds, updatedHolds.lastIndex)
                    },
                    onEditedHoldApplied = { targetIndex, replacementHolds ->
                        if (targetIndex in draftHolds.indices) {
                            val updatedHolds = buildList {
                                addAll(draftHolds.take(targetIndex))
                                addAll(replacementHolds)
                                addAll(draftHolds.drop(targetIndex + 1))
                            }
                            updateDraft(
                                updatedHolds = updatedHolds,
                                updatedSelectedIndex = when {
                                    replacementHolds.isEmpty() -> null
                                    else -> targetIndex.coerceAtMost(updatedHolds.lastIndex)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(compactSpacing)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppSectionSurfaceColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(compactControlSpacing)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            holdEditorToolButtons().forEach { tool ->
                                val isSelected = activeTool == tool
                                val label = stringResource(
                                    when (tool) {
                                        HoldEditorTool.ADD -> R.string.hold_editor_tool_add
                                        HoldEditorTool.ERASE -> R.string.hold_editor_tool_erase
                                        HoldEditorTool.DELETE -> R.string.hold_editor_tool_delete
                                        HoldEditorTool.EXTEND -> R.string.hold_editor_tool_extend
                                    }
                                )
                                HoldEditorModeTab(
                                    label = label,
                                    selected = isSelected,
                                    enabled = tool != HoldEditorTool.DELETE || draftHolds.isNotEmpty(),
                                    onClick = { activeTool = tool },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        AppOutlinedButton(
                            onClick = {
                                val previous = undoStack.lastOrNull() ?: return@AppOutlinedButton
                                undoStack = undoStack.dropLast(1)
                                draftHolds = previous.holds
                                selectedIndex = previous.selectedIndex?.takeIf { it in draftHolds.indices }
                            },
                            enabled = undoStack.isNotEmpty(),
                            contentPadding = compactButtonContentPadding,
                            minHeight = compactButtonMinHeight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 1.dp)
                        ) {
                            Text(stringResource(R.string.undo))
                        }

                        if (!isDeleteMode) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(compactSectionSpacing)
                            ) {
                                Text(
                                    text = stringResource(R.string.hold_tap_size_label),
                                    color = AppTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HoldTapAreaSize.values().forEach { size ->
                                        val buttonText = when (size) {
                                            HoldTapAreaSize.SMALL -> stringResource(R.string.hold_tap_size_small)
                                            HoldTapAreaSize.MEDIUM -> stringResource(R.string.hold_tap_size_medium)
                                            HoldTapAreaSize.LARGE -> stringResource(R.string.hold_tap_size_large)
                                        }
                                        if (size == state.holdTapAreaSize) {
                                            AppButton(
                                                onClick = { onHoldTapAreaSizeChange(size) },
                                                contentPadding = compactButtonContentPadding,
                                                minHeight = compactButtonMinHeight,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(buttonText)
                                            }
                                        } else {
                                            AppOutlinedButton(
                                                onClick = { onHoldTapAreaSizeChange(size) },
                                                contentPadding = compactButtonContentPadding,
                                                minHeight = compactButtonMinHeight,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(buttonText)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (activeTool == HoldEditorTool.ADD) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = compactSectionSpacing - compactControlSpacing),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.hold_editor_auto_merge_label),
                                    color = AppTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = isAutoMergeEnabled,
                                    onCheckedChange = { isAutoMergeEnabled = it }
                                )
                            }
                        }

                    }
                }
            }
        }

        if (isEditingExistingWall) {
            BottomActionBar {
                AppButton(
                    onClick = { saveDraft() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        } else {
            AppButton(
                onClick = {
                    if (hasUnsavedDraftChanges) {
                        saveDraft()
                    }
                    onOpenReachCalibration()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            ) {
                Text(stringResource(R.string.open_reach_calibration))
            }
        }
    }

    if (showDiscardDialog) {
        AppConfirmDialog(
            title = stringResource(R.string.back_to_list),
            message = stringResource(R.string.discard_dialog_message),
            confirmText = stringResource(R.string.discard_dialog_confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showDiscardDialog = false
                onReturnToList()
            },
            onDismissRequest = { showDiscardDialog = false }
        )
    }
}

private fun holdEditorToolButtons(): List<HoldEditorTool> {
    return listOf(
        HoldEditorTool.ADD,
        HoldEditorTool.ERASE,
        HoldEditorTool.DELETE
    )
}

@Composable
private fun HoldEditorModeTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = when {
        !enabled -> AppSecondaryTextColor.copy(alpha = 0.45f)
        selected -> accentColor
        else -> AppTextColor
    }

    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (selected) accentColor else AppSecondaryTextColor.copy(alpha = 0.18f)
                )
        )
    }
}
