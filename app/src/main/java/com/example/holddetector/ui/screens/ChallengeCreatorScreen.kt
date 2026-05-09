package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.example.holddetector.ui.components.AppContentDialog
import com.example.holddetector.ui.components.AppMessageDialog
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.CompactRangeSlider
import com.example.holddetector.ui.components.CompactSlider
import com.example.holddetector.ui.selectors.DrawTargetStatus
import com.example.holddetector.ui.selectors.deriveChallengeCreatorUiModel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ChallengeCreatorScreen(
    state: MainUiState,
    onSelectManualStartGoalChallengeMethod: () -> Unit,
    onSelectRandomStartGoalChallengeMethod: () -> Unit,
    onOpenChallengeCommonSettings: () -> Unit,
    onOpenChallengeGeneration: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onStartGoalSelection: () -> Unit,
    onDrawWithRandomStartGoal: () -> Unit,
    onStartDrawTargetSelection: () -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawClick: () -> Unit,
    onRerunCurrentChallengeGeneration: () -> Unit,
    onSaveChallenge: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onExcludePreviouslyGeneratedHoldsChange: (Boolean) -> Unit,
    onRandomStartGoalPairLimitChange: (Int) -> Unit,
    onRouteGenerationAttemptLimitChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val uiModel = deriveChallengeCreatorUiModel(state)
    var isDebugSummaryExpanded by rememberSaveable { mutableStateOf(false) }
    var isTuningDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isGenerationMethodDialogOpen by rememberSaveable { mutableStateOf(false) }
    val navigationBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val isFullHeightChallengeStep = state.challengeFlowStep == ChallengeFlowStep.GENERATION ||
        state.challengeFlowStep == ChallengeFlowStep.RESULT

    if (isFullHeightChallengeStep) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 20.dp
                )
        ) {
            when {
                state.challengeFlowStep == ChallengeFlowStep.RESULT ||
                    uiModel.orderedChallengeIndices.isNotEmpty() -> {
                    ChallengeResultContent(
                        state = state,
                        uiModel = uiModel,
                        isDebugSummaryExpanded = isDebugSummaryExpanded,
                        onDebugSummaryExpandedChange = { isDebugSummaryExpanded = it },
                        onChallengeHoldTapped = onChallengeHoldTapped,
                        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                        onRerunCurrentChallengeGeneration = onRerunCurrentChallengeGeneration,
                        onSaveChallenge = onSaveChallenge,
                        navigationBarBottomPadding = navigationBarBottomPadding
                    )
                }

                else -> when (state.challengeGenerationMethod) {
                    ChallengeGenerationMethod.MANUAL_START_GOAL -> {
                        ChallengeManualGenerationContent(
                            state = state,
                            uiModel = uiModel,
                            onChallengeHoldTapped = onChallengeHoldTapped,
                            onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                            onStartGoalSelection = onStartGoalSelection,
                            onDrawClick = onDrawClick,
                            navigationBarBottomPadding = navigationBarBottomPadding
                        )
                    }

                    ChallengeGenerationMethod.RANDOM_START_GOAL -> {
                        ChallengeRandomGenerationContent(
                            state = state,
                            uiModel = uiModel,
                            onChallengeHoldTapped = onChallengeHoldTapped,
                            onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                            onDrawWithRandomStartGoal = onDrawWithRandomStartGoal,
                            navigationBarBottomPadding = navigationBarBottomPadding
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
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 20.dp + navigationBarBottomPadding
                )
        ) {
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
                        onOpenChallengeGeneration = { isGenerationMethodDialogOpen = true },
                        onShowTuningDialog = { isTuningDialogOpen = true },
                        onChallengeHoldTapped = onChallengeHoldTapped,
                        onStartDrawTargetSelection = onStartDrawTargetSelection,
                        onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                        onDrawCountChange = onDrawCountChange,
                        onChallengeDifficultyRangeChange = onChallengeDifficultyRangeChange
                    )
                }

                ChallengeFlowStep.GENERATION -> {
                    Unit
                }

                ChallengeFlowStep.RESULT -> {
                    Unit
                }

                ChallengeFlowStep.TUNING -> {
                    ChallengeTuningContent(
                        state = state,
                        onDetourStrengthChange = onDetourStrengthChange,
                        onRouteWavinessChange = onRouteWavinessChange,
                        onStepDistanceVarianceChange = onStepDistanceVarianceChange,
                        onCorridorWidthChange = onCorridorWidthChange,
                        onExcludePreviouslyGeneratedHoldsChange = onExcludePreviouslyGeneratedHoldsChange,
                        onRandomStartGoalPairLimitChange = onRandomStartGoalPairLimitChange,
                        onRouteGenerationAttemptLimitChange = onRouteGenerationAttemptLimitChange
                    )
                }
            }
        }
    }

    if (isTuningDialogOpen) {
        AppContentDialog(
            title = null,
            onDismissRequest = { isTuningDialogOpen = false },
            dismissText = stringResource(R.string.close)
        ) {
            ChallengeTuningControls(
                state = state,
                onDetourStrengthChange = onDetourStrengthChange,
                onRouteWavinessChange = onRouteWavinessChange,
                onStepDistanceVarianceChange = onStepDistanceVarianceChange,
                onCorridorWidthChange = onCorridorWidthChange,
                onExcludePreviouslyGeneratedHoldsChange = onExcludePreviouslyGeneratedHoldsChange,
                onRandomStartGoalPairLimitChange = onRandomStartGoalPairLimitChange,
                onRouteGenerationAttemptLimitChange = onRouteGenerationAttemptLimitChange
            )
        }
    }

    if (isGenerationMethodDialogOpen) {
        AppContentDialog(
            title = stringResource(R.string.challenge_menu_title),
            onDismissRequest = { isGenerationMethodDialogOpen = false },
            dismissText = stringResource(R.string.cancel)
        ) {
            AppButton(
                onClick = {
                    isGenerationMethodDialogOpen = false
                    onSelectManualStartGoalChallengeMethod()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.challenge_method_manual_start_goal))
            }
            AppButton(
                onClick = {
                    isGenerationMethodDialogOpen = false
                    onSelectRandomStartGoalChallengeMethod()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.challenge_method_random_start_goal))
            }
        }
    }
}

