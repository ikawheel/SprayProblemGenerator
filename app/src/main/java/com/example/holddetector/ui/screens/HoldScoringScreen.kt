package com.example.holddetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.stringResourceByName
import com.example.holddetector.ui.canvas.HoldScoringCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.BottomActionBar
import com.example.holddetector.ui.selectors.deriveHoldScoringUiModel

@Composable
fun HoldScoringScreen(
    state: MainUiState,
    onBackToHoldEditor: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onSaveAndExit: () -> Unit,
    onDifficultyScoreSelected: (Int) -> Unit,
    onOpenChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveHoldScoringUiModel(state)
    val isEditingExistingWall = state.currentWallId != null

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            BottomActionBar {
                if (isEditingExistingWall) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppOutlinedButton(
                            onClick = onExitWithoutSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.exit_without_saving))
                        }

                        AppButton(
                            onClick = onSaveAndExit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.save_and_exit))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppOutlinedButton(
                            onClick = onBackToHoldEditor,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResourceByName("hold_scoring_back"))
                        }

                        AppButton(
                            onClick = onOpenChallenge,
                            enabled = uiModel.totalCount > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResourceByName("hold_scoring_open_challenge"))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = AppSurfaceColor,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = stringResourceByName("hold_scoring_title"),
                        color = AppTextColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResourceByName("hold_scoring_description"),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                        .clipToBounds()
                ) {
                    if (bitmap != null && uiModel.currentHoldIndex != null) {
                        HoldScoringCanvasScreen(
                            bitmap = bitmap,
                            holds = state.holds,
                            currentHoldIndex = uiModel.currentHoldIndex,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

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
                }
            }
        }
    }
}
