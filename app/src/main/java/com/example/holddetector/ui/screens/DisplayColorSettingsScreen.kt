package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.DisplayColorSettings
import com.example.holddetector.ui.DisplayColorTarget
import com.example.holddetector.ui.EditableRgbColor
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.BottomActionBar
import kotlin.math.roundToInt

@Composable
fun DisplayColorSettingsScreen(
    settings: DisplayColorSettings,
    onUpdateColor: (DisplayColorTarget, EditableRgbColor) -> Unit,
    onUpdateStrokeWidth: (DisplayColorTarget, Int) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val footerOverlayPadding = 132.dp

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DisplayColorSection(
                    title = stringResource(R.string.display_color_hold_outline_label),
                    description = stringResource(R.string.display_color_hold_outline_description),
                    color = settings.holdOutline,
                    strokeWidth = settings.normalizedHoldOutlineStrokeWidth,
                    onColorChange = { onUpdateColor(DisplayColorTarget.HOLD_OUTLINE, it) },
                    onStrokeWidthChange = { onUpdateStrokeWidth(DisplayColorTarget.HOLD_OUTLINE, it) }
                )

                DisplayColorSection(
                    title = stringResource(R.string.display_color_selected_hold_label),
                    description = stringResource(R.string.display_color_selected_hold_description),
                    color = settings.selectedHold,
                    strokeWidth = settings.normalizedSelectedHoldStrokeWidth,
                    onColorChange = { onUpdateColor(DisplayColorTarget.SELECTED_HOLD, it) },
                    onStrokeWidthChange = { onUpdateStrokeWidth(DisplayColorTarget.SELECTED_HOLD, it) }
                )

                DisplayColorSection(
                    title = stringResource(R.string.display_color_range_selection_label),
                    description = stringResource(R.string.display_color_range_selection_description),
                    color = settings.rangeSelection,
                    strokeWidth = settings.normalizedRangeSelectionStrokeWidth,
                    onColorChange = { onUpdateColor(DisplayColorTarget.RANGE_SELECTION, it) },
                    onStrokeWidthChange = { onUpdateStrokeWidth(DisplayColorTarget.RANGE_SELECTION, it) }
                )

                DisplayColorSection(
                    title = stringResource(R.string.display_color_start_goal_hold_label),
                    description = stringResource(R.string.display_color_start_goal_hold_description),
                    color = settings.startGoalHold,
                    strokeWidth = settings.normalizedStartGoalHoldStrokeWidth,
                    onColorChange = { onUpdateColor(DisplayColorTarget.START_GOAL_HOLD, it) },
                    onStrokeWidthChange = { onUpdateStrokeWidth(DisplayColorTarget.START_GOAL_HOLD, it) }
                )

                Spacer(modifier = Modifier.height(footerOverlayPadding))
            }

            BottomActionBar(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AppOutlinedButton(
                    onClick = onResetToDefaults,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.display_color_reset_defaults))
                }
            }
        }
    }
}

@Composable
private fun DisplayColorSection(
    title: String,
    description: String,
    color: EditableRgbColor,
    strokeWidth: Int,
    onColorChange: (EditableRgbColor) -> Unit,
    onStrokeWidthChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.toComposeColor(), RoundedCornerShape(12.dp))
                        .border(
                            width = strokeWidth.dp,
                            color = AppSecondaryTextColor.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        color = AppTextColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            ColorPalettePicker(
                selectedColor = color,
                onColorSelected = onColorChange
            )

            StrokeWidthSlider(
                strokeWidth = strokeWidth,
                onStrokeWidthChange = onStrokeWidthChange
            )
        }
    }
}

@Composable
private fun StrokeWidthSlider(
    strokeWidth: Int,
    onStrokeWidthChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSubtleSurfaceColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "線の太さ ${strokeWidth}px",
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = strokeWidth.toFloat(),
            onValueChange = { onStrokeWidthChange(it.roundToInt().coerceIn(1, 5)) },
            valueRange = 1f..5f,
            steps = 3
        )
    }
}

@Composable
private fun ColorPalettePicker(
    selectedColor: EditableRgbColor,
    onColorSelected: (EditableRgbColor) -> Unit
) {
    val paletteRows = rememberDisplayColorPaletteRows()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSubtleSurfaceColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        paletteRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { paletteColor ->
                    val isSelected = paletteColor == selectedColor
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RectangleShape)
                            .background(
                                color = paletteColor.toComposeColor(),
                                shape = RectangleShape
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) {
                                    AppTextColor
                                } else {
                                    AppSecondaryTextColor.copy(alpha = 0.28f)
                                },
                                shape = RectangleShape
                            )
                            .clickable { onColorSelected(paletteColor) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .padding(horizontal = 4.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.92f),
                                        shape = RectangleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun rememberDisplayColorPaletteRows(): List<List<EditableRgbColor>> {
    return listOf(
        listOf(
            editableRgbColor(0xFFDC2626),
            editableRgbColor(0xFFEA580C),
            editableRgbColor(0xFFD97706),
            editableRgbColor(0xFFCA8A04),
            editableRgbColor(0xFF65A30D),
            editableRgbColor(0xFF16A34A),
            editableRgbColor(0xFF0F766E),
            editableRgbColor(0xFF0891B2),
            editableRgbColor(0xFF2563EB),
            editableRgbColor(0xFF7C3AED),
            editableRgbColor(0xFFC026D3),
            editableRgbColor(0xFFDB2777)
        ),
        listOf(
            editableRgbColor(0xFFEF4444),
            editableRgbColor(0xFFF97316),
            editableRgbColor(0xFFF59E0B),
            editableRgbColor(0xFFEAB308),
            editableRgbColor(0xFF84CC16),
            editableRgbColor(0xFF22C55E),
            editableRgbColor(0xFF14B8A6),
            editableRgbColor(0xFF06B6D4),
            editableRgbColor(0xFF3B82F6),
            editableRgbColor(0xFF8B5CF6),
            editableRgbColor(0xFFD946EF),
            editableRgbColor(0xFFEC4899)
        ),
        listOf(
            editableRgbColor(0xFFFCA5A5),
            editableRgbColor(0xFFFDBA74),
            editableRgbColor(0xFFFCD34D),
            editableRgbColor(0xFFFEF08A),
            editableRgbColor(0xFFBEF264),
            editableRgbColor(0xFF86EFAC),
            editableRgbColor(0xFF99F6E4),
            editableRgbColor(0xFFA5F3FC),
            editableRgbColor(0xFF93C5FD),
            editableRgbColor(0xFFC4B5FD),
            editableRgbColor(0xFFF0ABFC),
            editableRgbColor(0xFFF9A8D4)
        )
    )
}

private fun editableRgbColor(hex: Long): EditableRgbColor {
    return EditableRgbColor(
        red = ((hex shr 16) and 0xFF).toInt(),
        green = ((hex shr 8) and 0xFF).toInt(),
        blue = (hex and 0xFF).toInt()
    )
}
