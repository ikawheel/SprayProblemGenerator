package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

@Composable
fun DisplayColorSettingsScreen(
    settings: DisplayColorSettings,
    onUpdateColor: (DisplayColorTarget, EditableRgbColor) -> Unit,
    onResetToDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.display_color_settings_title),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.display_color_settings_description),
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium
        )

        DisplayColorSection(
            title = stringResource(R.string.display_color_hold_outline_label),
            description = stringResource(R.string.display_color_hold_outline_description),
            color = settings.holdOutline,
            onColorChange = { onUpdateColor(DisplayColorTarget.HOLD_OUTLINE, it) }
        )

        DisplayColorSection(
            title = stringResource(R.string.display_color_selected_hold_label),
            description = stringResource(R.string.display_color_selected_hold_description),
            color = settings.selectedHold,
            onColorChange = { onUpdateColor(DisplayColorTarget.SELECTED_HOLD, it) }
        )

        DisplayColorSection(
            title = stringResource(R.string.display_color_range_selection_label),
            description = stringResource(R.string.display_color_range_selection_description),
            color = settings.rangeSelection,
            onColorChange = { onUpdateColor(DisplayColorTarget.RANGE_SELECTION, it) }
        )

        AppOutlinedButton(
            onClick = onResetToDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.display_color_reset_defaults))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DisplayColorSection(
    title: String,
    description: String,
    color: EditableRgbColor,
    onColorChange: (EditableRgbColor) -> Unit
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
                            width = 1.dp,
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

            ColorChannelSlider(
                label = stringResource(R.string.display_color_channel_red),
                value = color.normalizedRed,
                activeColor = Color.Red,
                onValueChange = { updated ->
                    onColorChange(
                        color.copy(red = updated)
                    )
                }
            )

            ColorChannelSlider(
                label = stringResource(R.string.display_color_channel_green),
                value = color.normalizedGreen,
                activeColor = Color(0xFF16A34A),
                onValueChange = { updated ->
                    onColorChange(
                        color.copy(green = updated)
                    )
                }
            )

            ColorChannelSlider(
                label = stringResource(R.string.display_color_channel_blue),
                value = color.normalizedBlue,
                activeColor = Color(0xFF2563EB),
                onValueChange = { updated ->
                    onColorChange(
                        color.copy(blue = updated)
                    )
                }
            )
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    activeColor: Color,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSubtleSurfaceColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value.toString(),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            steps = 254,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = activeColor.copy(alpha = 0.22f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(activeColor.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
        )
    }
}
