package com.example.holddetector.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import com.example.holddetector.R
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import kotlin.math.abs
import kotlin.math.max

private enum class CropDragTarget {
    NONE,
    MOVE,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

private data class NormalizedCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Composable
fun ImageCropScreen(
    bitmap: Bitmap?,
    message: String?,
    onApplyCropManual: (Float, Float, Float, Float) -> Unit,
    onApplyCropAuto: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var cropLeft by rememberSaveable { mutableStateOf(0.08f) }
    var cropTop by rememberSaveable { mutableStateOf(0.08f) }
    var cropRight by rememberSaveable { mutableStateOf(0.92f) }
    var cropBottom by rememberSaveable { mutableStateOf(0.92f) }
    var showRegistrationMethodDialog by rememberSaveable { mutableStateOf(false) }
    var displayedImageSize by remember { mutableStateOf(IntSize.Zero) }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    fun currentCropRect(): NormalizedCropRect {
        return NormalizedCropRect(
            left = cropLeft,
            top = cropTop,
            right = cropRight,
            bottom = cropBottom
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                val imageAspectRatio = if (bitmap != null && bitmap.height > 0) {
                    bitmap.width.toFloat() / bitmap.height.toFloat()
                } else {
                    1f
                }
                val containerAspectRatio = if (maxHeight.value > 0f) {
                    maxWidth.value / maxHeight.value
                } else {
                    imageAspectRatio
                }
                val imageModifier = if (imageAspectRatio >= containerAspectRatio) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(imageAspectRatio)
                } else {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(imageAspectRatio)
                }

                Box(
                    modifier = imageModifier
                        .align(Alignment.Center)
                        .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                        .clipToBounds()
                ) {
                    if (bitmap != null && imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { displayedImageSize = it }
                                .pointerInput(bitmap, displayedImageSize) {
                                    awaitEachGesture {
                                        var down = awaitPointerEvent().changes.firstOrNull { it.pressed }
                                        while (down == null) {
                                            down = awaitPointerEvent().changes.firstOrNull { it.pressed }
                                        }
                                        if (displayedImageSize.width <= 0 || displayedImageSize.height <= 0) {
                                            return@awaitEachGesture
                                        }

                                        val activeDragTarget = resolveCropDragTarget(
                                            touchOffset = down.position,
                                            imageSize = displayedImageSize,
                                            cropRect = currentCropRect()
                                        )

                                        if (activeDragTarget == CropDragTarget.NONE) {
                                            return@awaitEachGesture
                                        }

                                        down.consume()
                                        drag(down.id) { change ->
                                            val dragAmount = change.positionChange()
                                            if (dragAmount == Offset.Zero) {
                                                return@drag
                                            }

                                            val updatedCropRect = applyCropDrag(
                                                cropRect = currentCropRect(),
                                                dragTarget = activeDragTarget,
                                                deltaX = dragAmount.x / displayedImageSize.width.toFloat(),
                                                deltaY = dragAmount.y / displayedImageSize.height.toFloat()
                                            )

                                            cropLeft = updatedCropRect.left
                                            cropTop = updatedCropRect.top
                                            cropRight = updatedCropRect.right
                                            cropBottom = updatedCropRect.bottom
                                            change.consume()
                                        }
                                    }
                                }
                        )

                        CropOverlay(
                            cropRect = NormalizedCropRect(
                                left = cropLeft,
                                top = cropTop,
                                right = cropRight,
                                bottom = cropBottom
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = AppTextColor,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }

        AppButton(
            onClick = {
                showRegistrationMethodDialog = true
            },
            enabled = bitmap != null,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.image_crop_apply),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (showRegistrationMethodDialog) {
        AlertDialog(
            onDismissRequest = { showRegistrationMethodDialog = false },
            title = {
                Text(text = stringResource(R.string.registration_method_title))
            },
            text = {
                Column {
                    AppButton(
                        onClick = {
                            showRegistrationMethodDialog = false
                            onApplyCropManual(cropLeft, cropTop, cropRight, cropBottom)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(stringResource(R.string.registration_method_manual))
                    }

                    AppButton(
                        onClick = {
                            showRegistrationMethodDialog = false
                            onApplyCropAuto(cropLeft, cropTop, cropRight, cropBottom)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(stringResource(R.string.registration_method_auto))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                AppOutlinedButton(onClick = { showRegistrationMethodDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CropOverlay(
    cropRect: NormalizedCropRect,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val left = cropRect.left * size.width
        val top = cropRect.top * size.height
        val right = cropRect.right * size.width
        val bottom = cropRect.bottom * size.height
        val cropWidth = right - left
        val cropHeight = bottom - top

        drawRect(
            color = Color(0x88000000),
            topLeft = Offset.Zero,
            size = Size(size.width, top)
        )
        drawRect(
            color = Color(0x88000000),
            topLeft = Offset(0f, top),
            size = Size(left, cropHeight)
        )
        drawRect(
            color = Color(0x88000000),
            topLeft = Offset(right, top),
            size = Size(size.width - right, cropHeight)
        )
        drawRect(
            color = Color(0x88000000),
            topLeft = Offset(0f, bottom),
            size = Size(size.width, size.height - bottom)
        )

        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(cropWidth, cropHeight),
            style = Stroke(width = 4f)
        )

        val handleRadius = 10f
        val handleCenters = listOf(
            Offset(left, top),
            Offset(right, top),
            Offset(left, bottom),
            Offset(right, bottom)
        )
        handleCenters.forEach { center ->
            drawCircle(
                color = Color.White,
                radius = handleRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF7C3AED),
                radius = handleRadius,
                center = center,
                style = Stroke(width = 3f)
            )
        }
    }
}

private fun resolveCropDragTarget(
    touchOffset: Offset,
    imageSize: IntSize,
    cropRect: NormalizedCropRect
): CropDragTarget {
    if (imageSize.width <= 0 || imageSize.height <= 0) {
        return CropDragTarget.NONE
    }

    val left = cropRect.left * imageSize.width
    val top = cropRect.top * imageSize.height
    val right = cropRect.right * imageSize.width
    val bottom = cropRect.bottom * imageSize.height
    val edgeThresholdPx = 48f
    val cornerThresholdPx = 64f

    val nearLeft = abs(touchOffset.x - left) <= edgeThresholdPx
    val nearTop = abs(touchOffset.y - top) <= edgeThresholdPx
    val nearRight = abs(touchOffset.x - right) <= edgeThresholdPx
    val nearBottom = abs(touchOffset.y - bottom) <= edgeThresholdPx
    val nearTopLeft =
        abs(touchOffset.x - left) <= cornerThresholdPx &&
            abs(touchOffset.y - top) <= cornerThresholdPx
    val nearTopRight =
        abs(touchOffset.x - right) <= cornerThresholdPx &&
            abs(touchOffset.y - top) <= cornerThresholdPx
    val nearBottomLeft =
        abs(touchOffset.x - left) <= cornerThresholdPx &&
            abs(touchOffset.y - bottom) <= cornerThresholdPx
    val nearBottomRight =
        abs(touchOffset.x - right) <= cornerThresholdPx &&
            abs(touchOffset.y - bottom) <= cornerThresholdPx
    val insideX = touchOffset.x in left..right
    val insideY = touchOffset.y in top..bottom

    return when {
        nearTopLeft -> CropDragTarget.TOP_LEFT
        nearTopRight -> CropDragTarget.TOP_RIGHT
        nearBottomLeft -> CropDragTarget.BOTTOM_LEFT
        nearBottomRight -> CropDragTarget.BOTTOM_RIGHT
        nearLeft && insideY -> CropDragTarget.LEFT
        nearRight && insideY -> CropDragTarget.RIGHT
        nearTop && insideX -> CropDragTarget.TOP
        nearBottom && insideX -> CropDragTarget.BOTTOM
        insideX && insideY -> CropDragTarget.MOVE
        else -> CropDragTarget.NONE
    }
}

private fun applyCropDrag(
    cropRect: NormalizedCropRect,
    dragTarget: CropDragTarget,
    deltaX: Float,
    deltaY: Float
): NormalizedCropRect {
    val minSize = 0.18f
    var left = cropRect.left
    var top = cropRect.top
    var right = cropRect.right
    var bottom = cropRect.bottom

    when (dragTarget) {
        CropDragTarget.NONE -> return cropRect
        CropDragTarget.MOVE -> {
            val width = right - left
            val height = bottom - top
            left = (left + deltaX).coerceIn(0f, 1f - width)
            top = (top + deltaY).coerceIn(0f, 1f - height)
            right = left + width
            bottom = top + height
        }
        CropDragTarget.LEFT -> {
            left = (left + deltaX).coerceIn(0f, right - minSize)
        }
        CropDragTarget.TOP -> {
            top = (top + deltaY).coerceIn(0f, bottom - minSize)
        }
        CropDragTarget.RIGHT -> {
            right = (right + deltaX).coerceIn(left + minSize, 1f)
        }
        CropDragTarget.BOTTOM -> {
            bottom = (bottom + deltaY).coerceIn(top + minSize, 1f)
        }
        CropDragTarget.TOP_LEFT -> {
            left = (left + deltaX).coerceIn(0f, right - minSize)
            top = (top + deltaY).coerceIn(0f, bottom - minSize)
        }
        CropDragTarget.TOP_RIGHT -> {
            right = (right + deltaX).coerceIn(left + minSize, 1f)
            top = (top + deltaY).coerceIn(0f, bottom - minSize)
        }
        CropDragTarget.BOTTOM_LEFT -> {
            left = (left + deltaX).coerceIn(0f, right - minSize)
            bottom = (bottom + deltaY).coerceIn(top + minSize, 1f)
        }
        CropDragTarget.BOTTOM_RIGHT -> {
            right = (right + deltaX).coerceIn(left + minSize, 1f)
            bottom = (bottom + deltaY).coerceIn(top + minSize, 1f)
        }
    }

    return NormalizedCropRect(
        left = left,
        top = top,
        right = max(right, left + minSize),
        bottom = max(bottom, top + minSize)
    )
}
