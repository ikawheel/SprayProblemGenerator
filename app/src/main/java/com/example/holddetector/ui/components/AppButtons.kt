package com.example.holddetector.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor

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
    val accentColor = MaterialTheme.colorScheme.primary
    val accentContentColor = MaterialTheme.colorScheme.onPrimary
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.96f else 1f,
        label = "appButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSecondaryTextColor.copy(alpha = 0.28f)
            isPressed -> accentColor.copy(alpha = 0.86f)
            else -> accentColor
        },
        label = "appButtonContainer"
    )

    MaterialButton(
        onClick = onClick,
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
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val accentColor = MaterialTheme.colorScheme.primary
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.97f else 1f,
        label = "appOutlinedButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSurfaceColor
            isPressed -> accentColor.copy(alpha = 0.14f)
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
        onClick = onClick,
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