@Composable
private fun ChallengeMethodSelectionContent(
    onSelectManualStartGoalChallengeMethod: () -> Unit,
    onSelectRandomStartGoalChallengeMethod: () -> Unit
) {
    AppButton(
        onClick = onSelectManualStartGoalChallengeMethod,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.challenge_method_manual_start_goal))
    }

    AppButton(
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
    onOpenChallengeGeneration: () -> Unit,
    onShowTuningDialog: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onStartDrawTargetSelection: () -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawCountChange: (String) -> Unit,
    onChallengeDifficultyRangeChange: (Float, Float) -> Unit
) {
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

    AppButton(
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

    Text(
        text = stringResource(R.string.draw_count_label),
        color = AppTextColor,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )

    OutlinedTextField(
        value = state.drawCountInput,
        onValueChange = onDrawCountChange,
        modifier = Modifier
            .fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.draw_count_placeholder_auto)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    AppButton(
        onClick = onShowTuningDialog,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(stringResource(R.string.challenge_open_tuning))
    }

    AppButton(
        onClick = onOpenChallengeGeneration,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Text(text = stringResource(R.string.challenge_method_select_heading))
    }
}

@Composable
private fun ChallengeManualGenerationContent(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onStartGoalSelection: () -> Unit,
    onDrawClick: () -> Unit,
    navigationBarBottomPadding: androidx.compose.ui.unit.Dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ChallengeCanvasSection(
            state = state,
            uiModel = uiModel,
            onChallengeHoldTapped = onChallengeHoldTapped,
            onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            useAspectRatio = false
        )

        AppButton(
            onClick = onDrawClick,
            enabled = uiModel.isReadyToGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.draw))
        }

        AppButton(
            onClick = onStartGoalSelection,
            enabled = uiModel.canStartGoalSelection,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = navigationBarBottomPadding)
        ) {
            Text(
                text = stringResource(uiModel.startGoalButtonTextResId),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChallengeRandomGenerationContent(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onDrawWithRandomStartGoal: () -> Unit,
    navigationBarBottomPadding: androidx.compose.ui.unit.Dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ChallengeCanvasSection(
            state = state,
            uiModel = uiModel,
            onChallengeHoldTapped = onChallengeHoldTapped,
            onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            useAspectRatio = false
        )

        AppButton(
            onClick = onDrawWithRandomStartGoal,
            enabled = uiModel.canAutoGenerateWithRandomStartGoal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = navigationBarBottomPadding)
        ) {
            Text(stringResource(R.string.draw_random_start_goal))
        }
    }
}

