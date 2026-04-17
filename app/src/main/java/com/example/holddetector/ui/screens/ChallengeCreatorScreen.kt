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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.canvas.ChallengeCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.selectors.DrawTargetStatus
import com.example.holddetector.ui.selectors.deriveChallengeCreatorUiModel
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
    onHoldCountVarianceChange: (Float) -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onCandidateSelectionRandomnessChange: (Float) -> Unit,
    onFinalSelectionRandomnessChange: (Float) -> Unit,
    onClearChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val scrollState = rememberScrollState()
    val uiModel = deriveChallengeCreatorUiModel(state)

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
                    selectionCandidateIndices = uiModel.selectionCandidateIndices,
                    startHoldIndex = state.startHoldIndex,
                    goalHoldIndex = state.goalHoldIndex,
                    routeSelectionMode = state.routeSelectionMode,
                    isDrawTargetSelectionMode = state.isDrawTargetSelectionMode,
                    onHoldTapped = onChallengeHoldTapped,
                    onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = stringResource(uiModel.helpTextResId),
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

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

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_hold_count_variance_label),
            value = state.routeTuning.holdCountVariance,
            onValueChange = onHoldCountVarianceChange,
            modifier = Modifier.padding(top = 8.dp)
        )

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_detour_strength_label),
            value = state.routeTuning.detourStrength,
            onValueChange = onDetourStrengthChange
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

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_candidate_selection_randomness_label),
            value = state.routeTuning.candidateSelectionRandomness,
            onValueChange = onCandidateSelectionRandomnessChange
        )

        ChallengeTuningSlider(
            label = stringResource(R.string.challenge_final_selection_randomness_label),
            value = state.routeTuning.finalSelectionRandomness,
            onValueChange = onFinalSelectionRandomnessChange
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
