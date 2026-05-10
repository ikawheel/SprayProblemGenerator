package com.example.holddetector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.WallRegistrationStepScaffold
import com.example.holddetector.ui.selectors.deriveHoldEditorUiModel
import com.example.holddetector.ui.stringResourceByName
import com.example.holddetector.ui.canvas.HoldAttributeCanvasScreen

private enum class HoldAttributeEditMode {
    START,
    GOAL,
    CLEAR
}

@Composable
fun HoldAttributeEditorScreen(
    state: MainUiState,
    onBackToHoldEditor: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onSaveAndExit: () -> Unit,
    onOpenHoldScoring: () -> Unit,
    onAssignHoldAsStartCandidate: (Int?) -> Unit,
    onAssignHoldAsGoalCandidate: (Int?) -> Unit,
    onClearHoldAttributes: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveHoldEditorUiModel(state)
    val isEditingExistingWall = state.currentWallId != null
    var editModeName by rememberSaveable {
        mutableStateOf(HoldAttributeEditMode.START.name)
    }
    val editMode = HoldAttributeEditMode.valueOf(editModeName)
    val startCandidateIndices = state.holds.withIndex()
        .filter { it.value.isStartCandidate }
        .mapTo(linkedSetOf()) { it.index }
    val goalCandidateIndices = state.holds.withIndex()
        .filter { it.value.isGoalCandidate }
        .mapTo(linkedSetOf()) { it.index }
    val imageAspectRatio = bitmap?.takeIf { it.height > 0 }?.let {
        wallImageDisplayAspectRatio(
            imageWidth = it.width,
            imageHeight = it.height
        )
    }

    val attributeSummaryResId = when {
        !uiModel.hasSelectedHold -> R.string.hold_editor_attribute_none_selected
        uiModel.selectedHoldIsStartCandidate && uiModel.selectedHoldIsGoalCandidate ->
            R.string.hold_editor_attribute_summary_both
        uiModel.selectedHoldIsStartCandidate ->
            R.string.hold_editor_attribute_summary_start
        uiModel.selectedHoldIsGoalCandidate ->
            R.string.hold_editor_attribute_summary_goal
        else -> R.string.hold_editor_attribute_summary_none
    }

    WallRegistrationStepScaffold(
        modifier = modifier,
        headerText = stringResource(R.string.registration_step_hold_attribute_title),
        imageAspectRatio = imageAspectRatio,
        useFullImageViewport = true,
        imageContent = {
            if (bitmap != null) {
                HoldAttributeCanvasScreen(
                    bitmap = bitmap,
                    holds = state.holds,
                    selectedIndex = state.selectedHoldIndex,
                    startCandidateHoldIndices = startCandidateIndices,
                    goalCandidateHoldIndices = goalCandidateIndices,
                    displayColorSettings = state.displayColorSettings,
                    onHoldTapped = { index ->
                        when (editMode) {
                            HoldAttributeEditMode.START -> onAssignHoldAsStartCandidate(index)
                            HoldAttributeEditMode.GOAL -> onAssignHoldAsGoalCandidate(index)
                            HoldAttributeEditMode.CLEAR -> onClearHoldAttributes(index)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        bodyContent = {
            Text(
                text = stringResource(R.string.hold_editor_attribute_title),
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editMode == HoldAttributeEditMode.START) {
                    AppButton(
                        onClick = { editModeName = HoldAttributeEditMode.START.name },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hold_attribute_mode_start))
                    }
                } else {
                    AppOutlinedButton(
                        onClick = { editModeName = HoldAttributeEditMode.START.name },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hold_attribute_mode_start))
                    }
                }

                if (editMode == HoldAttributeEditMode.GOAL) {
                    AppButton(
                        onClick = { editModeName = HoldAttributeEditMode.GOAL.name },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hold_attribute_mode_goal))
                    }
                } else {
                    AppOutlinedButton(
                        onClick = { editModeName = HoldAttributeEditMode.GOAL.name },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hold_attribute_mode_goal))
                    }
                }

                if (editMode == HoldAttributeEditMode.CLEAR) {
                    AppButton(
                        onClick = { editModeName = HoldAttributeEditMode.CLEAR.name },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hold_attribute_mode_clear))
                    }
                } else {
                    AppOutlinedButton(
                        onClick = { editModeName = HoldAttributeEditMode.CLEAR.name },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.hold_attribute_mode_clear))
                    }
                }
            }

            Text(
                text = stringResource(attributeSummaryResId),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        },
        footerContent = {
            if (isEditingExistingWall) {
                AppButton(
                    onClick = onSaveAndExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.overwrite_save))
                }
            } else {
                AppButton(
                    onClick = onOpenHoldScoring,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResourceByName("hold_attribute_open_scoring"))
                }
            }
        }
    )
}
