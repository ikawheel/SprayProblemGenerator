package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.model.Hold
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.HoldTapAreaSize
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.RouteSelectionMode
import com.example.holddetector.ui.stringResourceByName
import com.example.holddetector.ui.canvas.HoldCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.BottomActionBar
import com.example.holddetector.ui.selectors.deriveHoldEditorUiModel

@Composable
fun HoldEditorScreen(
    state: MainUiState,
    onWallTitleChanged: (String) -> Unit,
    onHoldTapAreaSizeChange: (HoldTapAreaSize) -> Unit,
    onSaveWall: () -> Unit,
    onOpenHoldScoring: () -> Unit,
    onBackToList: () -> Unit,
    onDeleteSelectedHold: () -> Unit,
    onEditorHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveHoldEditorUiModel(state)
    val isEditingExistingWall = state.currentWallId != null
    val reachStatusText = if (uiModel.reachReferenceLengthCm != null) {
        stringResourceByName(
            "hold_editor_reach_status_configured",
            uiModel.reachReferenceLengthCm
        )
    } else {
        stringResourceByName("hold_editor_reach_status_unset")
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            BottomActionBar {
                if (isEditingExistingWall) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppOutlinedButton(
                            onClick = onBackToList,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.exit_without_saving))
                        }

                        AppButton(
                            onClick = onSaveWall,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.save_and_exit))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppOutlinedButton(
                            onClick = onBackToList,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.back_to_list))
                        }

                        AppButton(
                            onClick = onSaveWall,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(uiModel.saveButtonTextResId))
                        }
                    }

                    AppButton(
                        onClick = onOpenHoldScoring,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResourceByName("open_hold_scoring"))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

                    OutlinedTextField(
                        value = state.wallTitle,
                        onValueChange = onWallTitleChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.wall_title_label)) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                        .clipToBounds()
                ) {
                    if (bitmap != null) {
                        HoldCanvasScreen(
                            bitmap = bitmap,
                            holds = state.holds,
                            selectedIndex = state.selectedHoldIndex,
                            challengeHoldIndices = emptySet(),
                            startHoldIndex = null,
                            goalHoldIndex = null,
                            routeSelectionMode = RouteSelectionMode.NONE,
                            reachCalibrationReference = null,
                            pendingReachCalibrationPoint = null,
                            isReachCalibrationSelectionMode = false,
                            holdTapAreaSize = state.holdTapAreaSize,
                            onHoldTapped = onEditorHoldTapped,
                            onReachCalibrationPointSelected = {},
                            onManualHoldCreated = onManualHoldCreated,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.hold_editor_help),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.hold_count_label, state.holds.size),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.hold_tap_size_label),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HoldTapAreaSize.values().forEach { size ->
                        val buttonText = when (size) {
                            HoldTapAreaSize.SMALL -> stringResource(R.string.hold_tap_size_small)
                            HoldTapAreaSize.MEDIUM -> stringResource(R.string.hold_tap_size_medium)
                            HoldTapAreaSize.LARGE -> stringResource(R.string.hold_tap_size_large)
                        }
                        val buttonModifier = Modifier.weight(1f)

                        if (size == state.holdTapAreaSize) {
                            AppButton(
                                onClick = { onHoldTapAreaSizeChange(size) },
                                modifier = buttonModifier
                            ) {
                                Text(buttonText)
                            }
                        } else {
                            AppOutlinedButton(
                                onClick = { onHoldTapAreaSizeChange(size) },
                                modifier = buttonModifier
                            ) {
                                Text(buttonText)
                            }
                        }
                    }
                }

                Text(
                    text = reachStatusText,
                    color = AppSecondaryTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AppButton(
                    onClick = onDeleteSelectedHold,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.delete_selected),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
