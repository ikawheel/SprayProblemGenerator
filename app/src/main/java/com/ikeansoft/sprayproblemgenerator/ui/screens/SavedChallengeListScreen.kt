package com.ikeansoft.sprayproblemgenerator.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.model.SavedChallengeSummary
import com.ikeansoft.sprayproblemgenerator.ui.AppSecondaryTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSubtleSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.DisplayColorSettings
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppConfirmDialog
import com.ikeansoft.sprayproblemgenerator.ui.components.AppOutlinedButton
import com.ikeansoft.sprayproblemgenerator.ui.selectors.formatWallTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SavedChallengeListScreen(
    savedChallenges: List<SavedChallengeSummary>,
    bitmap: Bitmap?,
    holds: List<Hold>,
    displayColorSettings: DisplayColorSettings,
    onOpenSavedChallenge: (String) -> Unit,
    onDeleteSavedChallenge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deletingChallengeId by remember { mutableStateOf<String?>(null) }

    if (savedChallenges.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.saved_challenges_empty),
                color = AppTextColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(savedChallenges, key = { it.id }) { challenge ->
            SavedChallengeCard(
                challenge = challenge,
                bitmap = bitmap,
                holds = holds,
                displayColorSettings = displayColorSettings,
                onOpenSavedChallenge = { onOpenSavedChallenge(challenge.id) },
                onDeleteSavedChallenge = { deletingChallengeId = challenge.id }
            )
        }
    }

    deletingChallengeId?.let { challengeId ->
        AppConfirmDialog(
            title = stringResource(R.string.delete_saved_challenge_title),
            message = stringResource(R.string.delete_saved_challenge_message),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                deletingChallengeId = null
                onDeleteSavedChallenge(challengeId)
            },
            onDismissRequest = { deletingChallengeId = null }
        )
    }
}

@Composable
private fun SavedChallengeCard(
    challenge: SavedChallengeSummary,
    bitmap: Bitmap?,
    holds: List<Hold>,
    displayColorSettings: DisplayColorSettings,
    onOpenSavedChallenge: () -> Unit,
    onDeleteSavedChallenge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatWallTimestamp(
                    challenge.createdAt,
                    stringResource(R.string.wall_timestamp_format),
                    stringResource(R.string.wall_timestamp_unknown)
                ),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall
            )

            SavedChallengeThumbnail(
                bitmap = bitmap,
                holds = holds,
                displayColorSettings = displayColorSettings,
                startHoldIndex = challenge.startHoldIndex,
                goalHoldIndex = challenge.goalHoldIndex,
                challengeHoldIndices = challenge.challengeHoldIndices,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            AppButton(
                onClick = onOpenSavedChallenge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.open_saved_challenge))
            }
            AppOutlinedButton(
                onClick = onDeleteSavedChallenge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun SavedChallengeThumbnail(
    bitmap: Bitmap?,
    holds: List<Hold>,
    displayColorSettings: DisplayColorSettings,
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    challengeHoldIndices: Set<Int>,
    modifier: Modifier = Modifier
) {
    val thumbnail by produceState<Bitmap?>(
        initialValue = null,
        bitmap,
        holds,
        startHoldIndex,
        goalHoldIndex,
        challengeHoldIndices
    ) {
        value = withContext(Dispatchers.Default) {
            bitmap?.let {
                cropChallengeThumbnail(
                    bitmap = it,
                    holds = holds,
                    displayColorSettings = displayColorSettings,
                    startHoldIndex = startHoldIndex,
                    goalHoldIndex = goalHoldIndex,
                    challengeHoldIndices = challengeHoldIndices
                )
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSubtleSurfaceColor, MaterialTheme.shapes.medium),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = stringResource(R.string.no_image),
                    color = AppSecondaryTextColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun cropChallengeThumbnail(
    bitmap: Bitmap,
    holds: List<Hold>,
    displayColorSettings: DisplayColorSettings,
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    challengeHoldIndices: Set<Int>
): Bitmap {
    val challengeHolds = challengeHoldIndices.mapNotNull(holds::getOrNull)
    if (challengeHolds.isEmpty()) {
        return bitmap
    }

    val cropHolds = challengeHolds

    val minX = cropHolds.minOf { it.minX }
    val maxX = cropHolds.maxOf { it.maxX }
    val minY = cropHolds.minOf { it.minY }
    val maxY = cropHolds.maxOf { it.maxY }

    val width = (maxX - minX).coerceAtLeast(1)
    val height = (maxY - minY).coerceAtLeast(1)
    val horizontalPadding = maxOf(24, (width * 0.2f).roundToInt())
    val verticalPadding = maxOf(24, (height * 0.2f).roundToInt())

    val paddedLeft = (minX - horizontalPadding).coerceAtLeast(0)
    val paddedTop = (minY - verticalPadding).coerceAtLeast(0)
    val paddedRight = (maxX + horizontalPadding).coerceAtMost(bitmap.width)
    val paddedBottom = (maxY + verticalPadding).coerceAtMost(bitmap.height)
    val cropLeft = paddedLeft
    val cropTop = paddedTop
    val cropWidth = (paddedRight - paddedLeft).coerceAtLeast(1)
    val cropHeight = (paddedBottom - paddedTop).coerceAtLeast(1)
    val croppedBitmap = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
    val resultBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)

    val grayscalePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    canvas.drawBitmap(croppedBitmap, 0f, 0f, grayscalePaint)

    val highlightedPath = Path().apply {
        challengeHolds.forEach { hold ->
            addPath(hold.toAndroidPath(offsetX = cropLeft, offsetY = cropTop))
        }
    }

    canvas.save()
    canvas.clipPath(highlightedPath)
    canvas.drawBitmap(croppedBitmap, 0f, 0f, bitmapPaint)
    canvas.restore()

    val holdOutlineColor = displayColorSettings.holdOutline
    val startGoalOutlineColor = displayColorSettings.startGoalHold
    val defaultOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(
            holdOutlineColor.normalizedRed,
            holdOutlineColor.normalizedGreen,
            holdOutlineColor.normalizedBlue
        )
        style = Paint.Style.STROKE
        strokeWidth = displayColorSettings.normalizedHoldOutlineStrokeWidth.toFloat() + 2f
    }
    val startGoalOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(
            startGoalOutlineColor.normalizedRed,
            startGoalOutlineColor.normalizedGreen,
            startGoalOutlineColor.normalizedBlue
        )
        style = Paint.Style.STROKE
        strokeWidth = displayColorSettings.normalizedStartGoalHoldStrokeWidth.toFloat() + 2f
    }
    challengeHoldIndices.forEach { holdIndex ->
        val hold = holds.getOrNull(holdIndex) ?: return@forEach
        val outlinePaint = if (holdIndex == startHoldIndex || holdIndex == goalHoldIndex) {
            startGoalOutlinePaint
        } else {
            defaultOutlinePaint
        }
        canvas.drawPath(
            hold.toAndroidPath(offsetX = cropLeft, offsetY = cropTop),
            outlinePaint
        )
    }

    return resultBitmap
}

private fun Hold.toAndroidPath(offsetX: Int, offsetY: Int): Path {
    return Path().apply {
        moveTo(
            (points.first().x - offsetX).toFloat(),
            (points.first().y - offsetY).toFloat()
        )
        for (index in 1 until points.size) {
            lineTo(
                (points[index].x - offsetX).toFloat(),
                (points[index].y - offsetY).toFloat()
            )
        }
        close()
    }
}
