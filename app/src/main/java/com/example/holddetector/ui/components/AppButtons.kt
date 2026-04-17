package com.example.holddetector.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppTextColor

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
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.96f else 1f,
        label = "appButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSecondaryTextColor.copy(alpha = 0.28f)
            isPressed -> Color(0xFF111827)
            else -> AppTextColor
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
            contentColor = Color.White,
            disabledContainerColor = AppSecondaryTextColor.copy(alpha = 0.28f),
            disabledContentColor = Color.White.copy(alpha = 0.72f)
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
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.97f else 1f,
        label = "appOutlinedButtonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSurfaceColor
            isPressed -> AppTextColor.copy(alpha = 0.14f)
            else -> AppSurfaceColor
        },
        label = "appOutlinedButtonContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSecondaryTextColor.copy(alpha = 0.5f)
            isPressed -> AppTextColor
            else -> AppTextColor
        },
        label = "appOutlinedButtonContent"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppSecondaryTextColor.copy(alpha = 0.25f)
            isPressed -> AppTextColor
            else -> AppSecondaryTextColor.copy(alpha = 0.55f)
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
