package com.example.holddetector.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntSize
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class BaseImageLayout(
    val left: Float,
    val top: Float,
    val drawWidth: Float,
    val drawHeight: Float,
    val fitScale: Float
) {
    val isValid: Boolean get() = drawWidth > 0f && drawHeight > 0f && fitScale > 0f
}

internal data class LocalHoldPolygon(
    val points: List<Offset>
) {
    init {
        require(points.size >= 3) { "Polygon requires at least 3 points" }
    }

    val minX: Float get() = points.minOf { it.x }
    val maxX: Float get() = points.maxOf { it.x }
    val minY: Float get() = points.minOf { it.y }
    val maxY: Float get() = points.maxOf { it.y }

    fun toPath(): Path {
        return Path().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 1 until points.size) {
                lineTo(points[index].x, points[index].y)
            }
            close()
        }
    }
}

internal fun calculateBaseImageLayout(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): BaseImageLayout {
    if (containerWidth <= 0f || containerHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return BaseImageLayout(0f, 0f, 0f, 0f, 0f)
    }

    val fitScale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
    val drawWidth = imageWidth * fitScale
    val drawHeight = imageHeight * fitScale

    return BaseImageLayout(
        left = (containerWidth - drawWidth) / 2f,
        top = (containerHeight - drawHeight) / 2f,
        drawWidth = drawWidth,
        drawHeight = drawHeight,
        fitScale = fitScale
    )
}

internal fun clampPanOffset(
    candidate: Offset,
    containerSize: IntSize,
    baseLayout: BaseImageLayout,
    zoomScale: Float
): Offset {
    if (!baseLayout.isValid) return Offset.Zero

    val scaledWidth = baseLayout.drawWidth * zoomScale
    val scaledHeight = baseLayout.drawHeight * zoomScale
    val minX = containerSize.width - baseLayout.left - scaledWidth
    val maxX = -baseLayout.left
    val minY = containerSize.height - baseLayout.top - scaledHeight
    val maxY = -baseLayout.top

    val clampedX = candidate.x.coerceIn(
        minimumValue = minOf(minX, maxX),
        maximumValue = maxOf(minX, maxX)
    )

    val clampedY = candidate.y.coerceIn(
        minimumValue = minOf(minY, maxY),
        maximumValue = maxOf(minY, maxY)
    )

    return Offset(
        x = clampedX,
        y = clampedY
    )
}

internal fun screenToLocalPoint(
    screenPoint: Offset,
    baseLayout: BaseImageLayout,
    panOffset: Offset,
    zoomScale: Float
): Offset {
    return Offset(
        x = (screenPoint.x - baseLayout.left - panOffset.x) / zoomScale,
        y = (screenPoint.y - baseLayout.top - panOffset.y) / zoomScale
    )
}

internal fun localOffsetToImagePoint(
    localPoint: Offset,
    baseLayout: BaseImageLayout,
    imageWidth: Int,
    imageHeight: Int
): HoldPoint {
    return HoldPoint(
        x = (localPoint.x / baseLayout.fitScale).roundToInt().coerceIn(0, imageWidth),
        y = (localPoint.y / baseLayout.fitScale).roundToInt().coerceIn(0, imageHeight)
    )
}

internal fun HoldPoint.toLocalOffset(baseLayout: BaseImageLayout): Offset {
    return Offset(
        x = x * baseLayout.fitScale,
        y = y * baseLayout.fitScale
    )
}

internal fun isInsideLocalBounds(
    localPoint: Offset,
    baseLayout: BaseImageLayout
): Boolean {
    return localPoint.x in 0f..baseLayout.drawWidth &&
        localPoint.y in 0f..baseLayout.drawHeight
}

internal fun Hold.toLocalPolygon(baseLayout: BaseImageLayout): LocalHoldPolygon {
    return LocalHoldPolygon(
        points = points.map { point ->
            Offset(
                x = point.x * baseLayout.fitScale,
                y = point.y * baseLayout.fitScale
            )
        }
    )
}

internal fun appendPointIfNeeded(
    currentPoints: List<Offset>,
    candidatePoint: Offset,
    minDistance: Float,
    baseLayout: BaseImageLayout
): List<Offset> {
    val clamped = Offset(
        x = candidatePoint.x.coerceIn(0f, baseLayout.drawWidth),
        y = candidatePoint.y.coerceIn(0f, baseLayout.drawHeight)
    )

    val lastPoint = currentPoints.lastOrNull() ?: return listOf(clamped)
    val dx = clamped.x - lastPoint.x
    val dy = clamped.y - lastPoint.y
    return if (dx * dx + dy * dy >= minDistance * minDistance) {
        currentPoints + clamped
    } else {
        currentPoints
    }
}

