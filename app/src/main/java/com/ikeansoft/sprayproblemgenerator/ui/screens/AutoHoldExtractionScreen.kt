package com.ikeansoft.sprayproblemgenerator.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.domain.hold.AutoExtractionTuning
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.model.HoldPoint
import com.ikeansoft.sprayproblemgenerator.ui.AppSecondaryTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AUTO_EXTRACTION_WALL_SAMPLE_TARGET_COUNT
import com.ikeansoft.sprayproblemgenerator.ui.DisplayColorSettings
import com.ikeansoft.sprayproblemgenerator.ui.canvas.AutoExtractionCanvasScreen
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppContentDialog
import com.ikeansoft.sprayproblemgenerator.ui.components.CompactSlider
import com.ikeansoft.sprayproblemgenerator.ui.components.WallRegistrationStepScaffold
import kotlin.math.roundToInt

@Composable
fun AutoHoldExtractionScreen(
    bitmap: Bitmap?,
    extractedHolds: List<Hold>,
    tuning: AutoExtractionTuning,
    selectedHoldIndex: Int?,
    wallSamplePoints: List<HoldPoint>,
    isWallSamplingMode: Boolean,
    isBusy: Boolean,
    displayColorSettings: DisplayColorSettings,
    onHoldTapped: (Int?) -> Unit,
    onStartWallSampling: () -> Unit,
    onStopWallSampling: () -> Unit,
    onWallSamplePointSelected: (HoldPoint) -> Unit,
    onTuningChange: (AutoExtractionTuning) -> Unit,
    onApplyExtraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var localHueTolerance by rememberSaveable(tuning.hueTolerance) {
        mutableFloatStateOf(tuning.hueTolerance)
    }
    var localValueTolerance by rememberSaveable(tuning.valueTolerance) {
        mutableFloatStateOf(tuning.valueTolerance)
    }
    var localSaturationMin by rememberSaveable(tuning.saturationMin) {
        mutableFloatStateOf(tuning.saturationMin)
    }
    var localBackgroundDistanceThreshold by rememberSaveable(tuning.backgroundDistanceThreshold) {
        mutableFloatStateOf(tuning.backgroundDistanceThreshold)
    }
    var isTuningDialogOpen by rememberSaveable { mutableStateOf(false) }

    fun currentLocalTuning(): AutoExtractionTuning {
        return AutoExtractionTuning(
            hueTolerance = localHueTolerance,
            valueTolerance = localValueTolerance,
            saturationMin = localSaturationMin,
            backgroundDistanceThreshold = localBackgroundDistanceThreshold
        )
    }
    val imageAspectRatio = bitmap?.takeIf { it.height > 0 }?.let {
        wallImageDisplayAspectRatio(
            imageWidth = it.width,
            imageHeight = it.height
        )
    }

    if (isTuningDialogOpen) {
        AppContentDialog(
            title = null,
            onDismissRequest = { isTuningDialogOpen = false },
            dismissText = stringResource(R.string.close)
        ) {
            AutoExtractionTuningControls(
                localHueTolerance = localHueTolerance,
                onHueToleranceChange = { localHueTolerance = it },
                localValueTolerance = localValueTolerance,
                onValueToleranceChange = { localValueTolerance = it },
                localSaturationMin = localSaturationMin,
                onSaturationMinChange = { localSaturationMin = it },
                localBackgroundDistanceThreshold = localBackgroundDistanceThreshold,
                onBackgroundDistanceThresholdChange = { localBackgroundDistanceThreshold = it },
                onTuningChange = {
                    onTuningChange(currentLocalTuning())
                }
            )
        }
    }

    WallRegistrationStepScaffold(
        modifier = modifier,
        headerText = stringResource(R.string.registration_step_auto_extraction_title),
        imageAspectRatio = imageAspectRatio,
        useFullImageViewport = true,
        imageContent = {
            if (bitmap != null) {
                AutoExtractionCanvasScreen(
                    bitmap = bitmap,
                    holds = extractedHolds,
                    selectedIndex = null,
                    wallSamplePoints = wallSamplePoints,
                    isWallSamplingMode = isWallSamplingMode,
                    displayColorSettings = displayColorSettings,
                    onHoldTapped = onHoldTapped,
                    onWallSamplePointSelected = onWallSamplePointSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        bodyContent = {
            if (isWallSamplingMode) {
                Text(
                    text = stringResource(
                        R.string.auto_hold_extraction_wall_sample_count,
                        wallSamplePoints.size,
                        AUTO_EXTRACTION_WALL_SAMPLE_TARGET_COUNT
                    ),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = if (isWallSamplingMode) {
                    stringResource(R.string.auto_hold_extraction_wall_sample_mode_active)
                } else {
                    stringResource(R.string.auto_hold_extraction_wall_sample_hint)
                },
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            AppButton(
                onClick = if (isWallSamplingMode) onStopWallSampling else onStartWallSampling,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    if (isWallSamplingMode) {
                        stringResource(R.string.auto_hold_extraction_wall_sample_stop)
                    } else {
                        stringResource(R.string.auto_hold_extraction_wall_sample_start)
                    }
                )
            }

            AppButton(
                onClick = { isTuningDialogOpen = true },
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(stringResource(R.string.auto_hold_extraction_tuning_open))
            }
        },
        footerContent = {
            AppButton(
                onClick = onApplyExtraction,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auto_hold_extraction_apply))
            }
        }
    )
}

@Composable
private fun AutoExtractionTuningControls(
    localHueTolerance: Float,
    onHueToleranceChange: (Float) -> Unit,
    localValueTolerance: Float,
    onValueToleranceChange: (Float) -> Unit,
    localSaturationMin: Float,
    onSaturationMinChange: (Float) -> Unit,
    localBackgroundDistanceThreshold: Float,
    onBackgroundDistanceThresholdChange: (Float) -> Unit,
    onTuningChange: () -> Unit
) {
    Text(
        text = stringResource(
            R.string.auto_hold_extraction_hue_tolerance_value,
            localHueTolerance.roundToInt()
        ),
        color = AppTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 12.dp)
    )

    CompactSlider(
        value = localHueTolerance,
        valueRange = 8f..180f,
        onValueChange = onHueToleranceChange,
        onValueChangeFinished = onTuningChange,
        modifier = Modifier.padding(top = 4.dp)
    )

    Text(
        text = stringResource(R.string.auto_hold_extraction_hue_tolerance_label),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodySmall
    )

    Text(
        text = stringResource(
            R.string.auto_hold_extraction_value_tolerance_value,
            (localValueTolerance * 100f).roundToInt()
        ),
        color = AppTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp)
    )

    CompactSlider(
        value = localValueTolerance,
        valueRange = 0.08f..1f,
        onValueChange = onValueToleranceChange,
        onValueChangeFinished = onTuningChange,
        modifier = Modifier.padding(top = 4.dp)
    )

    Text(
        text = stringResource(R.string.auto_hold_extraction_value_tolerance_label),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodySmall
    )

    Text(
        text = stringResource(
            R.string.auto_hold_extraction_saturation_min_value,
            (localSaturationMin * 100f).roundToInt()
        ),
        color = AppTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp)
    )

    CompactSlider(
        value = localSaturationMin,
        valueRange = 0f..1f,
        onValueChange = onSaturationMinChange,
        onValueChangeFinished = onTuningChange,
        modifier = Modifier.padding(top = 4.dp)
    )

    Text(
        text = stringResource(R.string.auto_hold_extraction_saturation_min_label),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodySmall
    )

    Text(
        text = stringResource(
            R.string.auto_hold_extraction_background_distance_value,
            (localBackgroundDistanceThreshold * 100f).roundToInt()
        ),
        color = AppTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp)
    )

    CompactSlider(
        value = localBackgroundDistanceThreshold,
        valueRange = 0f..1f,
        onValueChange = onBackgroundDistanceThresholdChange,
        onValueChangeFinished = onTuningChange,
        modifier = Modifier.padding(top = 4.dp)
    )

    Text(
        text = stringResource(R.string.auto_hold_extraction_background_distance_label),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodySmall
    )
}
