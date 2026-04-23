package com.example.holddetector.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.domain.hold.AutoExtractionTuning
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.canvas.AutoExtractionCanvasScreen
import com.example.holddetector.ui.components.AppButton
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
    onHoldTapped: (Int?) -> Unit,
    onEstimateWallSamplePoints: () -> Unit,
    onStartWallSampling: () -> Unit,
    onStopWallSampling: () -> Unit,
    onWallSamplePointSelected: (HoldPoint) -> Unit,
    onClearWallSamplePoints: () -> Unit,
    onTuningChange: (AutoExtractionTuning) -> Unit,
    onBackToMethodSelection: () -> Unit,
    onApplyExtraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var localMinimumThreshold by rememberSaveable(tuning.minimumThreshold) {
        mutableFloatStateOf(tuning.minimumThreshold)
    }
    var localStandardDeviationMultiplier by rememberSaveable(tuning.standardDeviationMultiplier) {
        mutableFloatStateOf(tuning.standardDeviationMultiplier)
    }
    var localAreaFilterStrength by rememberSaveable(tuning.areaFilterStrength) {
        mutableFloatStateOf(tuning.areaFilterStrength)
    }
    var localSmoothingStrength by rememberSaveable(tuning.smoothingStrength) {
        mutableFloatStateOf(tuning.smoothingStrength)
    }

    fun currentLocalTuning(): AutoExtractionTuning {
        return AutoExtractionTuning(
            minimumThreshold = localMinimumThreshold,
            standardDeviationMultiplier = localStandardDeviationMultiplier,
            areaFilterStrength = localAreaFilterStrength,
            smoothingStrength = localSmoothingStrength
        )
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
                        text = stringResource(R.string.auto_hold_extraction_title),
                        color = AppTextColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.auto_hold_extraction_description),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = if (extractedHolds.isEmpty()) {
                            stringResource(R.string.auto_hold_extraction_empty)
                        } else {
                            stringResource(R.string.auto_hold_extraction_count, extractedHolds.size)
                        },
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Text(
                        text = stringResource(
                            R.string.auto_hold_extraction_wall_sample_count,
                            wallSamplePoints.size
                        ),
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
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
                        onClick = onEstimateWallSamplePoints,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(stringResource(R.string.auto_hold_extraction_wall_sample_estimate))
                    }

                    AppButton(
                        onClick = if (isWallSamplingMode) {
                            onStopWallSampling
                        } else {
                            onStartWallSampling
                        },
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
                }
            }

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
                        onHoldTapped = onHoldTapped,
                        onWallSamplePointSelected = onWallSamplePointSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 20.dp),
                color = AppSurfaceColor,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.auto_hold_extraction_minimum_threshold_value,
                            localMinimumThreshold
                        ),
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Slider(
                        value = localMinimumThreshold,
                        valueRange = 4f..40f,
                        onValueChange = { localMinimumThreshold = it },
                        onValueChangeFinished = {
                            onTuningChange(currentLocalTuning())
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = stringResource(R.string.auto_hold_extraction_minimum_threshold_label),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = stringResource(
                            R.string.auto_hold_extraction_standard_deviation_multiplier_value,
                            localStandardDeviationMultiplier
                        ),
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Slider(
                        value = localStandardDeviationMultiplier,
                        valueRange = 0.4f..3f,
                        onValueChange = { localStandardDeviationMultiplier = it },
                        onValueChangeFinished = {
                            onTuningChange(currentLocalTuning())
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = stringResource(R.string.auto_hold_extraction_standard_deviation_multiplier_label),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = stringResource(
                            R.string.auto_hold_extraction_area_filter_value,
                            (localAreaFilterStrength * 100f).roundToInt()
                        ),
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Slider(
                        value = localAreaFilterStrength,
                        valueRange = 0.25f..3f,
                        onValueChange = { localAreaFilterStrength = it },
                        onValueChangeFinished = {
                            onTuningChange(currentLocalTuning())
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = stringResource(R.string.auto_hold_extraction_area_filter_label),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = stringResource(
                            R.string.auto_hold_extraction_smoothing_value,
                            (localSmoothingStrength * 100f).roundToInt()
                        ),
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Slider(
                        value = localSmoothingStrength,
                        valueRange = 0f..1f,
                        onValueChange = { localSmoothingStrength = it },
                        onValueChangeFinished = {
                            onTuningChange(currentLocalTuning())
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = stringResource(R.string.auto_hold_extraction_smoothing_label),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodySmall
                    )

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
