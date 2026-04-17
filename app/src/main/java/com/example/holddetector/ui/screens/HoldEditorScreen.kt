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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.model.Hold
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.RouteSelectionMode
import com.example.holddetector.ui.canvas.HoldCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.selectors.deriveHoldEditorUiModel

private const val OpenHoldScoringButtonText = "\u70b9\u6570\u4ed8\u3051\u3078"

@Composable
fun HoldEditorScreen(
    state: MainUiState,
    onWallTitleChanged: (String) -> Unit,
    onSaveWall: () -> Unit,
    onOpenHoldScoring: () -> Unit,
    onBackToList: () -> Unit,
    onDeleteSelectedHold: () -> Unit,
    onEditorHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    onOpenReachCalibrationScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveHoldEditorUiModel(state)

    Column(modifier = modifier) {
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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp)
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
            text = uiModel.reachStatusText,
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AppOutlinedButton(
            onClick = onOpenReachCalibrationScreen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = uiModel.reachButtonText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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
                onClick = onDeleteSelectedHold,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.delete_selected),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(OpenHoldScoringButtonText)
        }
    }
}
