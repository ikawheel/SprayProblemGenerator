package com.example.holddetector.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.domain.hold.AutoExtractionTuning
import com.example.holddetector.domain.hold.HoldColorCategory
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppSurfaceColor
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
    selectedColors: Set<HoldColorCategory>,
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
    onToggleColor: (HoldColorCategory) -> Unit,
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

    fun currentLocalTuning(): AutoExtractionTuning {
        return AutoExtractionTuning(
            hueTolerance = localHueTolerance,
            valueTolerance = localValueTolerance,
            saturationMin = localSaturationMin,
            backgroundDistanceThreshold = localBackgroundDistanceThreshold
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
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            color = AppSurfaceColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.auto_hold_extraction_target_colors_title),
                    color = AppTextColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = stringResource(R.string.auto_hold_extraction_target_colors_description),
                    color = AppSecondaryTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )

                HoldColorCategory.values().toList().chunked(5).forEachIndexed { rowIndex, rowCategories ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (rowIndex == 0) 12.dp else 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { category ->
                            HoldColorToggleChip(
                                category = category,
                                isSelected = category in selectedColors,
                                onClick = { onToggleColor(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(
                        R.string.auto_hold_extraction_hue_tolerance_value,
                        localHueTolerance.roundToInt()
                    ),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 20.dp)
                )

                Slider(
                    value = localHueTolerance,
                    valueRange = 8f..180f,
                    onValueChange = { localHueTolerance = it },
                    onValueChangeFinished = {
                        onTuningChange(currentLocalTuning())
                    },
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
                    onValueChange = { localValueTolerance = it },
                    onValueChangeFinished = {
                        onTuningChange(currentLocalTuning())
                    },
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
                    onValueChange = { localSaturationMin = it },
                    onValueChangeFinished = {
                        onTuningChange(currentLocalTuning())
                    },
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
                    onValueChange = { localBackgroundDistanceThreshold = it },
                    onValueChangeFinished = {
                        onTuningChange(currentLocalTuning())
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = stringResource(R.string.auto_hold_extraction_background_distance_label),
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

@Composable
private fun HoldColorToggleChip(
    category: HoldColorCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColor = holdColorChipColor(category)
    val containerColor = if (isSelected) chipColor else Color.Transparent
    val borderColor = if (isSelected) chipColor else chipColor.copy(alpha = 0.75f)
    val textColor = if (isSelected) holdColorChipTextColor(category) else AppTextColor

    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(holdColorCategoryLabelRes(category)),
                color = textColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun holdColorCategoryLabelRes(category: HoldColorCategory): Int {
    return when (category) {
        HoldColorCategory.WHITE -> R.string.auto_hold_color_white
        HoldColorCategory.BLACK -> R.string.auto_hold_color_black
        HoldColorCategory.ORANGE -> R.string.auto_hold_color_orange
        HoldColorCategory.RED -> R.string.auto_hold_color_red
        HoldColorCategory.PURPLE -> R.string.auto_hold_color_purple
        HoldColorCategory.BLUE -> R.string.auto_hold_color_blue
        HoldColorCategory.CYAN -> R.string.auto_hold_color_cyan
        HoldColorCategory.YELLOW -> R.string.auto_hold_color_yellow
        HoldColorCategory.GREEN -> R.string.auto_hold_color_green
        HoldColorCategory.LIME -> R.string.auto_hold_color_lime
    }
}

private fun holdColorChipColor(category: HoldColorCategory): Color {
    return when (category) {
        HoldColorCategory.WHITE -> Color(0xFFF3F4F6)
        HoldColorCategory.BLACK -> Color(0xFF111827)
        HoldColorCategory.ORANGE -> Color(0xFFF97316)
        HoldColorCategory.RED -> Color(0xFFDC2626)
        HoldColorCategory.PURPLE -> Color(0xFF7C3AED)
        HoldColorCategory.BLUE -> Color(0xFF2563EB)
        HoldColorCategory.CYAN -> Color(0xFF06B6D4)
        HoldColorCategory.YELLOW -> Color(0xFFFACC15)
        HoldColorCategory.GREEN -> Color(0xFF16A34A)
        HoldColorCategory.LIME -> Color(0xFF84CC16)
    }
}

private fun holdColorChipTextColor(category: HoldColorCategory): Color {
    return when (category) {
        HoldColorCategory.WHITE,
        HoldColorCategory.YELLOW,
        HoldColorCategory.LIME -> AppTextColor
        else -> Color.White
    }
}
