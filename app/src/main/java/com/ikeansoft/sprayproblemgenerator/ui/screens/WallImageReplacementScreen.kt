package com.ikeansoft.sprayproblemgenerator.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.DisplayColorSettings
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppOutlinedButton
import com.ikeansoft.sprayproblemgenerator.ui.components.WallRegistrationStepScaffold
import kotlin.math.max
import kotlin.math.roundToInt

private data class ReplacementImagePlacement(
    val offset: Offset,
    val scale: Float
)

private data class ReplacementImageDrawRect(
    val offset: Offset,
    val size: Size
)

@Composable
fun WallImageReplacementScreen(
    originalBitmap: Bitmap?,
    replacementBitmap: Bitmap?,
    holds: List<Hold>,
    displayColorSettings: DisplayColorSettings,
    onSaveReplacementImage: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val imageAspectRatio = originalBitmap?.takeIf { it.height > 0 }?.let {
        it.width.toFloat() / it.height.toFloat()
    }
    val replacementImage = remember(replacementBitmap) {
        replacementBitmap?.asImageBitmap()
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var imageScale by remember { mutableFloatStateOf(1f) }
    var imageOffset by remember { mutableStateOf(Offset.Zero) }
    var hasInitializedPlacement by remember(originalBitmap, replacementBitmap) {
        mutableStateOf(false)
    }

    fun resetPlacement() {
        val newBitmap = replacementBitmap ?: return
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return

        imageScale = 1f
        imageOffset = centeredReplacementOffset(
            canvasSize = canvasSize,
            drawSize = calculateCoverDrawSize(canvasSize, newBitmap)
        )
        hasInitializedPlacement = true
    }

    LaunchedEffect(canvasSize, replacementBitmap) {
        if (!hasInitializedPlacement) {
            resetPlacement()
        }
    }

    WallRegistrationStepScaffold(
        modifier = modifier,
        headerText = stringResource(R.string.wall_image_replacement_title),
        imageAspectRatio = imageAspectRatio,
        imageContent = {
            if (originalBitmap != null && replacementBitmap != null && replacementImage != null) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(replacementBitmap, canvasSize) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                if (canvasSize.width > 0 && canvasSize.height > 0) {
                                    val oldScale = imageScale
                                    val newScale = (oldScale * zoom).coerceIn(0.35f, 12f)
                                    val zoomFactor = newScale / oldScale
                                    imageOffset = centroid - (centroid - imageOffset) * zoomFactor + pan
                                    imageScale = newScale
                                }
                            }
                        }
                ) {
                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                        val drawRect = calculateReplacementDrawRect(
                            canvasSize = canvasSize,
                            bitmap = replacementBitmap,
                            scale = imageScale,
                            offset = imageOffset
                        )

                        drawImage(
                            image = replacementImage,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(replacementBitmap.width, replacementBitmap.height),
                            dstOffset = IntOffset(
                                drawRect.offset.x.roundToInt(),
                                drawRect.offset.y.roundToInt()
                            ),
                            dstSize = IntSize(
                                drawRect.size.width.roundToInt().coerceAtLeast(1),
                                drawRect.size.height.roundToInt().coerceAtLeast(1)
                            )
                        )

                        holds.forEach { hold ->
                            drawPath(
                                path = hold.toCanvasPath(
                                    canvasWidth = size.width,
                                    canvasHeight = size.height,
                                    imageWidth = originalBitmap.width,
                                    imageHeight = originalBitmap.height
                                ),
                                color = displayColorSettings.holdOutlineColor,
                                style = Stroke(
                                    width = displayColorSettings
                                        .normalizedHoldOutlineStrokeWidth
                                        .toFloat()
                                )
                            )
                        }
                    }
                }
            }
        },
        bodyContent = {
            Text(
                text = stringResource(R.string.wall_image_replacement_description),
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        footerContent = {
            AppOutlinedButton(
                onClick = { resetPlacement() },
                enabled = replacementBitmap != null && canvasSize.width > 0 && canvasSize.height > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wall_image_replacement_reset))
            }
            Spacer(modifier = Modifier.height(8.dp))
            AppButton(
                onClick = {
                    val original = originalBitmap
                    val replacement = replacementBitmap
                    if (original != null && replacement != null) {
                        renderReplacementBitmap(
                            originalBitmap = original,
                            replacementBitmap = replacement,
                            canvasSize = canvasSize,
                            placement = ReplacementImagePlacement(
                                offset = imageOffset,
                                scale = imageScale
                            )
                        )?.let { renderedBitmap ->
                            onSaveReplacementImage(renderedBitmap)
                        }
                    }
                },
                enabled = originalBitmap != null &&
                    replacementBitmap != null &&
                    canvasSize.width > 0 &&
                    canvasSize.height > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.wall_image_replacement_save),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

private fun calculateCoverDrawSize(
    canvasSize: IntSize,
    bitmap: Bitmap
): Size {
    val scale = max(
        canvasSize.width.toFloat() / bitmap.width.toFloat(),
        canvasSize.height.toFloat() / bitmap.height.toFloat()
    )
    return Size(
        width = bitmap.width * scale,
        height = bitmap.height * scale
    )
}

private fun centeredReplacementOffset(
    canvasSize: IntSize,
    drawSize: Size
): Offset {
    return Offset(
        x = (canvasSize.width - drawSize.width) / 2f,
        y = (canvasSize.height - drawSize.height) / 2f
    )
}

private fun calculateReplacementDrawRect(
    canvasSize: IntSize,
    bitmap: Bitmap,
    scale: Float,
    offset: Offset
): ReplacementImageDrawRect {
    val coverDrawSize = calculateCoverDrawSize(canvasSize, bitmap)
    return ReplacementImageDrawRect(
        offset = offset,
        size = Size(
            width = coverDrawSize.width * scale,
            height = coverDrawSize.height * scale
        )
    )
}

private fun renderReplacementBitmap(
    originalBitmap: Bitmap,
    replacementBitmap: Bitmap,
    canvasSize: IntSize,
    placement: ReplacementImagePlacement
): Bitmap? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null

    val outputBitmap = Bitmap.createBitmap(
        originalBitmap.width,
        originalBitmap.height,
        Bitmap.Config.ARGB_8888
    )
    val drawRect = calculateReplacementDrawRect(
        canvasSize = canvasSize,
        bitmap = replacementBitmap,
        scale = placement.scale,
        offset = placement.offset
    )
    val scaleX = originalBitmap.width.toFloat() / canvasSize.width.toFloat()
    val scaleY = originalBitmap.height.toFloat() / canvasSize.height.toFloat()
    val destination = RectF(
        drawRect.offset.x * scaleX,
        drawRect.offset.y * scaleY,
        (drawRect.offset.x + drawRect.size.width) * scaleX,
        (drawRect.offset.y + drawRect.size.height) * scaleY
    )

    AndroidCanvas(outputBitmap).apply {
        drawColor(AndroidColor.WHITE)
        drawBitmap(
            replacementBitmap,
            null,
            destination,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
    }
    return outputBitmap
}

private fun Hold.toCanvasPath(
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Int,
    imageHeight: Int
): Path {
    return Path().apply {
        points.forEachIndexed { index, point ->
            val x = point.x.toFloat() / imageWidth.toFloat() * canvasWidth
            val y = point.y.toFloat() / imageHeight.toFloat() * canvasHeight
            if (index == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }
}
