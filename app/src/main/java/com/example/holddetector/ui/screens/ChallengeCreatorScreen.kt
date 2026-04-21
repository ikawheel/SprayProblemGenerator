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
import com.example.holddetector.ui.ChallengeFlowStep
import com.example.holddetector.ui.ChallengeGenerationMethod
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
    onSelectManualStartGoalChallengeMethod: () -> Unit,
    onSelectRandomStartGoalChallengeMethod: () -> Unit,
    onOpenChallengeMethodSelection: () -> Unit,
    onOpenChallengeCommonSettings: () -> Unit,
    onOpenChallengeGeneration: () -> Unit,
    onOpenChallengeTuning: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onStartGoalSelection: () -> Unit,
    onDrawWithRandomStartGoal: () -> Unit,
    onStartDrawTargetSelection: () -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawClick: () -> Unit,
    onRerunCurrentChallengeGeneration: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
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

        when (state.challengeFlowStep) {
            ChallengeFlowStep.METHOD_SELECT -> {
                ChallengeMethodSelectionContent(
                    onSelectManualStartGoalChallengeMethod = onSelectManualStartGoalChallengeMethod,
                    onSelectRandomStartGoalChallengeMethod = onSelectRandomStartGoalChallengeMethod
                )
            }

            ChallengeFlowStep.COMMON_SETTINGS -> {
                ChallengeCommonSettingsContent(
                    state = state,
                    uiModel = uiModel,
                    onOpenChallengeMethodSelection = onOpenChallengeMethodSelection,
                    onOpenChallengeGeneration = onOpenChallengeGeneration,
                    onOpenChallengeTuning = onOpenChallengeTuning,
                    onChallengeHoldTapped = onChallengeHoldTapped,
                    onStartDrawTargetSelection = onStartDrawTargetSelection,
                    onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                    onDrawCountChange = onDrawCountChange,
                    onChallengeDifficultyRangeChange = onChallengeDifficultyRangeChange
                )
            }

            ChallengeFlowStep.GENERATION -> {
                when (state.challengeGenerationMethod) {
                    ChallengeGenerationMethod.MANUAL_START_GOAL -> {
                        ChallengeManualGenerationContent(
                            state = state,
                            uiModel = uiModel,
                            onOpenChallengeCommonSettings = onOpenChallengeCommonSettings,
                            onChallengeHoldTapped = onChallengeHoldTapped,
                            onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                            onStartGoalSelection = onStartGoalSelection,
                            onDrawClick = onDrawClick
                        )
                    }

                    ChallengeGenerationMethod.RANDOM_START_GOAL -> {
                        ChallengeRandomGenerationContent(
                            state = state,
                            uiModel = uiModel,
                            onOpenChallengeCommonSettings = onOpenChallengeCommonSettings,
                            onChallengeHoldTapped = onChallengeHoldTapped,
                            onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                            onDrawWithRandomStartGoal = onDrawWithRandomStartGoal
                        )
                    }

                    null -> {
                        ChallengeMethodSelectionContent(
                            onSelectManualStartGoalChallengeMethod = onSelectManualStartGoalChallengeMethod,
                            onSelectRandomStartGoalChallengeMethod = onSelectRandomStartGoalChallengeMethod
                        )
                    }
                }
            }

            ChallengeFlowStep.RESULT -> {
                ChallengeResultContent(
                    state = state,
                    uiModel = uiModel,
                    isDebugSummaryExpanded = isDebugSummaryExpanded,
                    onDebugSummaryExpandedChange = { isDebugSummaryExpanded = it },
                    onOpenChallengeCommonSettings = onOpenChallengeCommonSettings,
                    onChallengeHoldTapped = onChallengeHoldTapped,
                    onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                    onRerunCurrentChallengeGeneration = onRerunCurrentChallengeGeneration
                )
            }

            ChallengeFlowStep.TUNING -> {
                ChallengeTuningContent(
                    state = state,
                    onOpenChallengeCommonSettings = onOpenChallengeCommonSettings,
                    onDetourStrengthChange = onDetourStrengthChange,
                    onRouteWavinessChange = onRouteWavinessChange,
                    onStepDistanceVarianceChange = onStepDistanceVarianceChange,
                    onCorridorWidthChange = onCorridorWidthChange
                )
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
private fun ChallengeMethodSelectionContent(
    onSelectManualStartGoalChallengeMethod: () -> Unit,
    onSelectRandomStartGoalChallengeMethod: () -> Unit
) {
    Text(
        text = stringResource(R.string.challenge_method_select_heading),
        color = AppTextColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Text(
        text = stringResource(R.string.challenge_method_select_description),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    AppButton(
        onClick = onSelectManualStartGoalChallengeMethod,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.challenge_method_manual_start_goal))
    }

    AppOutlinedButton(
        onClick = onSelectRandomStartGoalChallengeMethod,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(stringResource(R.string.challenge_method_random_start_goal))
    }
}

@Composable
private fun ChallengeCommonSettingsContent(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    onOpenChallengeMethodSelection: () -> Unit,
    onOpenChallengeGeneration: () -> Unit,
    onOpenChallengeTuning: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onStartDrawTargetSelection: () -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit
) {
    Text(
        text = stringResource(R.string.challenge_settings_heading),
        color = AppTextColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Text(
        text = stringResource(R.string.challenge_settings_description),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    ChallengeCanvasSection(
        state = state,
        uiModel = uiModel,
        onChallengeHoldTapped = onChallengeHoldTapped,
        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted
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

    AppOutlinedButton(
        onClick = onStartDrawTargetSelection,
        enabled = !state.isDrawTargetSelectionMode,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(stringResource(uiModel.drawTargetButtonTextResId))
    }

    HoldDifficultyRangeSlider(
        label = stringResource(R.string.challenge_difficulty_range_label),
        startValue = state.challengeDifficultyScoreMin.toFloat(),
        endValue = state.challengeDifficultyScoreMax.toFloat(),
        onValueChange = onChallengeDifficultyRangeChange,
        modifier = Modifier.padding(top = 12.dp)
    )

    OutlinedTextField(
        value = state.drawCountInput,
        onValueChange = onDrawCountChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        singleLine = true,
        label = { Text(stringResource(R.string.draw_count_label)) },
        placeholder = { Text(stringResource(R.string.draw_count_placeholder_auto)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppOutlinedButton(
            onClick = onOpenChallengeMethodSelection,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.challenge_change_method))
        }

        AppOutlinedButton(
            onClick = onOpenChallengeTuning,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.challenge_open_tuning))
        }
    }

    AppButton(
        onClick = onOpenChallengeGeneration,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = stringResource(
                when (state.challengeGenerationMethod) {
                    ChallengeGenerationMethod.MANUAL_START_GOAL -> {
                        R.string.challenge_open_generation_manual
                    }

                    ChallengeGenerationMethod.RANDOM_START_GOAL -> {
                        R.string.challenge_open_generation_random
                    }

                    null -> R.string.challenge_open_generation_manual
                }
            )
        )
    }
}

@Composable
private fun ChallengeManualGenerationContent(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    onOpenChallengeCommonSettings: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onStartGoalSelection: () -> Unit,
    onDrawClick: () -> Unit
) {
    Text(
        text = stringResource(R.string.challenge_manual_generation_heading),
        color = AppTextColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Text(
        text = stringResource(uiModel.helpTextResId),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    ChallengeCanvasSection(
        state = state,
        uiModel = uiModel,
        onChallengeHoldTapped = onChallengeHoldTapped,
        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted
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
        modifier = Modifier.padding(top = 12.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppOutlinedButton(
            onClick = onOpenChallengeCommonSettings,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.challenge_back_to_settings))
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
    }

    AppButton(
        onClick = onDrawClick,
        enabled = uiModel.isReadyToGenerate,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(stringResource(R.string.draw))
    }
}

@Composable
private fun ChallengeRandomGenerationContent(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    onOpenChallengeCommonSettings: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawWithRandomStartGoal: () -> Unit
) {
    Text(
        text = stringResource(R.string.challenge_random_generation_heading),
        color = AppTextColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Text(
        text = stringResource(R.string.challenge_random_generation_description),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    ChallengeCanvasSection(
        state = state,
        uiModel = uiModel,
        onChallengeHoldTapped = onChallengeHoldTapped,
        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted
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
        modifier = Modifier.padding(top = 12.dp)
    )

    AppOutlinedButton(
        onClick = onOpenChallengeCommonSettings,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(stringResource(R.string.challenge_back_to_settings))
    }

    AppButton(
        onClick = onDrawWithRandomStartGoal,
        enabled = uiModel.canAutoGenerateWithRandomStartGoal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(stringResource(R.string.draw_random_start_goal))
    }
}

@Composable
private fun ChallengeResultContent(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    isDebugSummaryExpanded: Boolean,
    onDebugSummaryExpandedChange: (Boolean) -> Unit,
    onOpenChallengeCommonSettings: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onRerunCurrentChallengeGeneration: () -> Unit
) {
    Text(
        text = stringResource(R.string.challenge_result_heading),
        color = AppTextColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Text(
        text = stringResource(R.string.challenge_result_description),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    ChallengeCanvasSection(
        state = state,
        uiModel = uiModel,
        onChallengeHoldTapped = onChallengeHoldTapped,
        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted
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
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
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

    ChallengeDebugSummarySection(
        uiModel = uiModel,
        isExpanded = isDebugSummaryExpanded,
        onExpandedChange = onDebugSummaryExpandedChange
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppOutlinedButton(
            onClick = onOpenChallengeCommonSettings,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.challenge_back_to_settings))
        }

        AppButton(
            onClick = onRerunCurrentChallengeGeneration,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.challenge_regenerate))
        }
    }
}

@Composable
private fun ChallengeTuningContent(
    state: MainUiState,
    onOpenChallengeCommonSettings: () -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit
) {
    Text(
        text = stringResource(R.string.challenge_tuning_title),
        color = AppTextColor,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Text(
        text = stringResource(R.string.challenge_tuning_description),
        color = AppSecondaryTextColor,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
    )

    ChallengeTuningSlider(
        label = stringResource(R.string.challenge_detour_strength_label),
        value = state.routeTuning.detourStrength,
        onValueChange = onDetourStrengthChange
    )

    ChallengeTuningSlider(
        label = stringResource(R.string.challenge_route_waviness_label),
        value = state.routeTuning.routeWaviness,
        onValueChange = onRouteWavinessChange,
        modifier = Modifier.padding(top = 8.dp)
    )

    ChallengeTuningSlider(
        label = stringResource(R.string.challenge_step_distance_variance_label),
        value = state.routeTuning.stepDistanceVariance,
        onValueChange = onStepDistanceVarianceChange,
        modifier = Modifier.padding(top = 8.dp)
    )

    ChallengeTuningSlider(
        label = stringResource(R.string.challenge_corridor_width_label),
        value = state.routeTuning.corridorWidth,
        onValueChange = onCorridorWidthChange,
        modifier = Modifier.padding(top = 8.dp)
    )

    AppOutlinedButton(
        onClick = onOpenChallengeCommonSettings,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(stringResource(R.string.challenge_back_to_settings))
    }
}

@Composable
private fun ChallengeCanvasSection(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit
) {
    val bitmap = state.capturedBitmap

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
}

@Composable
private fun ChallengeDebugSummarySection(
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    if (uiModel.challengeDebugSummaryRows.isEmpty()) return

    AppOutlinedButton(
        onClick = { onExpandedChange(!isExpanded) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(
            text = stringResource(
                if (isExpanded) {
                    R.string.challenge_debug_hide
                } else {
                    R.string.challenge_debug_show
                }
            )
        )
    }

    if (!isExpanded) return

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
