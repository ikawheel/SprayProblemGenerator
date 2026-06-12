package com.ikeansoft.sprayproblemgenerator.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.ui.AppSubtleSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.HoldEditorTool
import com.ikeansoft.sprayproblemgenerator.ui.HoldTapAreaSize
import com.ikeansoft.sprayproblemgenerator.ui.RouteSelectionMode
import com.ikeansoft.sprayproblemgenerator.ui.canvas.HoldCanvasScreen
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppConfirmDialog
import com.ikeansoft.sprayproblemgenerator.ui.components.AppOutlinedButton
import com.ikeansoft.sprayproblemgenerator.ui.DisplayColorSettings

private data class HoldEditOperationSnapshot(
    val holds: List<Hold>,
    val selectedIndex: Int?
)

@Composable
fun HoldEditOperationScreen(
    mode: HoldEditorTool,
    bitmap: Bitmap?,
    initialHolds: List<Hold>,
    initialSelectedIndex: Int?,
    holdTapAreaSize: HoldTapAreaSize,
    displayColorSettings: DisplayColorSettings,
    isEditingExistingWall: Boolean,
    onHoldTapAreaSizeChange: (HoldTapAreaSize) -> Unit,
    onConfirm: (List<Hold>, Int?) -> Unit,
    onRequestBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeBitmap = bitmap ?: return
    val isDeleteMode = mode == HoldEditorTool.DELETE
    var draftHolds by remember(mode, initialHolds) { mutableStateOf(initialHolds) }
    var selectedIndex by remember(mode, initialHolds, initialSelectedIndex) {
        mutableStateOf(initialSelectedIndex?.takeIf { it in initialHolds.indices })
    }
    var undoStack by remember(mode, initialHolds, initialSelectedIndex) {
        mutableStateOf<List<HoldEditOperationSnapshot>>(emptyList())
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isAutoMergeEnabled by remember(mode) { mutableStateOf(true) }
    val hasUnsavedDraftChanges = draftHolds != initialHolds

    BackHandler {
        when {
            showDiscardDialog -> {
                showDiscardDialog = false
            }

            isEditingExistingWall && hasUnsavedDraftChanges -> {
                showDiscardDialog = true
            }

            else -> {
                onRequestBack()
            }
        }
    }

    fun updateDraft(updatedHolds: List<Hold>, updatedSelectedIndex: Int?) {
        if (updatedHolds == draftHolds && updatedSelectedIndex == selectedIndex) return
        undoStack = undoStack + HoldEditOperationSnapshot(
            holds = draftHolds,
            selectedIndex = selectedIndex
        )
        draftHolds = updatedHolds
        selectedIndex = updatedSelectedIndex?.takeIf { it in updatedHolds.indices }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                    .clipToBounds()
            ) {
                HoldCanvasScreen(
                    bitmap = safeBitmap,
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
                    displayColorSettings = displayColorSettings,
                    holdTapAreaSize = holdTapAreaSize,
                    holdEditorTool = mode,
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

            AppOutlinedButton(
                onClick = {
                    val previous = undoStack.lastOrNull() ?: return@AppOutlinedButton
                    undoStack = undoStack.dropLast(1)
                    draftHolds = previous.holds
                    selectedIndex = previous.selectedIndex?.takeIf { it in draftHolds.indices }
                },
                enabled = undoStack.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp)
            ) {
                Text(stringResource(R.string.undo))
            }

            if (!isDeleteMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    color = AppSubtleSurfaceColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HoldTapAreaSize.values().forEach { size ->
                                val buttonText = when (size) {
                                    HoldTapAreaSize.SMALL -> stringResource(R.string.hold_tap_size_small)
                                    HoldTapAreaSize.MEDIUM -> stringResource(R.string.hold_tap_size_medium)
                                    HoldTapAreaSize.LARGE -> stringResource(R.string.hold_tap_size_large)
                                    HoldTapAreaSize.EXTRA_LARGE -> stringResource(R.string.hold_tap_size_extra_large)
                                }
                                if (size == holdTapAreaSize) {
                                    AppButton(
                                        onClick = { onHoldTapAreaSizeChange(size) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(buttonText)
                                    }
                                } else {
                                    AppOutlinedButton(
                                        onClick = { onHoldTapAreaSizeChange(size) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(buttonText)
                                    }
                                }
                            }
                        }

                        if (mode == HoldEditorTool.ADD) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(
                    onClick = { onConfirm(draftHolds, selectedIndex) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
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
                onRequestBack()
            },
            onDismissRequest = { showDiscardDialog = false }
        )
    }
}
