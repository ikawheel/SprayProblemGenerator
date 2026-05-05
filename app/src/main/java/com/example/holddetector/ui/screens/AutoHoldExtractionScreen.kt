package com.example.holddetector.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.domain.hold.AutoExtractionTuning
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.DisplayColorSettings
import com.example.holddetector.ui.canvas.AutoExtractionCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppContentDialog
import com.example.holddetector.ui.components.AppOutlinedButton
import kotlin.math.roundToInt

@Composable
fun AutoHoldExtractionScreen(
    bitmap: Bitmap?,
    extractedHolds: List<Hold>,
    tuning: AutoExtractionTuning,
    selectedHoldIndex: Int?,
    wallSamplePoints: List<HoldPoint>,
    isWallSamplingMode: Boolean,
    displayColorSettings: DisplayColorSettings,
    onHoldTapped: (Int?) -> Unit,
    onStartWallSampling: () -> Unit,
    onStopWallSampling: () -> Unit,
    onWallSamplePointSelected: (HoldPoint) -> Unit,
    onClearWallSamplePoints: () -> Unit,
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

    if (isTuningDialogOpen) {
        AppContentDialog(
            title = stringResource(R.string.auto_hold_extraction_tuning_title),
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (extractedHolds.isEmpty()) {
                stringResource(R.string.auto_hold_extraction_empty)
            } else {
                stringResource(R.string.auto_hold_extraction_count, extractedHolds.size)
            },
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
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
                AutoExtractionCanvasScreen(
                    bitmap = bitmap,
                    holds = extractedHolds,
                    selectedIndex = selectedHoldIndex,
                    wallSamplePoints = wallSamplePoints,
                    isWallSamplingMode = isWallSamplingMode,
                    displayColorSettings = displayColorSettings,
                    onHoldTapped = onHoldTapped,
                    onWallSamplePointSelected = onWallSamplePointSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            color = AppSurfaceColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.auto_hold_extraction_wall_sample_count,
                        wallSamplePoints.size
                    ),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium
                )

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

                AppOutlinedButton(
                    onClick = onClearWallSamplePoints,
                    enabled = wallSamplePoints.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.auto_hold_extraction_wall_sample_clear))
                }

                AppButton(
                    onClick = { isTuningDialogOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.auto_hold_extraction_tuning_open))
                }

                AppButton(
                    onClick = onApplyExtraction,
                    enabled = extractedHolds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(stringResource(R.string.auto_hold_extraction_apply))
                }
            }
        }
    }
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

    Slider(
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

    Slider(
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

    Slider(
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

    Slider(
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
