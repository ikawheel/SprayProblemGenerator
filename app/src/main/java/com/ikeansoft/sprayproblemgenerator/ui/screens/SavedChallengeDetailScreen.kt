package com.ikeansoft.sprayproblemgenerator.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.ui.AppSecondaryTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSubtleSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState
import com.ikeansoft.sprayproblemgenerator.ui.RouteSelectionMode
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.canvas.ChallengeCanvasScreen
import com.ikeansoft.sprayproblemgenerator.ui.selectors.deriveChallengeCreatorUiModel
import kotlin.math.roundToInt

@Composable
fun SavedChallengeDetailScreen(
    state: MainUiState,
    onSaveChallengeImage: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveChallengeCreatorUiModel(state)
    val navigationBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val rootView = LocalView.current
    var challengeBounds by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 20.dp + navigationBarBottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bitmap == null) {
            Text(
                text = stringResource(R.string.message_open_saved_challenge_failed),
                color = AppTextColor,
                style = MaterialTheme.typography.bodyLarge
            )
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(AppSubtleSurfaceColor, MaterialTheme.shapes.large)
                .clipToBounds()
                .onGloballyPositioned { coordinates ->
                    challengeBounds = coordinates.boundsInRoot()
                }
        ) {
            ChallengeCanvasScreen(
                bitmap = bitmap,
                holds = state.holds,
                selectedIndex = null,
                challengeHoldIndices = state.challengeHoldIndices,
                challengeOrderedHoldIndices = uiModel.orderedChallengeIndices,
                selectionCandidateIndices = emptySet(),
                hasDrawTargetSelection = false,
                startHoldIndex = state.startHoldIndex,
                goalHoldIndex = state.goalHoldIndex,
                coreChallengeHoldIndex = uiModel.coreChallengeHoldIndex,
                routeSelectionMode = RouteSelectionMode.NONE,
                isDrawTargetSelectionMode = false,
                useDefaultChallengeHoldOutlineColor = true,
                showChallengeOrderLabels = false,
                displayColorSettings = state.displayColorSettings,
                onHoldTapped = {},
                onDrawTargetSelectionCompleted = {},
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = stringResource(R.string.saved_challenge_hold_count_label, state.challengeHoldIndices.size),
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium
        )

        AppButton(
            onClick = {
                val capturedBitmap = challengeBounds?.let { bounds ->
                    captureChallengeAreaBitmap(
                        rootBitmap = rootView.drawToBitmap(),
                        bounds = bounds
                    )
                }
                onSaveChallengeImage(capturedBitmap)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_challenge_image))
        }
    }
}

private fun captureChallengeAreaBitmap(
    rootBitmap: Bitmap,
    bounds: Rect
): Bitmap? {
    if (rootBitmap.width <= 0 || rootBitmap.height <= 0) {
        return null
    }

    val left = bounds.left.roundToInt().coerceIn(0, rootBitmap.width)
    val top = bounds.top.roundToInt().coerceIn(0, rootBitmap.height)
    val right = bounds.right.roundToInt().coerceIn(0, rootBitmap.width)
    val bottom = bounds.bottom.roundToInt().coerceIn(0, rootBitmap.height)
    val width = (right - left).coerceAtLeast(1)
    val height = (bottom - top).coerceAtLeast(1)
    val safeWidth = width.coerceAtMost(rootBitmap.width - left)
    val safeHeight = height.coerceAtMost(rootBitmap.height - top)
    if (safeWidth <= 0 || safeHeight <= 0) {
        return null
    }

    return Bitmap.createBitmap(rootBitmap, left, top, safeWidth, safeHeight)
}
