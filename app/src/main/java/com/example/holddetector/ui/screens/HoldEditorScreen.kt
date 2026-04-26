package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.HoldEditorTool
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.RouteSelectionMode
import com.example.holddetector.ui.canvas.HoldCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.selectors.deriveHoldEditorUiModel

@Composable
fun HoldEditorScreen(
    state: MainUiState,
    onReturnToList: () -> Unit,
    onOpenReachCalibration: () -> Unit,
    onOpenHoldEditOperation: (HoldEditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveHoldEditorUiModel(state)
    val isEditingExistingWall = state.currentWallId != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = AppSurfaceColor,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(uiModel.titleResId),
                    color = AppTextColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            wallImageDisplayAspectRatio(
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height
                            )
                        )
                        .background(AppSubtleSurfaceColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .clipToBounds()
                ) {
                    HoldCanvasScreen(
                        bitmap = bitmap,
                        holds = state.holds,
                        selectedIndex = null,
                        challengeHoldIndices = emptySet(),
                        startCandidateHoldIndices = state.holds.withIndex()
                            .filter { it.value.isStartCandidate }
                            .mapTo(linkedSetOf()) { it.index },
                        goalCandidateHoldIndices = state.holds.withIndex()
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
                        holdEditorTool = HoldEditorTool.ADD,
                        isSelectionOnly = true,
                        onHoldTapped = {},
                        onReachCalibrationPointSelected = {},
                        onManualHoldCreated = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            AppButton(
                onClick = { onOpenHoldEditOperation(HoldEditorTool.ADD) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.hold_editor_tool_add))
            }

            AppButton(
                onClick = { onOpenHoldEditOperation(HoldEditorTool.ERASE) },
                enabled = state.holds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.hold_editor_tool_erase))
            }

            AppButton(
                onClick = { onOpenHoldEditOperation(HoldEditorTool.DELETE) },
                enabled = state.holds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.hold_editor_tool_delete))
            }

            if (isEditingExistingWall) {
                AppOutlinedButton(
                    onClick = onReturnToList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.back_to_list))
                }
            } else {
                AppButton(
                    onClick = onOpenReachCalibration,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_reach_calibration))
                }
            }
        }
    }
}
