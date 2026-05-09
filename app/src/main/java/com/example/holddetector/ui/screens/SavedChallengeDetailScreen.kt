package com.example.holddetector.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.RouteSelectionMode
import com.example.holddetector.ui.canvas.ChallengeCanvasScreen
import com.example.holddetector.ui.selectors.deriveChallengeCreatorUiModel

@Composable
fun SavedChallengeDetailScreen(
    state: MainUiState,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveChallengeCreatorUiModel(state)
    val navigationBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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
    }
}
