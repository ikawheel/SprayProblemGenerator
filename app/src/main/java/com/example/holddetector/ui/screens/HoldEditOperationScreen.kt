package com.example.holddetector.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.model.Hold
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.HoldEditorTool
import com.example.holddetector.ui.HoldTapAreaSize
import com.example.holddetector.ui.RouteSelectionMode
import com.example.holddetector.ui.canvas.HoldCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.DisplayColorSettings
import com.example.holddetector.ui.components.ScreenHeader

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
    onHoldTapAreaSizeChange: (HoldTapAreaSize) -> Unit,
    onConfirm: (List<Hold>, Int?) -> Unit,
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
        ScreenHeader(
            title = stringResource(holdEditOperationTitleResId(mode)),
            modifier = Modifier.padding(bottom = 16.dp)
        )

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
                        Text(
                            text = stringResource(R.string.hold_tap_size_label),
                            color = AppTextColor,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
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
}

private fun holdEditOperationTitleResId(mode: HoldEditorTool): Int {
    return when (mode) {
        HoldEditorTool.ADD -> R.string.hold_editor_dialog_title_add
        HoldEditorTool.EXTEND -> R.string.hold_editor_dialog_title_extend
        HoldEditorTool.ERASE -> R.string.hold_editor_dialog_title_erase
        HoldEditorTool.DELETE -> R.string.hold_editor_dialog_title_delete
    }
}
