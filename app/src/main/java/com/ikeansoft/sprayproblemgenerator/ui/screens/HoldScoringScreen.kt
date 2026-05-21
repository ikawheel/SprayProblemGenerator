package com.ikeansoft.sprayproblemgenerator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.ui.AppSecondaryTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSubtleSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState
import com.ikeansoft.sprayproblemgenerator.ui.stringResourceByName
import com.ikeansoft.sprayproblemgenerator.ui.canvas.HoldScoringCanvasScreen
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppOutlinedButton
import com.ikeansoft.sprayproblemgenerator.ui.components.WallRegistrationStepScaffold
import com.ikeansoft.sprayproblemgenerator.ui.selectors.deriveHoldScoringUiModel

@Composable
fun HoldScoringScreen(
    state: MainUiState,
    onBackToHoldEditor: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDifficultyScoreSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveHoldScoringUiModel(state)
    val isEditingExistingWall = state.currentWallId != null
    val imageAspectRatio = bitmap?.takeIf { it.height > 0 }?.let {
        wallImageDisplayAspectRatio(
            imageWidth = it.width,
            imageHeight = it.height
        )
    }

    WallRegistrationStepScaffold(
        modifier = modifier,
        headerText = stringResource(R.string.registration_step_hold_scoring_title),
        imageAspectRatio = imageAspectRatio,
        useFullImageViewport = true,
        imageContent = {
            if (bitmap != null && uiModel.currentHoldIndex != null) {
                HoldScoringCanvasScreen(
                    bitmap = bitmap,
                    holds = state.holds,
                    currentHoldIndex = uiModel.currentHoldIndex,
                    displayColorSettings = state.displayColorSettings,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        bodyContent = {
            Text(
                text = stringResource(R.string.hold_scoring_description),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall
            )

            if (uiModel.totalCount == 0) {
                Text(
                    text = stringResourceByName("hold_scoring_empty"),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else if (uiModel.isCompleted) {
                Text(
                    text = stringResourceByName("hold_scoring_completed"),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                Text(
                    text = stringResourceByName(
                        "hold_scoring_progress",
                        uiModel.currentPosition,
                        uiModel.totalCount
                    ),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Text(
                    text = stringResourceByName(
                        "hold_scoring_current_score",
                        uiModel.currentDifficultyScore ?: 3
                    ),
                    color = AppSecondaryTextColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { score ->
                        val isCurrentScore = uiModel.currentDifficultyScore == score
                        val buttonModifier = Modifier.weight(1f)
                        if (isCurrentScore) {
                            AppButton(
                                onClick = { onDifficultyScoreSelected(score) },
                                modifier = buttonModifier
                            ) {
                                Text(
                                    text = "$score",
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            AppOutlinedButton(
                                onClick = { onDifficultyScoreSelected(score) },
                                modifier = buttonModifier
                            ) {
                                Text(
                                    text = "$score",
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { score ->
                        Text(
                            text = when (score) {
                                1 -> stringResource(R.string.hold_scoring_easy)
                                5 -> stringResource(R.string.hold_scoring_hard)
                                else -> ""
                            },
                            color = AppSecondaryTextColor,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        footerContent = {
            AppButton(
                onClick = onSaveAndExit,
                enabled = uiModel.totalCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isEditingExistingWall) {
                        stringResource(R.string.overwrite_save)
                    } else {
                        stringResource(R.string.save_and_exit)
                    }
                )
            }
        }
    )
}