internal fun buildContourPolygonFromBrushPoints(
    points: List<Offset>,
    brushRadiusX: Float,
    brushRadiusY: Float,
    baseLayout: BaseImageLayout
): LocalHoldPolygon? {
    if (points.isEmpty() || !baseLayout.isValid) return null

    val strokeMask = rasterizeBrushStrokeToMask(
        points = points,
        brushRadiusX = brushRadiusX,
        brushRadiusY = brushRadiusY,
        baseLayout = baseLayout
    )

    val boundaryPixels = traceBoundaryPixels(strokeMask) ?: return null
    val contourPoints = boundaryPixels.map { pixel ->
        Offset(
            x = strokeMask.localLeft + (pixel.x - 1) + 0.5f,
            y = strokeMask.localTop + (pixel.y - 1) + 0.5f
        )
    }

    val thinnedPoints = thinBoundaryPoints(
        points = contourPoints,
        minDistance = 2f
    )

    return if (thinnedPoints.size >= 3) {
        LocalHoldPolygon(thinnedPoints)
    } else {
        null
    }
}

private data class StrokeMask(
    val width: Int,
    val height: Int,
    val localLeft: Float,
    val localTop: Float,
    val pixels: BooleanArray
) {
    fun isFilled(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        return pixels[y * width + x]
    }

    fun setFilled(x: Int, y: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        pixels[y * width + x] = true
    }
}

private data class RasterPixel(
    val x: Int,
    val y: Int
)

private fun rasterizeBrushStrokeToMask(
    points: List<Offset>,
    brushRadiusX: Float,
    brushRadiusY: Float,
    baseLayout: BaseImageLayout
): StrokeMask {
    val safeRadiusX = brushRadiusX.coerceAtLeast(1f)
    val safeRadiusY = brushRadiusY.coerceAtLeast(1f)

    val minPointX = points.minOf { it.x }
    val maxPointX = points.maxOf { it.x }
    val minPointY = points.minOf { it.y }
    val maxPointY = points.maxOf { it.y }

    val localLeft = kotlin.math.floor((minPointX - safeRadiusX - 2f).coerceAtLeast(0f))
    val localTop = kotlin.math.floor((minPointY - safeRadiusY - 2f).coerceAtLeast(0f))
    val localRight = kotlin.math.ceil((maxPointX + safeRadiusX + 2f).coerceAtMost(baseLayout.drawWidth))
    val localBottom = kotlin.math.ceil((maxPointY + safeRadiusY + 2f).coerceAtMost(baseLayout.drawHeight))

    val usableWidth = (localRight - localLeft).toInt().coerceAtLeast(1)
    val usableHeight = (localBottom - localTop).toInt().coerceAtLeast(1)
    val maskWidth = usableWidth + 2
    val maskHeight = usableHeight + 2

    val mask = StrokeMask(
        width = maskWidth,
        height = maskHeight,
        localLeft = localLeft,
        localTop = localTop,
        pixels = BooleanArray(maskWidth * maskHeight)
    )

    val densePoints = densifyStrokePoints(
        points = points,
        step = (minOf(safeRadiusX, safeRadiusY) * 0.45f).coerceAtLeast(2f)
    )

    densePoints.forEach { point ->
        fillEllipseOnMask(
            mask = mask,
            centerX = point.x - localLeft + 1f,
            centerY = point.y - localTop + 1f,
            radiusX = safeRadiusX,
            radiusY = safeRadiusY
        )
    }

    return mask
}

private fun densifyStrokePoints(
    points: List<Offset>,
    step: Float
): List<Offset> {
    if (points.isEmpty()) return emptyList()
    if (points.size == 1) return points

    val densePoints = mutableListOf(points.first())
    val safeStep = step.coerceAtLeast(1f)

    for (index in 0 until points.lastIndex) {
        val start = points[index]
        val end = points[index + 1]
        val dx = end.x - start.x
        val dy = end.y - start.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val segments = kotlin.math.ceil(distance / safeStep).toInt().coerceAtLeast(1)

        for (segment in 1..segments) {
            val t = segment.toFloat() / segments.toFloat()
            densePoints += Offset(
                x = start.x + dx * t,
                y = start.y + dy * t
            )
        }
    }

    return densePoints
}

private fun fillEllipseOnMask(
    mask: StrokeMask,
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float
) {
    val safeRadiusX = radiusX.coerceAtLeast(1f)
    val safeRadiusY = radiusY.coerceAtLeast(1f)

    val left = kotlin.math.floor(centerX - safeRadiusX).toInt()
    val right = kotlin.math.ceil(centerX + safeRadiusX).toInt()
    val top = kotlin.math.floor(centerY - safeRadiusY).toInt()
    val bottom = kotlin.math.ceil(centerY + safeRadiusY).toInt()

    for (y in top..bottom) {
        for (x in left..right) {
            val normalizedX = ((x + 0.5f) - centerX) / safeRadiusX
            val normalizedY = ((y + 0.5f) - centerY) / safeRadiusY
            if (normalizedX * normalizedX + normalizedY * normalizedY <= 1f) {
                mask.setFilled(x, y)
            }
        }
    }
}

