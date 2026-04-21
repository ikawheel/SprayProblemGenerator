package com.example.holddetector.domain.hold

import android.graphics.Bitmap
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object BinaryHoldExtractor {

    fun extract(
        bitmap: Bitmap,
        tuning: AutoExtractionTuning = AutoExtractionTuning(),
        wallSamplePoints: List<HoldPoint> = emptyList()
    ): List<Hold> {
        if (bitmap.width < 24 || bitmap.height < 24) return emptyList()

        val maxSide = 960
        val scale = max(bitmap.width, bitmap.height).toFloat() / maxSide.toFloat()
        val workingBitmap = if (scale > 1f) {
            val scaledWidth = (bitmap.width / scale).roundToInt().coerceAtLeast(1)
            val scaledHeight = (bitmap.height / scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        } else {
            bitmap
        }

        val workingWidth = workingBitmap.width
        val workingHeight = workingBitmap.height
        val pixels = IntArray(workingWidth * workingHeight)
        workingBitmap.getPixels(
            pixels,
            0,
            workingWidth,
            0,
            0,
            workingWidth,
            workingHeight
        )

        val grayscale = IntArray(pixels.size)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            grayscale[index] = (0.299 * red + 0.587 * green + 0.114 * blue).roundToInt()
        }

        val borderStats = sampleBorderStats(
            grayscale = grayscale,
            width = workingWidth,
            height = workingHeight
        )
        val wallReferenceStats = sampleWallReferenceStats(
            pixels = pixels,
            grayscale = grayscale,
            width = workingWidth,
            height = workingHeight,
            originalWidth = bitmap.width,
            originalHeight = bitmap.height,
            wallSamplePoints = wallSamplePoints
        )
        val grayStats = wallReferenceStats?.grayStats ?: borderStats
        val threshold = max(
            tuning.minimumThreshold.toDouble().coerceAtLeast(0.0),
            grayStats.stdDev * tuning.standardDeviationMultiplier.toDouble().coerceAtLeast(0.0)
        )
        val colorThreshold = wallReferenceStats?.let { referenceStats ->
            max(
                8.0,
                referenceStats.colorStdDev * tuning.standardDeviationMultiplier.toDouble().coerceAtLeast(0.0)
            )
        }
        val initialMask = BooleanArray(grayscale.size) { index ->
            val brightnessDifference = abs(grayscale[index] - grayStats.mean)
            if (wallReferenceStats != null && colorThreshold != null) {
                val pixel = pixels[index]
                val colorDistance = deltaE(
                    first = rgbToLab(
                        red = pixel shr 16 and 0xFF,
                        green = pixel shr 8 and 0xFF,
                        blue = pixel and 0xFF
                    ),
                    second = wallReferenceStats.meanLab
                )
                colorDistance >= colorThreshold ||
                    (brightnessDifference >= threshold && colorDistance >= colorThreshold * 0.35)
            } else {
                brightnessDifference >= threshold
            }
        }
        val smoothedMask = smoothMask(
            mask = initialMask,
            width = workingWidth,
            height = workingHeight,
            smoothingStrength = tuning.smoothingStrength
        )
        val components = collectComponents(
            mask = smoothedMask,
            width = workingWidth,
            height = workingHeight
        )

        val areaStrength = tuning.areaFilterStrength.coerceAtLeast(0.1f)
        val minArea = max(
            32,
            (workingWidth * workingHeight * 0.00022 * areaStrength).roundToInt()
        )
        val maxArea = max(minArea + 1, (workingWidth * workingHeight * 0.08).roundToInt())
        val scaleX = bitmap.width.toFloat() / workingWidth.toFloat()
        val scaleY = bitmap.height.toFloat() / workingHeight.toFloat()

        return components
            .filter { component ->
                val componentWidth = component.maxX - component.minX + 1
                val componentHeight = component.maxY - component.minY + 1
                val shortestSide = min(componentWidth, componentHeight)
                val longestSide = max(componentWidth, componentHeight)
                val aspectRatio = longestSide.toFloat() / shortestSide.toFloat().coerceAtLeast(1f)

                !component.touchesEdge &&
                    component.area in minArea..maxArea &&
                    componentWidth >= 6 &&
                    componentHeight >= 6 &&
                    aspectRatio <= 5.5f
            }
            .sortedWith(compareBy<Component> { it.minY }.thenBy { it.minX })
            .map { component ->
                component.toHold(
                    scaleX = scaleX,
                    scaleY = scaleY,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height
                )
            }
    }

    private data class BorderStats(
        val mean: Double,
        val stdDev: Double
    )

    private data class Component(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
        val area: Int,
        val touchesEdge: Boolean,
        val pixels: IntArray
    ) {
        fun toHold(
            scaleX: Float,
            scaleY: Float,
            imageWidth: Int,
            imageHeight: Int
        ): Hold {
            val componentMask = createComponentMask()
            val boundaryPixels = traceBoundaryPixels(componentMask)
            if (boundaryPixels != null) {
                val minDistance = max(2f, min(maxX - minX + 1, maxY - minY + 1) * 0.08f)
                val thinnedPoints = thinBoundaryPoints(
                    points = boundaryPixels.map { pixel ->
                        FloatPoint(
                            x = componentMask.localLeft + (pixel.x - 1) + 0.5f,
                            y = componentMask.localTop + (pixel.y - 1) + 0.5f
                        )
                    },
                    minDistance = minDistance
                )
                val holdPoints = thinnedPoints
                    .map { point ->
                        HoldPoint(
                            x = (point.x * scaleX).roundToInt().coerceIn(0, imageWidth - 1),
                            y = (point.y * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
                        )
                    }
                    .distinct()

                if (holdPoints.size >= 3 && isUsablePolygon(holdPoints)) {
                    return Hold(points = holdPoints)
                }
            }

            val paddingX = max(2, ((maxX - minX + 1) * 0.08f).roundToInt())
            val paddingY = max(2, ((maxY - minY + 1) * 0.08f).roundToInt())
            val left = ((minX - paddingX) * scaleX).roundToInt().coerceIn(0, imageWidth - 1)
            val top = ((minY - paddingY) * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
            val right = ((maxX + paddingX) * scaleX).roundToInt().coerceIn(0, imageWidth - 1)
            val bottom = ((maxY + paddingY) * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
            val normalizedRight = max(right, left + 1).coerceAtMost(imageWidth - 1)
            val normalizedBottom = max(bottom, top + 1).coerceAtMost(imageHeight - 1)

            return Hold(
                points = listOf(
                    HoldPoint(left, top),
                    HoldPoint(normalizedRight, top),
                    HoldPoint(normalizedRight, normalizedBottom),
                    HoldPoint(left, normalizedBottom)
                )
            )
        }

        private fun createComponentMask(): ComponentMask {
            val localLeft = (minX - 1).coerceAtLeast(0)
            val localTop = (minY - 1).coerceAtLeast(0)
            val localRight = maxX + 1
            val localBottom = maxY + 1
            val usableWidth = (localRight - localLeft + 1).coerceAtLeast(1)
            val usableHeight = (localBottom - localTop + 1).coerceAtLeast(1)
            val mask = ComponentMask(
                width = usableWidth + 2,
                height = usableHeight + 2,
                localLeft = localLeft.toFloat(),
                localTop = localTop.toFloat(),
                pixels = BooleanArray((usableWidth + 2) * (usableHeight + 2))
            )

            for (pixelIndex in pixels) {
                val x = pixelIndex and 0xFFFF
                val y = pixelIndex ushr 16
                mask.setFilled(x - localLeft + 1, y - localTop + 1)
            }

            return mask
        }
    }

    private data class ComponentMask(
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

    private data class BoundaryPixel(
        val x: Int,
        val y: Int
    )

    private data class FloatPoint(
        val x: Float,
        val y: Float
    )

    private data class LabColor(
        val l: Double,
        val a: Double,
        val b: Double
    )

    private data class WallReferenceStats(
        val grayStats: BorderStats,
        val meanLab: LabColor,
        val colorStdDev: Double
    )

    private fun sampleBorderStats(
        grayscale: IntArray,
        width: Int,
        height: Int
    ): BorderStats {
        val marginX = max(4, width / 18)
        val marginY = max(4, height / 18)
        var sum = 0.0
        var sumSquares = 0.0
        var count = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < marginX || x >= width - marginX || y < marginY || y >= height - marginY) {
                    val value = grayscale[y * width + x].toDouble()
                    sum += value
                    sumSquares += value * value
                    count += 1
                }
            }
        }

        if (count == 0) return BorderStats(mean = 127.0, stdDev = 24.0)

        val mean = sum / count.toDouble()
        val variance = (sumSquares / count.toDouble()) - mean * mean
        return BorderStats(
            mean = mean,
            stdDev = sqrt(max(variance, 0.0))
        )
    }

    private fun sampleWallReferenceStats(
        pixels: IntArray,
        grayscale: IntArray,
        width: Int,
        height: Int,
        originalWidth: Int,
        originalHeight: Int,
        wallSamplePoints: List<HoldPoint>
    ): WallReferenceStats? {
        if (wallSamplePoints.isEmpty()) return null

        val sampleDescriptors = wallSamplePoints.mapNotNull { point ->
            val localX = ((point.x.toFloat() / originalWidth.toFloat()) * width.toFloat()).roundToInt()
            val localY = ((point.y.toFloat() / originalHeight.toFloat()) * height.toFloat()).roundToInt()
            samplePatchDescriptor(
                pixels = pixels,
                grayscale = grayscale,
                width = width,
                height = height,
                centerX = localX,
                centerY = localY
            )
        }

        if (sampleDescriptors.isEmpty()) return null

        val grayMean = sampleDescriptors.map { it.gray }.average()
        val grayVariance = sampleDescriptors
            .map { descriptor -> (descriptor.gray - grayMean) * (descriptor.gray - grayMean) }
            .average()
        val meanLab = LabColor(
            l = sampleDescriptors.map { it.lab.l }.average(),
            a = sampleDescriptors.map { it.lab.a }.average(),
            b = sampleDescriptors.map { it.lab.b }.average()
        )
        val colorVariance = sampleDescriptors
            .map { descriptor ->
                val distance = deltaE(descriptor.lab, meanLab)
                distance * distance
            }
            .average()

        return WallReferenceStats(
            grayStats = BorderStats(
                mean = grayMean,
                stdDev = sqrt(max(grayVariance, 0.0))
            ),
            meanLab = meanLab,
            colorStdDev = sqrt(max(colorVariance, 0.0))
        )
    }

    private data class SamplePatchDescriptor(
        val gray: Double,
        val lab: LabColor
    )

    private fun samplePatchDescriptor(
        pixels: IntArray,
        grayscale: IntArray,
        width: Int,
        height: Int,
        centerX: Int,
        centerY: Int
    ): SamplePatchDescriptor? {
        val radius = 4
        val startX = (centerX - radius).coerceIn(0, width - 1)
        val endX = (centerX + radius).coerceIn(0, width - 1)
        val startY = (centerY - radius).coerceIn(0, height - 1)
        val endY = (centerY + radius).coerceIn(0, height - 1)

        var graySum = 0.0
        var lSum = 0.0
        var aSum = 0.0
        var bSum = 0.0
        var count = 0

        for (y in startY..endY) {
            for (x in startX..endX) {
                val index = y * width + x
                val pixel = pixels[index]
                val lab = rgbToLab(
                    red = pixel shr 16 and 0xFF,
                    green = pixel shr 8 and 0xFF,
                    blue = pixel and 0xFF
                )
                graySum += grayscale[index].toDouble()
                lSum += lab.l
                aSum += lab.a
                bSum += lab.b
                count += 1
            }
        }

        if (count == 0) return null

        return SamplePatchDescriptor(
            gray = graySum / count.toDouble(),
            lab = LabColor(
                l = lSum / count.toDouble(),
                a = aSum / count.toDouble(),
                b = bSum / count.toDouble()
            )
        )
    }

    private fun deltaE(
        first: LabColor,
        second: LabColor
    ): Double {
        val dl = first.l - second.l
        val da = first.a - second.a
        val db = first.b - second.b
        return sqrt(dl * dl + da * da + db * db)
    }

    private fun rgbToLab(
        red: Int,
        green: Int,
        blue: Int
    ): LabColor {
        fun pivotRgb(value: Double): Double {
            return if (value > 0.04045) {
                Math.pow((value + 0.055) / 1.055, 2.4)
            } else {
                value / 12.92
            }
        }

        fun pivotXyz(value: Double): Double {
            return if (value > 0.008856) {
                Math.pow(value, 1.0 / 3.0)
            } else {
                (7.787 * value) + (16.0 / 116.0)
            }
        }

        val r = pivotRgb(red / 255.0)
        val g = pivotRgb(green / 255.0)
        val b = pivotRgb(blue / 255.0)

        val x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
        val y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.00000
        val z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883

        val fx = pivotXyz(x)
        val fy = pivotXyz(y)
        val fz = pivotXyz(z)

        return LabColor(
            l = (116.0 * fy) - 16.0,
            a = 500.0 * (fx - fy),
            b = 200.0 * (fy - fz)
        )
    }

    private fun smoothMask(
        mask: BooleanArray,
        width: Int,
        height: Int,
        smoothingStrength: Float
    ): BooleanArray {
        var currentMask = mask.copyOf()
        val normalizedStrength = smoothingStrength.coerceIn(0f, 1f)
        val passes = when {
            normalizedStrength < 0.2f -> 0
            normalizedStrength < 0.45f -> 1
            normalizedStrength < 0.75f -> 2
            else -> 3
        }

        repeat(passes) {
            val filledNeighborThreshold = interpolateInt(
                start = 1,
                end = 3,
                fraction = normalizedStrength
            )
            val emptyNeighborThreshold = interpolateInt(
                start = 7,
                end = 5,
                fraction = normalizedStrength
            )
            val smoothed = BooleanArray(currentMask.size)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var trueNeighbors = 0
                    for (deltaY in -1..1) {
                        for (deltaX in -1..1) {
                            if (deltaX == 0 && deltaY == 0) continue
                            val neighborX = x + deltaX
                            val neighborY = y + deltaY
                            if (neighborX !in 0 until width || neighborY !in 0 until height) continue
                            if (currentMask[neighborY * width + neighborX]) {
                                trueNeighbors += 1
                            }
                        }
                    }
                    val index = y * width + x
                    smoothed[index] = if (currentMask[index]) {
                        trueNeighbors >= filledNeighborThreshold
                    } else {
                        trueNeighbors >= emptyNeighborThreshold
                    }
                }
            }
            currentMask = smoothed
        }

        return currentMask
    }

    private fun collectComponents(
        mask: BooleanArray,
        width: Int,
        height: Int
    ): List<Component> {
        val visited = BooleanArray(mask.size)
        val queue = IntArray(mask.size)
        val components = mutableListOf<Component>()

        for (startIndex in mask.indices) {
            if (!mask[startIndex] || visited[startIndex]) continue

            var head = 0
            var tail = 0
            queue[tail++] = startIndex
            visited[startIndex] = true

            var area = 0
            var minX = width
            var minY = height
            var maxX = 0
            var maxY = 0
            var touchesEdge = false
            val componentPixels = mutableListOf<Int>()

            while (head < tail) {
                val index = queue[head++]
                val x = index % width
                val y = index / width
                area += 1
                componentPixels += (y shl 16) or x
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    touchesEdge = true
                }

                for (deltaY in -1..1) {
                    for (deltaX in -1..1) {
                        if (deltaX == 0 && deltaY == 0) continue
                        val neighborX = x + deltaX
                        val neighborY = y + deltaY
                        if (neighborX !in 0 until width || neighborY !in 0 until height) continue
                        val neighborIndex = neighborY * width + neighborX
                        if (!mask[neighborIndex] || visited[neighborIndex]) continue
                        visited[neighborIndex] = true
                        queue[tail++] = neighborIndex
                    }
                }
            }

            components += Component(
                minX = minX,
                minY = minY,
                maxX = maxX,
                maxY = maxY,
                area = area,
                touchesEdge = touchesEdge,
                pixels = componentPixels.toIntArray()
            )
        }

        return components
    }

    private fun traceBoundaryPixels(mask: ComponentMask): List<BoundaryPixel>? {
        val start = findFirstBoundaryPixel(mask) ?: return null
        val directions = listOf(
            BoundaryPixel(-1, 0),
            BoundaryPixel(-1, -1),
            BoundaryPixel(0, -1),
            BoundaryPixel(1, -1),
            BoundaryPixel(1, 0),
            BoundaryPixel(1, 1),
            BoundaryPixel(0, 1),
            BoundaryPixel(-1, 1)
        )

        var current = start
        var previous = BoundaryPixel(start.x - 1, start.y)
        val startPrevious = previous
        val boundary = mutableListOf<BoundaryPixel>()
        var guard = 0

        while (guard < mask.width * mask.height * 4) {
            guard += 1
            boundary += current

            val relativePrevious = BoundaryPixel(previous.x - current.x, previous.y - current.y)
            val startDirectionIndex = directions.indexOf(relativePrevious).takeIf { it >= 0 } ?: 0

            var nextDirectionIndex: Int? = null
            for (offset in directions.indices) {
                val candidateIndex = (startDirectionIndex + offset) % directions.size
                val direction = directions[candidateIndex]
                val candidate = BoundaryPixel(current.x + direction.x, current.y + direction.y)
                if (mask.isFilled(candidate.x, candidate.y)) {
                    nextDirectionIndex = candidateIndex
                    break
                }
            }

            val foundDirectionIndex = nextDirectionIndex ?: break
            val nextDirection = directions[foundDirectionIndex]
            val previousDirection = directions[(foundDirectionIndex - 1 + directions.size) % directions.size]

            previous = BoundaryPixel(current.x + previousDirection.x, current.y + previousDirection.y)
            current = BoundaryPixel(current.x + nextDirection.x, current.y + nextDirection.y)

            if (current == start && previous == startPrevious && boundary.size > 2) {
                break
            }
        }

        return boundary.takeIf { it.size >= 3 }
    }

    private fun findFirstBoundaryPixel(mask: ComponentMask): BoundaryPixel? {
        for (y in 1 until mask.height - 1) {
            for (x in 1 until mask.width - 1) {
                if (!mask.isFilled(x, y)) continue
                if (
                    !mask.isFilled(x - 1, y) ||
                    !mask.isFilled(x + 1, y) ||
                    !mask.isFilled(x, y - 1) ||
                    !mask.isFilled(x, y + 1)
                ) {
                    return BoundaryPixel(x, y)
                }
            }
        }
        return null
    }

    private fun thinBoundaryPoints(
        points: List<FloatPoint>,
        minDistance: Float
    ): List<FloatPoint> {
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

    private fun isUsablePolygon(points: List<HoldPoint>): Boolean {
        if (points.size < 3) return false

        var doubledArea = 0.0
        for (index in points.indices) {
            val current = points[index]
            val next = points[(index + 1) % points.size]
            doubledArea += current.x.toDouble() * next.y.toDouble()
            doubledArea -= next.x.toDouble() * current.y.toDouble()
        }

        return kotlin.math.abs(doubledArea) >= 8.0
    }

    private fun interpolateInt(
        start: Int,
        end: Int,
        fraction: Float
    ): Int {
        return (start + (end - start) * fraction).roundToInt()
    }
}
