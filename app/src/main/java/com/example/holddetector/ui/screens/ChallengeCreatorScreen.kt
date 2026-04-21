package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppCoreHighlightBackgroundColor
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.canvas.ChallengeCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.selectors.DrawTargetStatus
import com.example.holddetector.ui.selectors.deriveChallengeCreatorUiModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ChallengeCreatorScreen(
    state: MainUiState,
    onBackToList: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onStartGoalSelection: () -> Unit,
    onStartDrawTargetSelection: () -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawClick: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onClearChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val scrollState = rememberScrollState()
    val uiModel = deriveChallengeCreatorUiModel(state)
    var isDebugSummaryExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        Text(
            text = stringResource(R.string.challenge_creator_title),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = state.wallTitle,
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                .clipToBounds()
        ) {
            if (bitmap != null) {
                ChallengeCanvasScreen(
                    bitmap = bitmap,
                    holds = state.holds,
                    selectedIndex = state.selectedHoldIndex,
                    challengeHoldIndices = state.challengeHoldIndices,
                    challengeOrderedHoldIndices = uiModel.orderedChallengeIndices,
                    selectionCandidateIndices = uiModel.selectionCandidateIndices,
                    startHoldIndex = state.startHoldIndex,
                    goalHoldIndex = state.goalHoldIndex,
                    coreChallengeHoldIndex = uiModel.coreChallengeHoldIndex,
                    routeSelectionMode = state.routeSelectionMode,
                    isDrawTargetSelectionMode = state.isDrawTargetSelectionMode,
                    onHoldTapped = onChallengeHoldTapped,
                    onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (uiModel.challengeDebugSummaryRows.isNotEmpty()) {
            AppOutlinedButton(
                onClick = { isDebugSummaryExpanded = !isDebugSummaryExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isDebugSummaryExpanded) {
                            R.string.challenge_debug_hide
                        } else {
                            R.string.challenge_debug_show
                        }
                    )
                )
            }

            if (isDebugSummaryExpanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    uiModel.challengeDebugSummaryRows.forEach { debugRow ->
                        val distanceText = debugRow.distanceCentimeters?.let { distanceCentimeters ->
                            stringResource(
                                R.string.challenge_debug_distance_centimeters,
                                distanceCentimeters
                            )
                        } ?: stringResource(R.string.challenge_debug_distance_unknown)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (debugRow.isCore) {
                                        AppCoreHighlightBackgroundColor
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.challenge_debug_summary,
                                    debugRow.stepNumber,
                                    formatChallengeDebugNumber(debugRow.totalDifficulty),
                                    distanceText,
                                    debugRow.previousHoldDifficulty,
                                    debugRow.nextHoldDifficulty,
                                    formatChallengeDebugNumber(debugRow.distanceMultiplier)
                                ),
                                color = AppSecondaryTextColor,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(
                R.string.challenge_selection_summary,
                state.challengeHoldIndices.size,
                stringResource(uiModel.startStatusResId),
                stringResource(uiModel.goalStatusResId)
            ),
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        uiModel.challengeDifficultyScore?.let { totalDifficulty ->
            Text(
                text = stringResource(
                    R.string.challenge_difficulty_score_label,
                    formatChallengeDebugNumber(totalDifficulty)
                ),
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            uiModel.coreMoveDifficulty?.let { coreDifficulty ->
                Text(
                    text = stringResource(
                        R.string.challenge_core_move_difficulty_label,
                        formatChallengeDebugNumber(coreDifficulty)
                    ),
                    color = AppSecondaryTextColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.drawCountInput,
                onValueChange = onDrawCountChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.draw_count_label)) },
                placeholder = { Text(stringResource(R.string.draw_count_placeholder_auto)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            AppButton(
                onClick = onDrawClick,
                enabled = uiModel.isReadyToGenerate,
                modifier = Modifier.height(56.dp)
            ) {
                Text(stringResource(R.string.draw))
            }
        }

        Text(
            text = stringResource(R.string.challenge_tuning_title),
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )

        HoldDifficultyRangeSlider(
            label = stringResource(R.string.challenge_difficulty_range_label),
            startValue = state.challengeDifficultyScoreMin.toFloat(),
            endValue = state.challengeDifficultyScoreMax.toFloat(),
            onValueChange = onChallengeDifficultyRangeChange,
            modifier = Modifier.padding(top = 8.dp)
        )

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_detour_strength_label),
            value = state.routeTuning.detourStrength,
            onValueChange = onDetourStrengthChange,
            modifier = Modifier.padding(top = 8.dp)
        )

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_route_waviness_label),
            value = state.routeTuning.routeWaviness,
            onValueChange = onRouteWavinessChange
        )

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_step_distance_variance_label),
            value = state.routeTuning.stepDistanceVariance,
            onValueChange = onStepDistanceVarianceChange
        )

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_corridor_width_label),
            value = state.routeTuning.corridorWidth,
            onValueChange = onCorridorWidthChange
        )

        Text(
            text = when (val drawTargetStatus = uiModel.drawTargetStatus) {
                DrawTargetStatus.Selecting -> stringResource(R.string.draw_target_status_selecting)
                DrawTargetStatus.All -> stringResource(R.string.draw_target_status_all)
                is DrawTargetStatus.Count -> stringResource(
                    R.string.draw_target_status_count,
                    drawTargetStatus.count
                )
            },
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppOutlinedButton(
                onClick = onStartDrawTargetSelection,
                enabled = !state.isDrawTargetSelectionMode,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(uiModel.drawTargetButtonTextResId),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AppButton(
                onClick = onStartGoalSelection,
                enabled = uiModel.canStartGoalSelection,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(uiModel.startGoalButtonTextResId),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AppOutlinedButton(
                onClick = onClearChallenge,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.clear_challenge))
            }
        }

        AppOutlinedButton(
            onClick = onBackToList,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.back_to_list))
        }
    }
}

@Composable
private fun HoldDifficultyRangeSlider(
    label: String,
    startValue: Float,
    endValue: Float,
    onValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.challenge_difficulty_range_value,
                    startValue.roundToInt(),
                    endValue.roundToInt()
                ),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        RangeSlider(
            value = startValue..endValue,
            onValueChange = { range ->
                onValueChange(range.start, range.endInclusive)
            },
            valueRange = 1f..5f,
            steps = 3,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun formatChallengeDebugNumber(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}

@Composable
private fun ChallengeTuningSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.challenge_randomness_value,
                    (value * 100f).roundToInt()
                ),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