private fun traceBoundaryPixels(mask: StrokeMask): List<RasterPixel>? {
    val start = findFirstBoundaryPixel(mask) ?: return null
    val directions = listOf(
        RasterPixel(-1, 0),
        RasterPixel(-1, -1),
        RasterPixel(0, -1),
        RasterPixel(1, -1),
        RasterPixel(1, 0),
        RasterPixel(1, 1),
        RasterPixel(0, 1),
        RasterPixel(-1, 1)
    )

    var current = start
    var previous = RasterPixel(start.x - 1, start.y)
    val startPrevious = previous
    val boundary = mutableListOf<RasterPixel>()
    var guard = 0

    while (guard < mask.width * mask.height * 4) {
        guard++
        boundary += current

        val relativePrevious = RasterPixel(previous.x - current.x, previous.y - current.y)
        val startDirectionIndex = directions.indexOf(relativePrevious).takeIf { it >= 0 } ?: 0

        var nextDirectionIndex: Int? = null
        for (offset in directions.indices) {
            val candidateIndex = (startDirectionIndex + offset) % directions.size
            val direction = directions[candidateIndex]
            val candidate = RasterPixel(current.x + direction.x, current.y + direction.y)
            if (mask.isFilled(candidate.x, candidate.y)) {
                nextDirectionIndex = candidateIndex
                break
            }
        }

        val foundDirectionIndex = nextDirectionIndex ?: break
        val nextDirection = directions[foundDirectionIndex]
        val previousDirection = directions[(foundDirectionIndex - 1 + directions.size) % directions.size]

        previous = RasterPixel(current.x + previousDirection.x, current.y + previousDirection.y)
        current = RasterPixel(current.x + nextDirection.x, current.y + nextDirection.y)

        if (current == start && previous == startPrevious && boundary.size > 2) {
            break
        }
    }

    return boundary.takeIf { it.size >= 3 }
}

private fun findFirstBoundaryPixel(mask: StrokeMask): RasterPixel? {
    for (y in 1 until mask.height - 1) {
        for (x in 1 until mask.width - 1) {
            if (!mask.isFilled(x, y)) continue
            if (
                !mask.isFilled(x - 1, y) ||
                    !mask.isFilled(x + 1, y) ||
                    !mask.isFilled(x, y - 1) ||
                    !mask.isFilled(x, y + 1)
            ) {
                return RasterPixel(x, y)
            }
        }
    }
    return null
}

private fun thinBoundaryPoints(
    points: List<Offset>,
    minDistance: Float
): List<Offset> {
    if (points.size <= 3) return points.distinct()

    val kept = mutableListOf(points.first())
    val minDistanceSquared = minDistance * minDistance

    for (index in 1 until points.size) {
        val point = points[index]
        val last = kept.last()
        val dx = point.x - last.x
        val dy = point.y - last.y
        if (dx * dx + dy * dy >= minDistanceSquared) {
            kept += point
        }
    }

    if (kept.size >= 3) {
        val first = kept.first()
        val last = kept.last()
        val dx = first.x - last.x
        val dy = first.y - last.y
        if (dx * dx + dy * dy < minDistanceSquared) {
            kept.removeAt(kept.lastIndex)
        }
    }

    return kept.distinct()
}

