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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.canvas.HoldScoringCanvasScreen
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.selectors.deriveHoldScoringUiModel

private const val HoldScoringTitle = "\u30db\u30fc\u30eb\u30c9\u70b9\u6570"
private const val HoldScoringDescription =
    "\u5de6\u4e0a\u304b\u3089\u9806\u306b\u30011\u301c5\u70b9\u3067\u63a1\u70b9\u3057\u307e\u3059"
private const val HoldScoringEmpty =
    "\u63a1\u70b9\u3067\u304d\u308b\u30db\u30fc\u30eb\u30c9\u304c\u3042\u308a\u307e\u305b\u3093"
private const val HoldScoringCompleted =
    "\u63a1\u70b9\u304c\u5b8c\u4e86\u3057\u307e\u3057\u305f"
private const val HoldScoringBack = "\u30db\u30fc\u30eb\u30c9\u767b\u9332\u3078"
private const val HoldScoringOpenChallenge = "\u8ab2\u984c\u4f5c\u6210\u3078"

@Composable
fun HoldScoringScreen(
    state: MainUiState,
    onBackToHoldEditor: () -> Unit,
    onDifficultyScoreSelected: (Int) -> Unit,
    onOpenChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val uiModel = deriveHoldScoringUiModel(state)

    Column(modifier = modifier) {
        Text(
            text = HoldScoringTitle,
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = HoldScoringDescription,
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )

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
                text = HoldScoringEmpty,
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else if (uiModel.isCompleted) {
            Text(
                text = HoldScoringCompleted,
                color = AppTextColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            Text(
                text = "\u5bfe\u8c61 ${uiModel.currentPosition} / ${uiModel.totalCount}",
                color = AppTextColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = "\u73fe\u5728\u306e\u70b9\u6570: ${uiModel.currentDifficultyScore ?: 3}\u70b9",
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppOutlinedButton(
                onClick = onBackToHoldEditor,
                modifier = Modifier.weight(1f)
            ) {
                Text(HoldScoringBack)
            }

            AppButton(
                onClick = onOpenChallenge,
                enabled = uiModel.totalCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text(HoldScoringOpenChallenge)
            }
        }
    }
}
