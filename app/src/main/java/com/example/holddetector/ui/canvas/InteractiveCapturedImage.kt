package com.example.holddetector.ui.canvas

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.holddetector.R
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.model.ReachCalibrationReference
import com.example.holddetector.ui.AppBackgroundColor
import com.example.holddetector.ui.AppBusyOverlayColor
import com.example.holddetector.ui.AppCoreLabelBackgroundColor
import com.example.holddetector.ui.AppOverlayStrokePreviewColor
import com.example.holddetector.ui.AppStartGoalLabelBackgroundColor
import com.example.holddetector.ui.DefaultHoldStrokeWidth
import com.example.holddetector.ui.HoldTapAreaSize
import com.example.holddetector.ui.RouteSelectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private enum class CanvasMode {
    HOLD_EDITOR,
    HOLD_ATTRIBUTE_EDITOR,
    CHALLENGE,
    REACH_CALIBRATION,
    SCORING
}

private data class PendingDrawTargetSelectionRequest(
    val movedEnough: Boolean,
    val startLocal: Offset,
    val strokePoints: List<Offset>,
    val baseLayout: BaseImageLayout,
    val holds: List<Hold>
)

@Composable
fun ReachCalibrationCanvasScreen(
    bitmap: Bitmap,
    reachCalibrationReference: ReachCalibrationReference?,
    pendingReachCalibrationPoint: HoldPoint?,
    isReachCalibrationSelectionMode: Boolean,
    onReachCalibrationPointSelected: (HoldPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveCapturedImage(
        bitmap = bitmap,
        holds = emptyList(),
        selectedIndex = null,
        challengeHoldIndices = emptySet(),
        selectionCandidateIndices = emptySet(),
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        reachCalibrationReference = reachCalibrationReference,
        pendingReachCalibrationPoint = pendingReachCalibrationPoint,
        isReachCalibrationSelectionMode = isReachCalibrationSelectionMode,
        isDrawTargetSelectionMode = false,
        mode = CanvasMode.REACH_CALIBRATION,
        onHoldTapped = {},
        onReachCalibrationPointSelected = onReachCalibrationPointSelected,
        onDrawTargetSelectionCompleted = {},
        onManualHoldCreated = {},
        modifier = modifier
    )
}

@Composable
fun HoldCanvasScreen(
    bitmap: Bitmap,
    holds: List<Hold>,
    selectedIndex: Int?,
    challengeHoldIndices: Set<Int>,
    startCandidateHoldIndices: Set<Int> = emptySet(),
    goalCandidateHoldIndices: Set<Int> = emptySet(),
    challengeOrderedHoldIndices: List<Int> = emptyList(),
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    routeSelectionMode: RouteSelectionMode,
    reachCalibrationReference: ReachCalibrationReference?,
    pendingReachCalibrationPoint: HoldPoint?,
    isReachCalibrationSelectionMode: Boolean,
    holdTapAreaSize: HoldTapAreaSize = HoldTapAreaSize.MEDIUM,
    isDrawTargetSelectionMode: Boolean = false,
    onHoldTapped: (Int?) -> Unit,
    onReachCalibrationPointSelected: (HoldPoint) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit = {},
    onManualHoldCreated: (Hold) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveCapturedImage(
        bitmap = bitmap,
        holds = holds,
        selectedIndex = selectedIndex,
        challengeHoldIndices = challengeHoldIndices,
        startCandidateHoldIndices = startCandidateHoldIndices,
        goalCandidateHoldIndices = goalCandidateHoldIndices,
        challengeOrderedHoldIndices = challengeOrderedHoldIndices,
        startHoldIndex = startHoldIndex,
        goalHoldIndex = goalHoldIndex,
        routeSelectionMode = routeSelectionMode,
        reachCalibrationReference = reachCalibrationReference,
        pendingReachCalibrationPoint = pendingReachCalibrationPoint,
        isReachCalibrationSelectionMode = isReachCalibrationSelectionMode,
        holdTapAreaSize = holdTapAreaSize,
        isDrawTargetSelectionMode = isDrawTargetSelectionMode,
        mode = CanvasMode.HOLD_EDITOR,
        onHoldTapped = onHoldTapped,
        onReachCalibrationPointSelected = onReachCalibrationPointSelected,
        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
        onManualHoldCreated = onManualHoldCreated,
        modifier = modifier
    )
}

@Composable
fun HoldAttributeCanvasScreen(
    bitmap: Bitmap,
    holds: List<Hold>,
    selectedIndex: Int?,
    startCandidateHoldIndices: Set<Int>,
    goalCandidateHoldIndices: Set<Int>,
    onHoldTapped: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveCapturedImage(
        bitmap = bitmap,
        holds = holds,
        selectedIndex = selectedIndex,
        challengeHoldIndices = emptySet(),
        startCandidateHoldIndices = startCandidateHoldIndices,
        goalCandidateHoldIndices = goalCandidateHoldIndices,
        challengeOrderedHoldIndices = emptyList(),
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        reachCalibrationReference = null,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        isDrawTargetSelectionMode = false,
        mode = CanvasMode.HOLD_ATTRIBUTE_EDITOR,
        onHoldTapped = onHoldTapped,
        onReachCalibrationPointSelected = {},
        onDrawTargetSelectionCompleted = {},
        onManualHoldCreated = {},
        modifier = modifier
    )
}

@Composable
fun ChallengeCanvasScreen(
    bitmap: Bitmap,
    holds: List<Hold>,
    selectedIndex: Int?,
    challengeHoldIndices: Set<Int>,
    challengeOrderedHoldIndices: List<Int>,
    selectionCandidateIndices: Set<Int>,
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    coreChallengeHoldIndex: Int?,
    routeSelectionMode: RouteSelectionMode,
    isDrawTargetSelectionMode: Boolean,
    onHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveCapturedImage(
        bitmap = bitmap,
        holds = holds,
        selectedIndex = selectedIndex,
        challengeHoldIndices = challengeHoldIndices,
        challengeOrderedHoldIndices = challengeOrderedHoldIndices,
        selectionCandidateIndices = selectionCandidateIndices,
        startHoldIndex = startHoldIndex,
        goalHoldIndex = goalHoldIndex,
        coreChallengeHoldIndex = coreChallengeHoldIndex,
        routeSelectionMode = routeSelectionMode,
        reachCalibrationReference = null,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        isDrawTargetSelectionMode = isDrawTargetSelectionMode,
        mode = CanvasMode.CHALLENGE,
        onHoldTapped = onHoldTapped,
        onReachCalibrationPointSelected = {},
        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
        onManualHoldCreated = {},
        modifier = modifier
    )
}

@Composable
fun HoldScoringCanvasScreen(
    bitmap: Bitmap,
    holds: List<Hold>,
    currentHoldIndex: Int?,
    modifier: Modifier = Modifier
) {
    InteractiveCapturedImage(
        bitmap = bitmap,
        holds = holds,
        selectedIndex = currentHoldIndex,
        challengeHoldIndices = emptySet(),
        challengeOrderedHoldIndices = emptyList(),
        selectionCandidateIndices = emptySet(),
        startHoldIndex = null,
        goalHoldIndex = null,
        routeSelectionMode = RouteSelectionMode.NONE,
        reachCalibrationReference = null,
        pendingReachCalibrationPoint = null,
        isReachCalibrationSelectionMode = false,
        isDrawTargetSelectionMode = false,
        focusHoldIndex = currentHoldIndex,
        mode = CanvasMode.SCORING,
        onHoldTapped = {},
        onReachCalibrationPointSelected = {},
        onDrawTargetSelectionCompleted = {},
        onManualHoldCreated = {},
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InteractiveCapturedImage(
    bitmap: Bitmap,
    holds: List<Hold>,
    selectedIndex: Int?,
    challengeHoldIndices: Set<Int>,
    startCandidateHoldIndices: Set<Int> = emptySet(),
    goalCandidateHoldIndices: Set<Int> = emptySet(),
    challengeOrderedHoldIndices: List<Int> = emptyList(),
    selectionCandidateIndices: Set<Int> = emptySet(),
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    coreChallengeHoldIndex: Int? = null,
    routeSelectionMode: RouteSelectionMode,
    reachCalibrationReference: ReachCalibrationReference? = null,
    pendingReachCalibrationPoint: HoldPoint? = null,
    isReachCalibrationSelectionMode: Boolean = false,
    holdTapAreaSize: HoldTapAreaSize = HoldTapAreaSize.MEDIUM,
    isDrawTargetSelectionMode: Boolean,
    focusHoldIndex: Int? = null,
    mode: CanvasMode,
    onHoldTapped: (Int?) -> Unit,
    onReachCalibrationPointSelected: (HoldPoint) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var activePointerCount by remember { mutableIntStateOf(0) }
    var isSelectionProcessing by remember { mutableStateOf(false) }
    var pendingDrawTargetSelectionRequest by remember {
        mutableStateOf<PendingDrawTargetSelectionRequest?>(null)
    }
    var draftPreviewPolygon by remember { mutableStateOf<LocalHoldPolygon?>(null) }
    var draftStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val density = LocalDensity.current
    val holdEditorTapMoveThresholdLocal = 10f
    val holdAreaScale = when (holdTapAreaSize) {
        HoldTapAreaSize.SMALL -> 0.5f
        HoldTapAreaSize.MEDIUM -> 1f
        HoldTapAreaSize.LARGE -> 1.5f
    }
    val brushRadiusXLocal = 10f * holdAreaScale
    val brushRadiusYLocal = 8f * holdAreaScale
    val tapRadiusXLocal = 10f * holdAreaScale
    val tapRadiusYLocal = 8f * holdAreaScale
    val labelPaint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 15f
            isAntiAlias = true
        }
    }
    val challengeOrderLabels = remember(challengeOrderedHoldIndices) {
        challengeOrderedHoldIndices
            .distinct()
            .withIndex()
            .associate { (position, holdIndex) -> holdIndex to (position + 1).toString() }
    }
    val startMarkerLabel = stringResource(R.string.challenge_start_marker)
    val goalMarkerLabel = stringResource(R.string.challenge_goal_marker)

    val baseLayout = remember(bitmap.width, bitmap.height, containerSize) {
        calculateBaseImageLayout(
            containerWidth = containerSize.width.toFloat(),
            containerHeight = containerSize.height.toFloat(),
            imageWidth = bitmap.width.toFloat(),
            imageHeight = bitmap.height.toFloat()
        )
    }
    val shouldShowChallengeSelectionMask =
        mode == CanvasMode.CHALLENGE &&
            routeSelectionMode == RouteSelectionMode.NONE &&
            !isDrawTargetSelectionMode &&
            challengeHoldIndices.isNotEmpty()
    val highlightedHoldPath = remember(
        holds,
        challengeHoldIndices,
        baseLayout,
        mode,
        routeSelectionMode,
        isDrawTargetSelectionMode,
        focusHoldIndex
    ) {
        if (!baseLayout.isValid) return@remember null

        when {
            mode == CanvasMode.SCORING -> {
                focusHoldIndex
                    ?.let { index -> holds.getOrNull(index) }
                    ?.toLocalPolygon(baseLayout)
                    ?.toPath()
            }

            shouldShowChallengeSelectionMask -> {
                Path().apply {
                    challengeHoldIndices.sorted().forEach { index ->
                        holds.getOrNull(index)?.let { hold ->
                            addPath(hold.toLocalPolygon(baseLayout).toPath())
                        }
                    }
                }
            }

            else -> null
        }
    }
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }
    val shouldShowHighlightMask =
        (mode == CanvasMode.CHALLENGE || mode == CanvasMode.SCORING) &&
            highlightedHoldPath != null

    LaunchedEffect(mode, focusHoldIndex, baseLayout, containerSize, holds) {
        if (mode != CanvasMode.SCORING || !baseLayout.isValid) return@LaunchedEffect

        val focusHold = focusHoldIndex?.let { index -> holds.getOrNull(index) }
            ?: return@LaunchedEffect
        val polygon = focusHold.toLocalPolygon(baseLayout)
        val containerWidth = containerSize.width.toFloat().coerceAtLeast(1f)
        val containerHeight = containerSize.height.toFloat().coerceAtLeast(1f)
        val holdWidth = max(polygon.maxX - polygon.minX, 36f)
        val holdHeight = max(polygon.maxY - polygon.minY, 36f)
        val targetZoom = minOf(
            containerWidth * 0.48f / holdWidth,
            containerHeight * 0.48f / holdHeight
        ).coerceIn(1f, 5f)
        val holdCenter = Offset(
            x = (polygon.minX + polygon.maxX) / 2f,
            y = (polygon.minY + polygon.maxY) / 2f
        )
        val targetPan = Offset(
            x = containerWidth / 2f - baseLayout.left - holdCenter.x * targetZoom,
            y = containerHeight / 2f - baseLayout.top - holdCenter.y * targetZoom
        )

        zoomScale = targetZoom
        panOffset = clampPanOffset(
            candidate = targetPan,
            containerSize = containerSize,
            baseLayout = baseLayout,
            zoomScale = targetZoom
        )
    }

    LaunchedEffect(pendingDrawTargetSelectionRequest) {
        val request = pendingDrawTargetSelectionRequest ?: return@LaunchedEffect
        isSelectionProcessing = true
        try {
            val selectedIndices = withContext(Dispatchers.Default) {
                val selectionPolygon = if (request.movedEnough) {
                    buildContourPolygonFromBrushPoints(
                        points = request.strokePoints,
                        brushRadiusX = brushRadiusXLocal,
                        brushRadiusY = brushRadiusYLocal,
                        baseLayout = request.baseLayout
                    )
                } else {
                    buildContourPolygonFromBrushPoints(
                        points = listOf(request.startLocal),
                        brushRadiusX = tapRadiusXLocal,
                        brushRadiusY = tapRadiusYLocal,
                        baseLayout = request.baseLayout
                    )
                }

                selectionPolygon?.let { polygon ->
                    findHoldIndicesIntersectingSelectionPolygon(
                        selectionPolygon = polygon,
                        holds = request.holds,
                        baseLayout = request.baseLayout
                    )
                } ?: emptySet()
            }

            onDrawTargetSelectionCompleted(selectedIndices)
        } finally {
            pendingDrawTargetSelectionRequest = null
            isSelectionProcessing = false
        }
    }

    Box(
        modifier = modifier
            .background(AppBackgroundColor)
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            .pointerInput(baseLayout, containerSize) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (!baseLayout.isValid) return@detectTransformGestures

                    val previousScale = zoomScale
                    val updatedScale = (previousScale * zoom).coerceIn(1f, 5f)
                    val zoomFactor = updatedScale / previousScale
                    val contentOrigin = Offset(baseLayout.left, baseLayout.top)

                    val candidatePanOffset = if (activePointerCount >= 2) {
                        (panOffset * zoomFactor) +
                            pan +
                            ((centroid - contentOrigin) * (1f - zoomFactor))
                    } else {
                        panOffset
                    }

                    zoomScale = updatedScale
                    panOffset = clampPanOffset(
                        candidate = candidatePanOffset,
                        containerSize = containerSize,
                        baseLayout = baseLayout,
                        zoomScale = updatedScale
                    )
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        activePointerCount = event.changes.count { it.pressed }
                    } while (activePointerCount > 0)
                    activePointerCount = 0
                }
            }
            .pointerInput(
                baseLayout,
                zoomScale,
                panOffset,
                holds,
                routeSelectionMode,
                reachCalibrationReference,
                pendingReachCalibrationPoint,
                isReachCalibrationSelectionMode,
                isDrawTargetSelectionMode,
                mode
            ) {
                awaitEachGesture {
                    if (isSelectionProcessing) {
                        return@awaitEachGesture
                    }

                    val down = awaitFirstDown(requireUnconsumed = false)

                    if (!baseLayout.isValid || activePointerCount > 1) {
                        draftPreviewPolygon = null
                        draftStrokePoints = emptyList()
                        return@awaitEachGesture
                    }

                    val startLocal = screenToLocalPoint(
                        screenPoint = down.position,
                        baseLayout = baseLayout,
                        panOffset = panOffset,
                        zoomScale = zoomScale
                    )

                    if (!isInsideLocalBounds(startLocal, baseLayout)) {
                        draftPreviewPolygon = null
                        draftStrokePoints = emptyList()
                        return@awaitEachGesture
                    }

                    if (mode == CanvasMode.SCORING) {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) break
                        }
                        draftPreviewPolygon = null
                        draftStrokePoints = emptyList()
                        return@awaitEachGesture
                    }

                    if (mode == CanvasMode.REACH_CALIBRATION) {
                        if (!isReachCalibrationSelectionMode) {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                            }
                            draftPreviewPolygon = null
                            draftStrokePoints = emptyList()
                            return@awaitEachGesture
                        }

                        var movedEnough = false
                        var multiTouchDetected = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size > 1) {
                                multiTouchDetected = true
                                break
                            }
                            val currentLocal = screenToLocalPoint(
                                screenPoint = pressed.first().position,
                                baseLayout = baseLayout,
                                panOffset = panOffset,
                                zoomScale = zoomScale
                            )
                            val dx = currentLocal.x - startLocal.x
                            val dy = currentLocal.y - startLocal.y
                            if (
                                abs(dx) > holdEditorTapMoveThresholdLocal ||
                                    abs(dy) > holdEditorTapMoveThresholdLocal
                            ) {
                                movedEnough = true
                            }
                        }
                        if (!multiTouchDetected && !movedEnough) {
                            onReachCalibrationPointSelected(
                                localOffsetToImagePoint(
                                    localPoint = startLocal,
                                    baseLayout = baseLayout,
                                    imageWidth = bitmap.width,
                                    imageHeight = bitmap.height
                                )
                            )
                        }
                        draftPreviewPolygon = null
                        draftStrokePoints = emptyList()
                        return@awaitEachGesture
                    }

                    if (
                        (mode == CanvasMode.CHALLENGE && !isDrawTargetSelectionMode) ||
                            mode == CanvasMode.HOLD_ATTRIBUTE_EDITOR
                    ) {
                        var movedEnough = false
                        var multiTouchDetected = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size > 1) {
                                multiTouchDetected = true
                                break
                            }
                            val currentLocal = screenToLocalPoint(
                                screenPoint = pressed.first().position,
                                baseLayout = baseLayout,
                                panOffset = panOffset,
                                zoomScale = zoomScale
                            )
                            val dx = currentLocal.x - startLocal.x
                            val dy = currentLocal.y - startLocal.y
                            if (abs(dx) > 4f || abs(dy) > 4f) {
                                movedEnough = true
                            }
                        }
                        if (!multiTouchDetected && !movedEnough) {
                            onHoldTapped(
                                findTappedIndexFromLocal(
                                    localPoint = startLocal,
                                    holds = holds,
                                    baseLayout = baseLayout
                                )
                            )
                        }
                        return@awaitEachGesture
                    }

                    var movedEnough = false
                    var multiTouchDetected = false
                    draftPreviewPolygon = null
                    draftStrokePoints = listOf(startLocal)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            break
                        }

                        if (pressed.size > 1) {
                            multiTouchDetected = true
                            draftPreviewPolygon = null
                            draftStrokePoints = emptyList()
                            break
                        }

                        val currentLocal = screenToLocalPoint(
                            screenPoint = pressed.first().position,
                            baseLayout = baseLayout,
                            panOffset = panOffset,
                            zoomScale = zoomScale
                        )

                        val dx = currentLocal.x - startLocal.x
                        val dy = currentLocal.y - startLocal.y

                        if (
                            abs(dx) > holdEditorTapMoveThresholdLocal ||
                                abs(dy) > holdEditorTapMoveThresholdLocal
                        ) {
                            movedEnough = true
                            val updatedPoints = appendPointIfNeeded(
                                draftStrokePoints,
                                currentLocal,
                                minDistance = 3f,
                                baseLayout = baseLayout
                            )
                            draftStrokePoints = updatedPoints
                            draftPreviewPolygon = buildContourPolygonFromBrushPoints(
                                points = updatedPoints,
                                brushRadiusX = brushRadiusXLocal,
                                brushRadiusY = brushRadiusYLocal,
                                baseLayout = baseLayout
                            )
                        }
                    }

                    if (!multiTouchDetected) {
                        if (mode == CanvasMode.CHALLENGE && isDrawTargetSelectionMode) {
                            pendingDrawTargetSelectionRequest = PendingDrawTargetSelectionRequest(
                                movedEnough = movedEnough,
                                startLocal = startLocal,
                                strokePoints = draftStrokePoints.toList(),
                                baseLayout = baseLayout,
                                holds = holds.toList()
                            )
                        } else if (movedEnough) {
                            createManualHoldFromBrushPoints(
                                points = draftStrokePoints,
                                brushRadiusX = brushRadiusXLocal,
                                brushRadiusY = brushRadiusYLocal,
                                baseLayout = baseLayout,
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height,
                                onManualHoldCreated = onManualHoldCreated
                            )
                        } else {
                            val tappedIndex = findTappedIndexFromLocal(
                                localPoint = startLocal,
                                holds = holds,
                                baseLayout = baseLayout
                            )
                            if (tappedIndex != null) {
                                onHoldTapped(tappedIndex)
                            } else {
                                createTapHoldFromLocalPoint(
                                    localPoint = startLocal,
                                    localRadiusX = tapRadiusXLocal,
                                    localRadiusY = tapRadiusYLocal,
                                    baseLayout = baseLayout,
                                    imageWidth = bitmap.width,
                                    imageHeight = bitmap.height,
                                    onManualHoldCreated = onManualHoldCreated
                                )
                            }
                        }
                    }

                    draftPreviewPolygon = null
                    draftStrokePoints = emptyList()
                }
            }
    ) {
        if (baseLayout.isValid) {
            val transformedOffset = IntOffset(
                (baseLayout.left + panOffset.x).roundToInt(),
                (baseLayout.top + panOffset.y).roundToInt()
            )

            val imageWidthDp = with(density) { baseLayout.drawWidth.toDp() }
            val imageHeightDp = with(density) { baseLayout.drawHeight.toDp() }

            Box(
                modifier = Modifier
                    .offset { transformedOffset }
                    .size(imageWidthDp, imageHeightDp)
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (shouldShowHighlightMask) {
                        grayscaleFilter
                    } else {
                        null
                    }
                )

                if (shouldShowHighlightMask) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val selectedPath = highlightedHoldPath
                                if (selectedPath != null) {
                                    clipPath(selectedPath) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            }
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (mode == CanvasMode.REACH_CALIBRATION) {
                        if (!isReachCalibrationSelectionMode) {
                            reachCalibrationReference?.let { reference ->
                                val first = reference.firstPoint.toLocalOffset(baseLayout)
                                val second = reference.secondPoint.toLocalOffset(baseLayout)
                                drawLine(
                                    color = Color(0xFF2563EB),
                                    start = first,
                                    end = second,
                                    strokeWidth = 4f
                                )
                                drawCircle(
                                    color = Color(0xFF2563EB),
                                    radius = 4f,
                                    center = first
                                )
                                drawCircle(
                                    color = Color(0xFF2563EB),
                                    radius = 4f,
                                    center = second
                                )
                            }
                        }
                        pendingReachCalibrationPoint?.let { pendingPoint ->
                            drawCircle(
                                color = Color(0xFF2563EB),
                                radius = 4f,
                                center = pendingPoint.toLocalOffset(baseLayout)
                            )
                        }
                    }

                    holds.forEachIndexed { index, hold ->
                        val shouldDrawOutline = when {
                            mode == CanvasMode.HOLD_EDITOR || mode == CanvasMode.HOLD_ATTRIBUTE_EDITOR -> true
                            mode == CanvasMode.SCORING -> index == focusHoldIndex
                            mode == CanvasMode.REACH_CALIBRATION -> false
                            routeSelectionMode != RouteSelectionMode.NONE ->
                                selectionCandidateIndices.contains(index) ||
                                    index == startHoldIndex ||
                                    index == goalHoldIndex
                            else ->
                                challengeHoldIndices.contains(index) ||
                                    index == startHoldIndex ||
                                    index == goalHoldIndex
                        }
                        if (!shouldDrawOutline) return@forEachIndexed

                        val polygon = hold.toLocalPolygon(baseLayout)
                        val strokeColor = when {
                            mode == CanvasMode.SCORING -> Color(0xFFDC2626)
                            index == startHoldIndex -> Color(0xFF0284C7)
                            index == goalHoldIndex -> Color(0xFFCA8A04)
                            index == selectedIndex -> Color.Red
                            challengeHoldIndices.contains(index) -> Color.Yellow
                            else -> Color.Green
                        }

                        drawPath(
                            path = polygon.toPath(),
                            color = strokeColor,
                            style = Stroke(width = DefaultHoldStrokeWidth)
                        )

                        val label = buildList {
                            if (mode == CanvasMode.HOLD_EDITOR || mode == CanvasMode.HOLD_ATTRIBUTE_EDITOR) {
                                if (startCandidateHoldIndices.contains(index)) add(startMarkerLabel)
                                if (goalCandidateHoldIndices.contains(index)) add(goalMarkerLabel)
                            } else {
                                if (index == startHoldIndex) add(startMarkerLabel)
                                if (index == goalHoldIndex) add(goalMarkerLabel)
                            }
                            if (mode == CanvasMode.CHALLENGE && challengeHoldIndices.contains(index)) {
                                challengeOrderLabels[index]?.let(::add)
                            }
                        }.joinToString("/")
                        if (label.isNotEmpty()) {
                            val metrics = labelPaint.fontMetrics
                            val lineHeight = metrics.descent - metrics.ascent
                            val horizontalPadding = 6f
                            val verticalPadding = 4f
                            val labelHeight = verticalPadding * 2f + lineHeight
                            val labelTop = (polygon.minY - labelHeight - 4f).coerceAtLeast(0f)
                            val labelLeft = polygon.minX.coerceAtLeast(0f)
                            val labelWidth = max(labelPaint.measureText(label) + horizontalPadding * 2f, 24f)

                            drawRect(
                                color = if (
                                    mode == CanvasMode.CHALLENGE &&
                                    index == coreChallengeHoldIndex &&
                                    challengeHoldIndices.contains(index)
                                ) {
                                    AppCoreLabelBackgroundColor
                                } else {
                                    AppStartGoalLabelBackgroundColor
                                },
                                topLeft = Offset(labelLeft, labelTop),
                                size = Size(labelWidth, labelHeight)
                            )

                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    label,
                                    labelLeft + horizontalPadding,
                                    labelTop + verticalPadding - metrics.ascent,
                                    labelPaint
                                )
                            }
                        }
                    }

                    if (
                        (mode == CanvasMode.HOLD_EDITOR && routeSelectionMode == RouteSelectionMode.NONE) ||
                            (mode == CanvasMode.CHALLENGE && isDrawTargetSelectionMode)
                    ) {
                        draftStrokePoints.forEach { point ->
                            drawOval(
                                color = AppOverlayStrokePreviewColor,
                                topLeft = Offset(point.x - brushRadiusXLocal, point.y - brushRadiusYLocal),
                                size = Size(
                                    brushRadiusXLocal * 2f,
                                    brushRadiusYLocal * 2f
                                )
                            )
                        }

                        draftPreviewPolygon?.let { polygon ->
                            drawPath(
                                path = polygon.toPath(),
                                color = Color.Cyan,
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                }
            }
        }

        if (isSelectionProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBusyOverlayColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