internal fun createTapHoldFromLocalPoint(
    localPoint: Offset,
    localRadiusX: Float,
    localRadiusY: Float,
    baseLayout: BaseImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    onManualHoldCreated: (Hold) -> Unit
) {
    val polygon = buildContourPolygonFromBrushPoints(
        points = listOf(localPoint),
        brushRadiusX = localRadiusX,
        brushRadiusY = localRadiusY,
        baseLayout = baseLayout
    ) ?: return

    onManualHoldCreated(
        localPolygonToHold(
            polygon = polygon,
            baseLayout = baseLayout,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    )
}

internal fun createManualHoldFromBrushPoints(
    points: List<Offset>,
    brushRadiusX: Float,
    brushRadiusY: Float,
    baseLayout: BaseImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    onManualHoldCreated: (Hold) -> Unit
) {
    val polygon = buildContourPolygonFromBrushPoints(
        points = points,
        brushRadiusX = brushRadiusX,
        brushRadiusY = brushRadiusY,
        baseLayout = baseLayout
    ) ?: return

    onManualHoldCreated(
        localPolygonToHold(
            polygon = polygon,
            baseLayout = baseLayout,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    )
}

internal fun localPolygonToHold(
    polygon: LocalHoldPolygon,
    baseLayout: BaseImageLayout,
    imageWidth: Int,
    imageHeight: Int
): Hold {
    val imagePoints = polygon.points
        .map { point ->
            HoldPoint(
                x = (point.x / baseLayout.fitScale).roundToInt().coerceIn(0, imageWidth),
                y = (point.y / baseLayout.fitScale).roundToInt().coerceIn(0, imageHeight)
            )
        }
        .distinct()

    return Hold(
        points = if (imagePoints.size >= 3) {
            imagePoints
        } else {
            listOf(
                HoldPoint(0, 0),
                HoldPoint(1, 0),
                HoldPoint(0, 1)
            )
        }
    )
}

internal fun findTappedIndexFromLocal(
    localPoint: Offset,
    holds: List<Hold>,
    baseLayout: BaseImageLayout
): Int? {
    return holds.indexOfLast { hold ->
        isPointInsidePolygon(
            point = localPoint,
            polygon = hold.toLocalPolygon(baseLayout).points
        )
    }.takeIf { it >= 0 }
}

internal fun findHoldIndicesIntersectingSelectionPolygon(
    selectionPolygon: LocalHoldPolygon,
    holds: List<Hold>,
    baseLayout: BaseImageLayout
): Set<Int> {
    return holds.mapIndexedNotNull { index, hold ->
        val holdPolygon = hold.toLocalPolygon(baseLayout).points
        if (polygonsIntersect(selectionPolygon.points, holdPolygon)) {
            index
        } else {
            null
        }
    }.toSet()
}

private fun polygonsIntersect(
    first: List<Offset>,
    second: List<Offset>
): Boolean {
    if (first.size < 3 || second.size < 3) return false

    if (first.any { point -> isPointInsidePolygon(point, second) }) return true
    if (second.any { point -> isPointInsidePolygon(point, first) }) return true

    val firstEdges = polygonEdges(first)
    val secondEdges = polygonEdges(second)

    return firstEdges.any { (firstStart, firstEnd) ->
        secondEdges.any { (secondStart, secondEnd) ->
            segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)
        }
    }
}

private fun polygonEdges(points: List<Offset>): List<Pair<Offset, Offset>> {
    if (points.size < 2) return emptyList()

    return points.indices.map { index ->
        val nextIndex = (index + 1) % points.size
        points[index] to points[nextIndex]
    }
}

private fun segmentsIntersect(
    firstStart: Offset,
    firstEnd: Offset,
    secondStart: Offset,
    secondEnd: Offset
): Boolean {
    val firstOrientation = segmentOrientation(firstStart, firstEnd, secondStart)
    val secondOrientation = segmentOrientation(firstStart, firstEnd, secondEnd)
    val thirdOrientation = segmentOrientation(secondStart, secondEnd, firstStart)
    val fourthOrientation = segmentOrientation(secondStart, secondEnd, firstEnd)

    if (firstOrientation != secondOrientation && thirdOrientation != fourthOrientation) {
        return true
    }

    if (firstOrientation == 0 && isPointOnSegment(secondStart, firstStart, firstEnd)) return true
    if (secondOrientation == 0 && isPointOnSegment(secondEnd, firstStart, firstEnd)) return true
    if (thirdOrientation == 0 && isPointOnSegment(firstStart, secondStart, secondEnd)) return true
    if (fourthOrientation == 0 && isPointOnSegment(firstEnd, secondStart, secondEnd)) return true

    return false
}

private fun segmentOrientation(
    start: Offset,
    end: Offset,
    point: Offset
): Int {
    val value = (end.y - start.y) * (point.x - end.x) - (end.x - start.x) * (point.y - end.y)

    return when {
        abs(value) < 0.0001f -> 0
        value > 0f -> 1
        else -> 2
    }
}

private fun isPointInsidePolygon(
    point: Offset,
    polygon: List<Offset>
): Boolean {
    if (polygon.size < 3) return false

    var inside = false
    var previous = polygon.last()

    for (current in polygon) {
        if (isPointOnSegment(point, previous, current)) {
            return true
        }

        val intersects = ((current.y > point.y) != (previous.y > point.y)) &&
            (point.x < (previous.x - current.x) * (point.y - current.y) / ((previous.y - current.y).takeIf { it != 0f } ?: 0.0001f) + current.x)

        if (intersects) {
            inside = !inside
        }
        previous = current
    }

    return inside
}

private fun isPointOnSegment(
    point: Offset,
    start: Offset,
    end: Offset,
    tolerance: Float = 3f
): Boolean {
    val cross = (point.y - start.y) * (end.x - start.x) - (point.x - start.x) * (end.y - start.y)
    if (abs(cross) > tolerance) return false

    val dot = (point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)
    if (dot < 0f) return false

    val squaredLength = (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
    if (dot > squaredLength) return false

    return true
}