@Composable
private fun ChallengeResultContent(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    isDebugSummaryExpanded: Boolean,
    onDebugSummaryExpandedChange: (Boolean) -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    onRerunCurrentChallengeGeneration: () -> Unit,
    onSaveChallenge: () -> Unit,
    navigationBarBottomPadding: androidx.compose.ui.unit.Dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ChallengeCanvasSection(
            state = state,
            uiModel = uiModel,
            onChallengeHoldTapped = onChallengeHoldTapped,
            onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            useAspectRatio = false
        )

        AppButton(
            onClick = onSaveChallenge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.save_challenge))
        }

        AppButton(
            onClick = onRerunCurrentChallengeGeneration,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.challenge_regenerate))
        }

        ChallengeDebugSummarySection(
            state = state,
            uiModel = uiModel,
            isExpanded = isDebugSummaryExpanded,
            onExpandedChange = onDebugSummaryExpandedChange,
            modifier = Modifier.padding(bottom = navigationBarBottomPadding)
        )
    }
}

@Composable
private fun ChallengeTuningContent(
    state: MainUiState,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onExcludePreviouslyGeneratedHoldsChange: (Boolean) -> Unit,
    onRandomStartGoalPairLimitChange: (Int) -> Unit,
    onRouteGenerationAttemptLimitChange: (Int) -> Unit
) {
    ChallengeTuningControls(
        state = state,
        onDetourStrengthChange = onDetourStrengthChange,
        onRouteWavinessChange = onRouteWavinessChange,
        onStepDistanceVarianceChange = onStepDistanceVarianceChange,
        onCorridorWidthChange = onCorridorWidthChange,
        onExcludePreviouslyGeneratedHoldsChange = onExcludePreviouslyGeneratedHoldsChange,
        onRandomStartGoalPairLimitChange = onRandomStartGoalPairLimitChange,
        onRouteGenerationAttemptLimitChange = onRouteGenerationAttemptLimitChange
    )
}

