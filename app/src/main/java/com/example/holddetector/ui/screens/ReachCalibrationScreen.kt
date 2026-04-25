package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.example.holddetector.R
import com.example.holddetector.model.DEFAULT_REACH_REFERENCE_LENGTH_CM
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.stringResourceByName
import com.example.holddetector.ui.canvas.ReachCalibrationCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.model.HoldPoint

@Composable
fun ReachCalibrationScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onSaveAndExit: () -> Unit,
    onStartReachCalibrationSelection: () -> Unit,
    onReachCalibrationLengthInputChange: (String) -> Unit,
    onClearReachCalibration: () -> Unit,
    onReachCalibrationPointSelected: (HoldPoint) -> Unit,
    onContinueToHoldEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val isEditingExistingWall = state.currentWallId != null
    val referenceLengthText = state.reachCalibrationLengthInput
    val isReferenceLengthValid = referenceLengthText.toIntOrNull()?.let { it > 0 } == true
    val canContinue = state.reachCalibrationReference != null &&
        isReferenceLengthValid &&
        !state.isReachCalibrationSelectionMode &&
        state.pendingReachCalibrationPoint == null
    val isReturningToHoldEditor = state.reachCalibrationReturnToHoldEditor
    val statusText = when {
        state.isReachCalibrationSelectionMode && state.pendingReachCalibrationPoint == null ->
            stringResourceByName("reach_calibration_first_point")
        state.isReachCalibrationSelectionMode && state.pendingReachCalibrationPoint != null ->
            stringResourceByName("reach_calibration_second_point")
        state.reachCalibrationReference != null ->
            stringResourceByName(
                "reach_calibration_configured",
                state.reachCalibrationReference.referenceLengthCm
            )
        else ->
            stringResourceByName("reach_calibration_unset")
    }
    val selectButtonText = if (state.reachCalibrationReference == null) {
        stringResourceByName("reach_calibration_setup")
    } else {
        stringResourceByName("reach_calibration_reset")
    }
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
                        text = stringResourceByName("reach_calibration_title"),
                        color = AppTextColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResourceByName("reach_calibration_description"),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Text(
                        text = stringResourceByName("reach_calibration_recommendation"),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (bitmap != null && bitmap.height > 0) {
                                Modifier.aspectRatio(
                                    wallImageDisplayAspectRatio(
                                        imageWidth = bitmap.width,
                                        imageHeight = bitmap.height
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                        .clipToBounds()
                ) {
                    if (bitmap != null) {
                        ReachCalibrationCanvasScreen(
                            bitmap = bitmap,
                            reachCalibrationReference = state.reachCalibrationReference,
                            pendingReachCalibrationPoint = state.pendingReachCalibrationPoint,
                            isReachCalibrationSelectionMode = state.isReachCalibrationSelectionMode,
                            onReachCalibrationPointSelected = onReachCalibrationPointSelected,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = statusText,
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = referenceLengthText,
                    onValueChange = onReachCalibrationLengthInputChange,
                    label = { Text(stringResourceByName("reach_calibration_length_label")) },
                    placeholder = { Text(DEFAULT_REACH_REFERENCE_LENGTH_CM.toString()) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isEditingExistingWall) {
                    AppButton(
                        onClick = onStartReachCalibrationSelection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = selectButtonText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    AppButton(
                        onClick = onStartReachCalibrationSelection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = selectButtonText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .navigationBarsPadding()
                ) {
                    if (isEditingExistingWall) {
                        AppButton(
                            onClick = onSaveAndExit,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.overwrite_save))
                        }
                    } else {
                        AppButton(
                            onClick = onContinueToHoldEditor,
                            enabled = canContinue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResourceByName("reach_calibration_continue"))
                        }
                    }
                }
            }
    }
}
