package com.ikeansoft.holddetector.domain.hold

import android.graphics.Bitmap
import android.graphics.Color
import com.ikeansoft.holddetector.model.Hold
import com.ikeansoft.holddetector.model.HoldPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object BinaryHoldExtractor {

    fun extract(
        bitmap: Bitmap,
        tuning: AutoExtractionTuning = AutoExtractionTuning(),
        wallSamplePoints: List<HoldPoint> = emptyList(),
        selectedColors: Set<HoldColorCategory> = defaultSelectedAutoExtractionColors()
    ): List<Hold> {
        if (bitmap.width < 24 || bitmap.height < 24) return emptyList()
        if (selectedColors.isEmpty()) return emptyList()

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

        val hsvPixels = Array(pixels.size) { index ->
            rgbToHsv(pixels[index])
        }
        val backgroundModel = sampleBackgroundHsvModel(
            hsvPixels = hsvPixels,
            width = workingWidth,
            height = workingHeight,
            originalWidth = bitmap.width,
            originalHeight = bitmap.height,
            wallSamplePoints = wallSamplePoints
        )
        val holdColorScores = FloatArray(hsvPixels.size)
        val backgroundDistanceScores = FloatArray(hsvPixels.size)
        for (index in hsvPixels.indices) {
            val hsv = hsvPixels[index]
            holdColorScores[index] = calculateHoldColorScore(
                color = hsv,
                selectedColors = selectedColors,
                tuning = tuning
            )
            backgroundDistanceScores[index] = calculateBackgroundDistanceScore(
                color = hsv,
                backgroundModel = backgroundModel
            )
        }
        val initialMask = BooleanArray(hsvPixels.size) { index ->
            val holdColorScore = holdColorScores[index]
            val backgroundDistanceScore = backgroundDistanceScores[index]
            val finalScore = holdColorScore * 0.72f + backgroundDistanceScore * 0.28f
            backgroundDistanceScore >= tuning.backgroundDistanceThreshold &&
                holdColorScore >= 0.24f &&
                finalScore >= 0.34f
        }
        val smoothedMask = smoothMask(
            mask = initialMask,
            width = workingWidth,
            height = workingHeight,
            smoothingStrength = 0.38f
        )
        val components = collectComponents(
            mask = smoothedMask,
            width = workingWidth,
            height = workingHeight
        )

        val minArea = max(
            24,
            (workingWidth * workingHeight * 0.00018f).roundToInt()
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
            val componentWidth = maxX - minX + 1
            val componentHeight = maxY - minY + 1
            val componentMask = createComponentMask()
            val boundaryPixels = traceBoundaryPixels(componentMask)
            if (boundaryPixels != null) {
                val minDistance = max(2f, min(componentWidth, componentHeight) * 0.08f)
                val thinnedPoints = thinBoundaryPoints(
                    points = boundaryPixels.map { pixel ->
                        FloatPoint(
                            x = componentMask.localLeft + (pixel.x - 1) + 0.5f,
                            y = componentMask.localTop + (pixel.y - 1) + 0.5f
                        )
                    },
                    minDistance = minDistance
                )
                val silhouettePoints = buildRoundedSilhouette(
                    points = thinnedPoints,
                    componentWidth = componentWidth,
                    componentHeight = componentHeight
                )
                val holdPoints = silhouettePoints
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

            val paddingX = max(2f, componentWidth * 0.08f)
            val paddingY = max(2f, componentHeight * 0.08f)
            val fallbackBase = listOf(
                FloatPoint(minX.toFloat() - paddingX, minY.toFloat() - paddingY),
                FloatPoint(maxX.toFloat() + paddingX, minY.toFloat() - paddingY),
                FloatPoint(maxX.toFloat() + paddingX, maxY.toFloat() + paddingY),
                FloatPoint(minX.toFloat() - paddingX, maxY.toFloat() + paddingY)
            )
            val fallbackPoints = buildRoundedSilhouette(
                points = fallbackBase,
                componentWidth = componentWidth,
                componentHeight = componentHeight
            ).map { point ->
                HoldPoint(
                    x = (point.x * scaleX).roundToInt().coerceIn(0, imageWidth - 1),
                    y = (point.y * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
                )
            }.distinct()

            return if (fallbackPoints.size >= 3 && isUsablePolygon(fallbackPoints)) {
                Hold(points = fallbackPoints)
            } else {
                val left = ((minX - paddingX) * scaleX).roundToInt().coerceIn(0, imageWidth - 1)
                val top = ((minY - paddingY) * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
                val right = ((maxX + paddingX) * scaleX).roundToInt().coerceIn(0, imageWidth - 1)
                val bottom = ((maxY + paddingY) * scaleY).roundToInt().coerceIn(0, imageHeight - 1)
                val normalizedRight = max(right, left + 1).coerceAtMost(imageWidth - 1)
                val normalizedBottom = max(bottom, top + 1).coerceAtMost(imageHeight - 1)
                Hold(
                    points = listOf(
                        HoldPoint(left, top),
                        HoldPoint(normalizedRight, top),
                        HoldPoint(normalizedRight, normalizedBottom),
                        HoldPoint(left, normalizedBottom)
                    )
                )
            }
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

    private data class HsvColor(
        val h: Float,
        val s: Float,
        val v: Float
    )

    private data class HsvBackgroundModel(
        val meanHue: Float,
        val meanSaturation: Float,
        val meanValue: Float
    )

    private data class HsvAccumulator(
        var count: Int = 0,
        var sinSum: Double = 0.0,
        var cosSum: Double = 0.0,
        var saturationSum: Double = 0.0,
        var valueSum: Double = 0.0
    ) {
        fun add(color: HsvColor) {
            val radians = color.h.toDouble() * PI / 180.0
            sinSum += sin(radians)
            cosSum += cos(radians)
            saturationSum += color.s.toDouble()
            valueSum += color.v.toDouble()
            count += 1
        }

        fun meanColor(): HsvColor {
            if (count == 0) return HsvColor(h = 0f, s = 0f, v = 0f)
            val meanHue = normalizeHueDegrees(
                Math.toDegrees(atan2(sinSum / count.toDouble(), cosSum / count.toDouble())).toFloat()
            )
            return HsvColor(
                h = meanHue,
                s = (saturationSum / count.toDouble()).toFloat(),
                v = (valueSum / count.toDouble()).toFloat()
            )
        }
    }

    private fun sampleBackgroundHsvModel(
        hsvPixels: Array<HsvColor>,
        width: Int,
        height: Int,
        originalWidth: Int,
        originalHeight: Int,
        wallSamplePoints: List<HoldPoint>
    ): HsvBackgroundModel {
        val sampledColors = if (wallSamplePoints.isEmpty()) {
            sampleBorderHsvColors(
                hsvPixels = hsvPixels,
                width = width,
                height = height
            )
        } else {
            wallSamplePoints.mapNotNull { point ->
                val localX = ((point.x.toFloat() / originalWidth.toFloat()) * width.toFloat()).roundToInt()
                val localY = ((point.y.toFloat() / originalHeight.toFloat()) * height.toFloat()).roundToInt()
                sampleHsvPatch(
                    hsvPixels = hsvPixels,
                    width = width,
                    height = height,
                    centerX = localX,
                    centerY = localY
                )
            }
        }
        if (sampledColors.isEmpty()) {
            val fallback = sampleBorderHsvColors(
                hsvPixels = hsvPixels,
                width = width,
                height = height
            )
            if (fallback.isEmpty()) {
                return HsvBackgroundModel(meanHue = 0f, meanSaturation = 0f, meanValue = 0.5f)
            }
            val fallbackMean = averageHsvColors(fallback)
            return HsvBackgroundModel(
                meanHue = fallbackMean.h,
                meanSaturation = fallbackMean.s,
                meanValue = fallbackMean.v
            )
        }
        val meanColor = averageHsvColors(sampledColors)
        return HsvBackgroundModel(
            meanHue = meanColor.h,
            meanSaturation = meanColor.s,
            meanValue = meanColor.v
        )
    }

    private fun sampleBorderHsvColors(
        hsvPixels: Array<HsvColor>,
        width: Int,
        height: Int
    ): List<HsvColor> {
        val marginX = max(4, width / 18)
        val marginY = max(4, height / 18)
        val colors = ArrayList<HsvColor>()
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < marginX || x >= width - marginX || y < marginY || y >= height - marginY) {
                    colors += hsvPixels[y * width + x]
                }
            }
        }
        return colors
    }

    private fun sampleHsvPatch(
        hsvPixels: Array<HsvColor>,
        width: Int,
        height: Int,
        centerX: Int,
        centerY: Int
    ): HsvColor? {
        val radius = 4
        val startX = (centerX - radius).coerceIn(0, width - 1)
        val endX = (centerX + radius).coerceIn(0, width - 1)
        val startY = (centerY - radius).coerceIn(0, height - 1)
        val endY = (centerY + radius).coerceIn(0, height - 1)
        val accumulator = HsvAccumulator()
        for (y in startY..endY) {
            for (x in startX..endX) {
                accumulator.add(hsvPixels[y * width + x])
            }
        }
        return if (accumulator.count == 0) null else accumulator.meanColor()
    }

    private fun averageHsvColors(colors: List<HsvColor>): HsvColor {
        val accumulator = HsvAccumulator()
        colors.forEach(accumulator::add)
        return accumulator.meanColor()
    }

    private fun rgbToHsv(pixel: Int): HsvColor {
        val hsv = FloatArray(3)
        Color.RGBToHSV(
            pixel shr 16 and 0xFF,
            pixel shr 8 and 0xFF,
            pixel and 0xFF,
            hsv
        )
        return HsvColor(
            h = hsv[0],
            s = hsv[1],
            v = hsv[2]
        )
    }

    private fun calculateBackgroundDistanceScore(
        color: HsvColor,
        backgroundModel: HsvBackgroundModel
    ): Float {
        val hueComponentWeight = if (
            backgroundModel.meanSaturation < 0.12f &&
            color.s < 0.2f
        ) {
            0.18f
        } else {
            0.5f
        }
        val saturationComponentWeight = 0.25f
        val valueComponentWeight = 1f - hueComponentWeight - saturationComponentWeight
        val hueScore = (hueDistanceDegrees(color.h, backgroundModel.meanHue) / 180f).coerceIn(0f, 1f)
        val saturationScore = abs(color.s - backgroundModel.meanSaturation).coerceIn(0f, 1f)
        val valueScore = abs(color.v - backgroundModel.meanValue).coerceIn(0f, 1f)
        return (
            hueScore * hueComponentWeight +
                saturationScore * saturationComponentWeight +
                valueScore * valueComponentWeight
            ).coerceIn(0f, 1f)
    }

    private fun calculateHoldColorScore(
        color: HsvColor,
        selectedColors: Set<HoldColorCategory>,
        tuning: AutoExtractionTuning
    ): Float {
        return selectedColors.maxOfOrNull { category ->
            calculateCategoryScore(
                color = color,
                category = category,
                tuning = tuning
            )
        } ?: 0f
    }

    private fun calculateCategoryScore(
        color: HsvColor,
        category: HoldColorCategory,
        tuning: AutoExtractionTuning
    ): Float {
        return when (category) {
            HoldColorCategory.WHITE -> calculateWhiteCategoryScore(color, tuning)
            HoldColorCategory.BLACK -> calculateBlackCategoryScore(color, tuning)
            HoldColorCategory.GRAY -> calculateGrayCategoryScore(color, tuning)
            else -> calculateChromaticCategoryScore(color, category, tuning)
        }
    }

    private fun calculateChromaticCategoryScore(
        color: HsvColor,
        category: HoldColorCategory,
        tuning: AutoExtractionTuning
    ): Float {
        val hueCenter = category.hueCenter ?: return 0f
        val hueTolerance = tuning.hueTolerance.coerceIn(8f, 180f)
        val valueTolerance = tuning.valueTolerance.coerceIn(0.05f, 1f)
        val hueScore = (1f - hueDistanceDegrees(color.h, hueCenter) / hueTolerance).coerceIn(0f, 1f)
        val valueScore = (1f - abs(color.v - category.referenceValue) / valueTolerance).coerceIn(0f, 1f)
        val saturationFloor = tuning.saturationMin.coerceIn(0f, 1f)
        val saturationDenominator = (1f - saturationFloor).coerceAtLeast(0.1f)
        val saturationScore = ((color.s - saturationFloor) / saturationDenominator).coerceIn(0f, 1f)
        return ((hueScore * 0.7f + valueScore * 0.3f) * (0.35f + saturationScore * 0.65f)).coerceIn(0f, 1f)
    }

    private fun calculateWhiteCategoryScore(
        color: HsvColor,
        tuning: AutoExtractionTuning
    ): Float {
        val saturationCeiling = (tuning.saturationMin + 0.24f).coerceIn(0.12f, 1f)
        val valueFloor = (HoldColorCategory.WHITE.referenceValue - tuning.valueTolerance * 1.2f)
            .coerceIn(0.05f, 0.96f)
        val saturationScore = (1f - color.s / saturationCeiling).coerceIn(0f, 1f)
        val valueScore = ((color.v - valueFloor) / (1f - valueFloor).coerceAtLeast(0.08f)).coerceIn(0f, 1f)
        return (saturationScore * 0.45f + valueScore * 0.55f).coerceIn(0f, 1f)
    }

    private fun calculateBlackCategoryScore(
        color: HsvColor,
        tuning: AutoExtractionTuning
    ): Float {
        val maxValue = (HoldColorCategory.BLACK.referenceValue + tuning.valueTolerance).coerceIn(0.08f, 1f)
        val saturationCeiling = (tuning.saturationMin + 0.35f).coerceAtLeast(0.2f)
        val darknessScore = ((maxValue - color.v) / maxValue.coerceAtLeast(0.08f)).coerceIn(0f, 1f)
        val lowSaturationScore = (1f - color.s / saturationCeiling).coerceIn(0f, 1f)
        return (darknessScore * 0.75f + lowSaturationScore * 0.25f).coerceIn(0f, 1f)
    }

    private fun calculateGrayCategoryScore(
        color: HsvColor,
        tuning: AutoExtractionTuning
    ): Float {
        val valueTolerance = (tuning.valueTolerance * 1.15f).coerceIn(0.08f, 1f)
        val saturationCeiling = (tuning.saturationMin + 0.22f).coerceIn(0.08f, 0.65f)
        val valueScore = (
            1f - abs(color.v - HoldColorCategory.GRAY.referenceValue) / valueTolerance
            ).coerceIn(0f, 1f)
        val lowSaturationScore = (1f - color.s / saturationCeiling).coerceIn(0f, 1f)
        return (valueScore * 0.65f + lowSaturationScore * 0.35f).coerceIn(0f, 1f)
    }

    private fun hueDistanceDegrees(first: Float, second: Float): Float {
        val rawDistance = abs(normalizeHueDegrees(first) - normalizeHueDegrees(second))
        return min(rawDistance, 360f - rawDistance)
    }

    private fun normalizeHueDegrees(hue: Float): Float {
        var normalizedHue = hue % 360f
        if (normalizedHue < 0f) {
            normalizedHue += 360f
        }
        return normalizedHue
    }

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

    private fun quantizeLab(lab: LabColor): Int {
        val lBucket = (lab.l / 8.0).roundToInt().coerceIn(0, 13)
        val aBucket = ((lab.a + 128.0) / 16.0).roundToInt().coerceIn(0, 16)
        val bBucket = ((lab.b + 128.0) / 16.0).roundToInt().coerceIn(0, 16)
        return (lBucket shl 16) or (aBucket shl 8) or bBucket
    }

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

    private fun buildRoundedSilhouette(
        points: List<FloatPoint>,
        componentWidth: Int,
        componentHeight: Int
    ): List<FloatPoint> {
        if (points.size < 3) return points.distinct()

        val hull = buildConvexHull(points)
        val padding = max(2f, min(componentWidth, componentHeight) * 0.12f)
        val expanded = expandPolygonOutward(hull, padding)
        val rounded = chaikinSmooth(expanded, iterations = 2)
        val simplified = thinBoundaryPoints(
            points = rounded,
            minDistance = max(1.5f, min(componentWidth, componentHeight) * 0.04f)
        )
        return if (simplified.size >= 3) simplified else hull
    }

    private fun buildConvexHull(points: List<FloatPoint>): List<FloatPoint> {
        val sorted = points
            .distinct()
            .sortedWith(compareBy<FloatPoint> { it.x }.thenBy { it.y })
        if (sorted.size <= 3) return sorted

        val lower = mutableListOf<FloatPoint>()
        for (point in sorted) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), point) <= 0f) {
                lower.removeAt(lower.lastIndex)
            }
            lower += point
        }

        val upper = mutableListOf<FloatPoint>()
        for (index in sorted.indices.reversed()) {
            val point = sorted[index]
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), point) <= 0f) {
                upper.removeAt(upper.lastIndex)
            }
            upper += point
        }

        lower.removeAt(lower.lastIndex)
        upper.removeAt(upper.lastIndex)
        return (lower + upper).distinct()
    }

    private fun expandPolygonOutward(
        points: List<FloatPoint>,
        padding: Float
    ): List<FloatPoint> {
        if (points.size < 3) return points
        val centroid = calculatePolygonCentroid(points)
        return points.map { point ->
            val dx = point.x - centroid.x
            val dy = point.y - centroid.y
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < 0.001f) {
                point
            } else {
                val scale = (distance + padding) / distance
                FloatPoint(
                    x = centroid.x + dx * scale,
                    y = centroid.y + dy * scale
                )
            }
        }
    }

    private fun calculatePolygonCentroid(points: List<FloatPoint>): FloatPoint {
        var signedArea = 0f
        var centerX = 0f
        var centerY = 0f
        for (index in points.indices) {
            val current = points[index]
            val next = points[(index + 1) % points.size]
            val cross = current.x * next.y - next.x * current.y
            signedArea += cross
            centerX += (current.x + next.x) * cross
            centerY += (current.y + next.y) * cross
        }

        if (abs(signedArea) < 0.001f) {
            return FloatPoint(
                x = points.map { it.x }.average().toFloat(),
                y = points.map { it.y }.average().toFloat()
            )
        }

        val factor = 1f / (3f * signedArea)
        return FloatPoint(
            x = centerX * factor,
            y = centerY * factor
        )
    }

    private fun chaikinSmooth(
        points: List<FloatPoint>,
        iterations: Int
    ): List<FloatPoint> {
        var current = points
        repeat(iterations.coerceAtLeast(0)) {
            if (current.size < 3) return current
            val refined = mutableListOf<FloatPoint>()
            for (index in current.indices) {
                val currentPoint = current[index]
                val nextPoint = current[(index + 1) % current.size]
                refined += FloatPoint(
                    x = currentPoint.x * 0.75f + nextPoint.x * 0.25f,
                    y = currentPoint.y * 0.75f + nextPoint.y * 0.25f
                )
                refined += FloatPoint(
                    x = currentPoint.x * 0.25f + nextPoint.x * 0.75f,
                    y = currentPoint.y * 0.25f + nextPoint.y * 0.75f
                )
            }
            current = refined
        }
        return current
    }

    private fun cross(
        origin: FloatPoint,
        first: FloatPoint,
        second: FloatPoint
    ): Float {
        return (first.x - origin.x) * (second.y - origin.y) -
            (first.y - origin.y) * (second.x - origin.x)
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
