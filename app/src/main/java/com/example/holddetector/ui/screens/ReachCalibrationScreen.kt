package com.example.holddetector.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.example.holddetector.ui.components.WallRegistrationStepScaffold

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
    val canResetReference = state.reachCalibrationReference != null &&
        !state.isReachCalibrationSelectionMode
    val isReturningToHoldEditor = state.reachCalibrationReturnToHoldEditor
    val statusText = when {
        state.isReachCalibrationSelectionMode ->
            null
        state.reachCalibrationReference != null ->
            null
        else ->
            stringResourceByName("reach_calibration_unset")
    }
    val selectButtonText = stringResourceByName("reach_calibration_reset")
    val descriptionText = stringResourceByName("reach_calibration_description")
    val imageAspectRatio = bitmap?.takeIf { it.height > 0 }?.let {
        wallImageDisplayAspectRatio(
            imageWidth = it.width,
            imageHeight = it.height
        )
    }
    WallRegistrationStepScaffold(
        modifier = modifier,
        headerText = stringResource(R.string.registration_step_reach_calibration_title),
        imageAspectRatio = imageAspectRatio,
        useFullImageViewport = true,
        applyImePadding = true,
        imageContent = {
            if (bitmap != null) {
                ReachCalibrationCanvasScreen(
                    bitmap = bitmap,
                    reachCalibrationReference = state.reachCalibrationReference,
                    pendingReachCalibrationPoint = state.pendingReachCalibrationPoint,
                    isReachCalibrationSelectionMode = state.isReachCalibrationSelectionMode,
                    displayColorSettings = state.displayColorSettings,
                    onReachCalibrationPointSelected = onReachCalibrationPointSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        bodyContent = {
            Text(
                text = descriptionText,
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall
            )

            if (statusText != null) {
                Text(
                    text = statusText,
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            OutlinedTextField(
                value = referenceLengthText,
                onValueChange = onReachCalibrationLengthInputChange,
                label = { Text(stringResourceByName("reach_calibration_length_label")) },
                placeholder = { Text(DEFAULT_REACH_REFERENCE_LENGTH_CM.toString()) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            AppButton(
                onClick = onStartReachCalibrationSelection,
                enabled = canResetReference,
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
                    onClick = onContinueToHoldEditor,
                    enabled = canContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResourceByName("reach_calibration_continue"))
                }
            }
        }
    )
}
