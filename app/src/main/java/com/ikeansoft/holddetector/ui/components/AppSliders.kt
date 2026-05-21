package com.ikeansoft.holddetector.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlin.ranges.ClosedFloatingPointRange

@Composable
fun CompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleX = 1f, scaleY = 0.75f),
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished
            )
        }
    }
}

@Composable
fun CompactRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            RangeSlider(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleX = 1f, scaleY = 0.75f),
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished
            )
        }
    }
}
