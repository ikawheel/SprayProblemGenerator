package com.ikeansoft.sprayproblemgenerator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.ui.AppSecondaryTextColor
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppOutlinedButton
import com.ikeansoft.sprayproblemgenerator.ui.components.WallRegistrationStepScaffold
import com.ikeansoft.sprayproblemgenerator.ui.stringResourceByName
import com.ikeansoft.sprayproblemgenerator.ui.canvas.HoldAttributeCanvasScreen

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
    val highlightedStartCandidateIndices = when (editMode) {
        HoldAttributeEditMode.START, HoldAttributeEditMode.CLEAR -> startCandidateIndices
        HoldAttributeEditMode.GOAL -> emptySet()
    }
    val highlightedGoalCandidateIndices = when (editMode) {
        HoldAttributeEditMode.GOAL, HoldAttributeEditMode.CLEAR -> goalCandidateIndices
        HoldAttributeEditMode.START -> emptySet()
    }
    val imageAspectRatio = bitmap?.takeIf { it.height > 0 }?.let {
        wallImageDisplayAspectRatio(
            imageWidth = it.width,
            imageHeight = it.height
        )
    }
    val modeDescriptionResId = when (editMode) {
        HoldAttributeEditMode.START -> R.string.hold_attribute_mode_description_start
        HoldAttributeEditMode.GOAL -> R.string.hold_attribute_mode_description_goal
        HoldAttributeEditMode.CLEAR -> R.string.hold_attribute_mode_description_clear
    }
    val modeButtonContentPadding = PaddingValues(
        horizontal = 7.dp,
        vertical = ButtonDefaults.ContentPadding.calculateTopPadding()
    )

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
                    selectedIndex = null,
                    startCandidateHoldIndices = highlightedStartCandidateIndices,
                    goalCandidateHoldIndices = highlightedGoalCandidateIndices,
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
                text = stringResource(R.string.hold_attribute_editor_help),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall
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
                        modifier = Modifier.weight(1f),
                        contentPadding = modeButtonContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.hold_attribute_mode_start),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    AppOutlinedButton(
                        onClick = { editModeName = HoldAttributeEditMode.START.name },
                        modifier = Modifier.weight(1f),
                        contentPadding = modeButtonContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.hold_attribute_mode_start),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (editMode == HoldAttributeEditMode.GOAL) {
                    AppButton(
                        onClick = { editModeName = HoldAttributeEditMode.GOAL.name },
                        modifier = Modifier.weight(1f),
                        contentPadding = modeButtonContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.hold_attribute_mode_goal),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    AppOutlinedButton(
                        onClick = { editModeName = HoldAttributeEditMode.GOAL.name },
                        modifier = Modifier.weight(1f),
                        contentPadding = modeButtonContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.hold_attribute_mode_goal),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (editMode == HoldAttributeEditMode.CLEAR) {
                    AppButton(
                        onClick = { editModeName = HoldAttributeEditMode.CLEAR.name },
                        modifier = Modifier.weight(1f),
                        contentPadding = modeButtonContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.hold_attribute_mode_clear),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    AppOutlinedButton(
                        onClick = { editModeName = HoldAttributeEditMode.CLEAR.name },
                        modifier = Modifier.weight(1f),
                        contentPadding = modeButtonContentPadding
                    ) {
                        Text(
                            text = stringResource(R.string.hold_attribute_mode_clear),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Text(
                text = stringResource(modeDescriptionResId),
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
