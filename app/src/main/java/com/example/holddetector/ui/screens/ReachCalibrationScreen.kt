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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.canvas.ReachCalibrationCanvasScreen
import com.example.holddetector.model.HoldPoint

private const val ReachCalibrationTitle = "150cm\u8a2d\u5b9a"
private const val ReachCalibrationDescription =
    "\u58c1\u4e0a\u306e2\u70b9\u3092\u6307\u5b9a\u3057\u3066150cm\u306e\u57fa\u6e96\u3092\u8a2d\u5b9a\u3057\u3066\u304f\u3060\u3055\u3044"
private const val ReachCalibrationFirstPoint =
    "1\u70b9\u76ee\u3092\u30bf\u30c3\u30d7\u3057\u3066\u304f\u3060\u3055\u3044"
private const val ReachCalibrationSecondPoint =
    "2\u70b9\u76ee\u3092\u30bf\u30c3\u30d7\u3057\u3066\u304f\u3060\u3055\u3044"
private const val ReachCalibrationConfigured =
    "150cm\u57fa\u6e96\u3092\u8a2d\u5b9a\u6e08\u307f\u3067\u3059"
private const val ReachCalibrationUnset =
    "150cm\u57fa\u6e96\u304c\u672a\u8a2d\u5b9a\u3067\u3059"
private const val ReachCalibrationSetup = "150cm\u8a2d\u5b9a"
private const val ReachCalibrationReset = "150cm\u518d\u8a2d\u5b9a"
private const val ReachCalibrationBack = "\u623b\u308b"
private const val ReachCalibrationBackToList = "\u4e00\u89a7\u3078\u623b\u308b"
private const val ReachCalibrationClear = "\u30af\u30ea\u30a2"
private const val ReachCalibrationContinue = "\u30db\u30fc\u30eb\u30c9\u767b\u9332\u3078"

@Composable
fun ReachCalibrationScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onStartReachCalibrationSelection: () -> Unit,
    onClearReachCalibration: () -> Unit,
    onReachCalibrationPointSelected: (HoldPoint) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap
    val canContinue = state.reachCalibrationReference != null &&
        !state.isReachCalibrationSelectionMode &&
        state.pendingReachCalibrationPoint == null
    val isReturningToHoldEditor = state.reachCalibrationReturnToHoldEditor
    val statusText = when {
        state.isReachCalibrationSelectionMode && state.pendingReachCalibrationPoint == null ->
            ReachCalibrationFirstPoint
        state.isReachCalibrationSelectionMode && state.pendingReachCalibrationPoint != null ->
            ReachCalibrationSecondPoint
        state.reachCalibrationReference != null ->
            ReachCalibrationConfigured
        else ->
            ReachCalibrationUnset
    }
    val selectButtonText = if (state.reachCalibrationReference == null) {
        ReachCalibrationSetup
    } else {
        ReachCalibrationReset
    }
    val backButtonText = if (isReturningToHoldEditor) {
        ReachCalibrationBack
    } else {
        ReachCalibrationBackToList
    }

    Column(modifier = modifier) {
        Text(
            text = ReachCalibrationTitle,
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = ReachCalibrationDescription,
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
            if (bitmap != null) {
                ReachCalibrationCanvasScreen(
                    bitmap = bitmap,
                    reachCalibrationReference = state.reachCalibrationReference,
                    pendingReachCalibrationPoint = state.pendingReachCalibrationPoint,
                    isReachCalibrationSelectionMode = state.isReachCalibrationSelectionMode,
                    onReachCalibrationPointSelected = onReachCalibrationPointSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = statusText,
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text(backButtonText)
            }

            OutlinedButton(
                onClick = onStartReachCalibrationSelection,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = selectButtonText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onClearReachCalibration,
                enabled = state.reachCalibrationReference != null || state.pendingReachCalibrationPoint != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(ReachCalibrationClear)
            }

            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.weight(1f)
            ) {
                Text(ReachCalibrationContinue)
            }
        }
    }
}
