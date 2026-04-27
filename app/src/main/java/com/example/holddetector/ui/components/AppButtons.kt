package com.example.holddetector.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun rememberAppButtonTapFlash(
    enabled: Boolean,
    onClick: () -> Unit
): Pair<Boolean, () -> Unit> {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    var showTapFlash by remember { mutableStateOf(false) }
    var isHandlingTap by remember { mutableStateOf(false) }

    val wrappedOnClick: () -> Unit = click@{
        if (!enabled || isHandlingTap) return@click
        isHandlingTap = true
        showTapFlash = true
        scope.launch {
            try {
                delay(60)
                currentOnClick()
            } finally {
                showTapFlash = false
                isHandlingTap = false
            }
        }
    }

    return showTapFlash to wrappedOnClick
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (showTapFlash, wrappedOnClick) = rememberAppButtonTapFlash(
        enabled = enabled,
        onClick = onClick
    )
    val accentColor = MaterialTheme.colorScheme.primary
    val accentContentColor = MaterialTheme.colorScheme.onPrimary
    val scale by animateFloatAsState(
        targetValue = if (enabled && (isPressed || showTapFlash)) 0.96f else 1f,
        label = "appButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSecondaryTextColor.copy(alpha = 0.28f)
            isPressed || showTapFlash -> Color.White.copy(alpha = 0.22f).compositeOver(accentColor)
            else -> accentColor
        },
        label = "appButtonContainer"
    )

    MaterialButton(
        onClick = wrappedOnClick,
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = accentContentColor,
            disabledContainerColor = AppSecondaryTextColor.copy(alpha = 0.28f),
            disabledContentColor = accentContentColor.copy(alpha = 0.72f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = 48.dp)
    ) {
        content()
    }
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (showTapFlash, wrappedOnClick) = rememberAppButtonTapFlash(
        enabled = enabled,
        onClick = onClick
    )
    val scale by animateFloatAsState(
        targetValue = if (enabled && (isPressed || showTapFlash)) 0.94f else 1f,
        label = "appIconButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed || showTapFlash -> AppSubtleSurfaceColor
            else -> Color.Transparent
        },
        label = "appIconButtonContainer"
    )

    MaterialIconButton(
        onClick = wrappedOnClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(containerColor, shape)
    ) {
        content()
    }
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (showTapFlash, wrappedOnClick) = rememberAppButtonTapFlash(
        enabled = enabled,
        onClick = onClick
    )
    val accentColor = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (enabled && (isPressed || showTapFlash)) 0.97f else 1f,
        label = "appOutlinedButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSurfaceColor
            isPressed || showTapFlash -> accentColor.copy(alpha = 0.14f)
            else -> AppSurfaceColor
        },
        label = "appOutlinedButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSecondaryTextColor.copy(alpha = 0.5f)
            isPressed -> accentColor
            else -> accentColor
        },
        label = "appOutlinedButtonContent"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSecondaryTextColor.copy(alpha = 0.25f)
            isPressed -> accentColor
            else -> accentColor.copy(alpha = 0.62f)
        },
        label = "appOutlinedButtonBorder"
    )

    MaterialOutlinedButton(
        onClick = wrappedOnClick,
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AppSurfaceColor,
            disabledContentColor = AppSecondaryTextColor.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = 48.dp)
    ) {
        content()
    }
}