@Composable
private fun ChallengeTuningControls(
    state: MainUiState,
    onDetourStrengthChange: (Float) -> Unit,
    onRouteWavinessChange: (Float) -> Unit,
    onStepDistanceVarianceChange: (Float) -> Unit,
    onCorridorWidthChange: (Float) -> Unit,
    onExcludePreviouslyGeneratedHoldsChange: (Boolean) -> Unit,
    onRandomStartGoalPairLimitChange: (Int) -> Unit,
    onRouteGenerationAttemptLimitChange: (Int) -> Unit
) {
    var helpDialogTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var helpDialogBody by rememberSaveable { mutableStateOf<String?>(null) }
    val detourLabel = stringResource(R.string.challenge_detour_strength_label)
    val detourHelp = stringResource(R.string.challenge_detour_strength_help)
    val wavinessLabel = stringResource(R.string.challenge_route_waviness_label)
    val wavinessHelp = stringResource(R.string.challenge_route_waviness_help)
    val varianceLabel = stringResource(R.string.challenge_step_distance_variance_label)
    val varianceHelp = stringResource(R.string.challenge_step_distance_variance_help)
    val corridorLabel = stringResource(R.string.challenge_corridor_width_label)
    val corridorHelp = stringResource(R.string.challenge_corridor_width_help)
    val excludeLabel = stringResource(R.string.challenge_exclude_previous_holds_label)
    val excludeHelp = stringResource(R.string.challenge_exclude_previous_holds_help)
    val attemptLimitsLabel = stringResource(R.string.challenge_attempt_limits_label)
    val attemptLimitsHelp = stringResource(R.string.challenge_attempt_limits_help)
    val initialPairLimit = state.routeTuning.randomStartGoalPairLimit.takeIf { it > 0 } ?: 10
    val initialAttemptLimit = state.routeTuning.routeGenerationAttemptLimit.takeIf { it > 0 } ?: 100
    var pairLimitInput by rememberSaveable(initialPairLimit) {
        mutableStateOf(initialPairLimit.toString())
    }
    var attemptLimitInput by rememberSaveable(initialAttemptLimit) {
        mutableStateOf(initialAttemptLimit.toString())
    }

    ChallengeTuningSlider(
        label = detourLabel,
        value = state.routeTuning.detourStrength,
        onValueChange = onDetourStrengthChange,
        onHelpClick = {
            helpDialogTitle = detourLabel
            helpDialogBody = detourHelp
        }
    )

    ChallengeTuningSlider(
        label = wavinessLabel,
        value = state.routeTuning.routeWaviness,
        onValueChange = onRouteWavinessChange,
        onHelpClick = {
            helpDialogTitle = wavinessLabel
            helpDialogBody = wavinessHelp
        },
        modifier = Modifier.padding(top = 8.dp)
    )

    ChallengeTuningSlider(
        label = varianceLabel,
        value = state.routeTuning.stepDistanceVariance,
        onValueChange = onStepDistanceVarianceChange,
        onHelpClick = {
            helpDialogTitle = varianceLabel
            helpDialogBody = varianceHelp
        },
        modifier = Modifier.padding(top = 8.dp)
    )

    ChallengeTuningSlider(
        label = corridorLabel,
        value = state.routeTuning.corridorWidth,
        onValueChange = onCorridorWidthChange,
        onHelpClick = {
            helpDialogTitle = corridorLabel
            helpDialogBody = corridorHelp
        },
        modifier = Modifier.padding(top = 8.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fun normalizeNumericInput(value: String, maxDigits: Int): String {
            val filteredValue = value.filter(Char::isDigit).take(maxDigits)
            val normalizedValue = filteredValue.trimStart('0')
            return normalizedValue.ifEmpty { "0" }
        }

        SettingLabelWithHelp(
            label = attemptLimitsLabel,
            onHelpClick = {
                helpDialogTitle = attemptLimitsLabel
                helpDialogBody = attemptLimitsHelp
            },
            modifier = Modifier.weight(1f)
        )

        CompactNumericField(
            value = pairLimitInput,
            onValueChange = { changedValue ->
                val normalizedValue = normalizeNumericInput(changedValue, maxDigits = 2)
                pairLimitInput = normalizedValue
                normalizedValue.toIntOrNull()?.let(onRandomStartGoalPairLimitChange)
            },
            width = 72.dp,
            modifier = Modifier.height(32.dp)
        )

        Text(
            text = "×",
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        CompactNumericField(
            value = attemptLimitInput,
            onValueChange = { changedValue ->
                val normalizedValue = normalizeNumericInput(changedValue, maxDigits = 3)
                attemptLimitInput = normalizedValue
                normalizedValue.toIntOrNull()?.let(onRouteGenerationAttemptLimitChange)
            },
            width = 86.dp,
            modifier = Modifier.height(32.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SettingLabelWithHelp(
                label = excludeLabel,
                onHelpClick = {
                    helpDialogTitle = excludeLabel
                    helpDialogBody = excludeHelp
                }
            )
        }

        Switch(
            checked = state.routeTuning.excludePreviouslyGeneratedHolds,
            onCheckedChange = onExcludePreviouslyGeneratedHoldsChange
        )
    }

    if (helpDialogTitle != null && helpDialogBody != null) {
        AppMessageDialog(
            title = helpDialogTitle!!,
            message = helpDialogBody!!,
            onDismissRequest = {
                helpDialogTitle = null
                helpDialogBody = null
            },
            dismissText = stringResource(R.string.close)
        )
    }
}

@Composable
private fun CompactNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.width(width),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = AppTextColor,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(AppTextColor),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}

@Composable
private fun ChallengeCanvasSection(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    onChallengeHoldTapped: (Int?) -> Unit,
    onDrawTargetSelectionCompleted: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    useAspectRatio: Boolean = true
) {
    val bitmap = state.capturedBitmap

    Box(
        modifier = modifier
            .then(
                if (!useAspectRatio) {
                    Modifier
                } else if (bitmap != null && bitmap.height > 0) {
                    Modifier.aspectRatio(
                        wallImageDisplayAspectRatio(
                            imageWidth = bitmap.width,
                            imageHeight = bitmap.height
                        )
                    )
                } else {
                    Modifier.height(560.dp)
                }
            )
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
                hasDrawTargetSelection = state.hasDrawTargetSelection,
                startHoldIndex = state.startHoldIndex,
                goalHoldIndex = state.goalHoldIndex,
                coreChallengeHoldIndex = uiModel.coreChallengeHoldIndex,
                routeSelectionMode = state.routeSelectionMode,
                isDrawTargetSelectionMode = state.isDrawTargetSelectionMode,
                displayColorSettings = state.displayColorSettings,
                onHoldTapped = onChallengeHoldTapped,
                onDrawTargetSelectionCompleted = onDrawTargetSelectionCompleted,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ChallengeDebugSummarySection(
    state: MainUiState,
    uiModel: com.example.holddetector.ui.selectors.ChallengeCreatorUiModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiModel.challengeDebugSummaryRows.isEmpty()) return

    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AppOutlinedButton(
            onClick = { onExpandedChange(!isExpanded) },
            modifier = Modifier.fillMaxWidth()
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

        if (!isExpanded) return@Column

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

        CompactRangeSlider(
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
    onHelpClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingLabelWithHelp(
                label = label,
                onHelpClick = onHelpClick
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

        CompactSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SettingLabelWithHelp(
    label: String,
    onHelpClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium
        )

        if (onHelpClick != null) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(22.dp)
                    .background(
                        color = AppSubtleSurfaceColor,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .clickable(onClick = onHelpClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = AppSecondaryTextColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
