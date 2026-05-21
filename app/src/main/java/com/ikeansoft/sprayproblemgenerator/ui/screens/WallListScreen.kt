package com.ikeansoft.sprayproblemgenerator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.model.SavedWallSummary
import com.ikeansoft.sprayproblemgenerator.ui.AppSecondaryTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSubtleSurfaceColor
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppIconButton
import com.ikeansoft.sprayproblemgenerator.ui.components.AppConfirmDialog
import com.ikeansoft.sprayproblemgenerator.ui.components.AppContentDialog
import com.ikeansoft.sprayproblemgenerator.ui.components.AppOutlinedButton
import com.ikeansoft.sprayproblemgenerator.ui.components.BottomActionBar
import com.ikeansoft.sprayproblemgenerator.ui.components.ScreenHeader
import com.ikeansoft.sprayproblemgenerator.ui.components.WallThumbnail
import com.ikeansoft.sprayproblemgenerator.ui.selectors.formatWallTimestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WallListScreen(
    savedWalls: List<SavedWallSummary>,
    onOpenMenu: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenSavedWallForReachCalibration: (String) -> Unit,
    onOpenSavedWallForHoldEditor: (String) -> Unit,
    onOpenSavedWallForHoldAttributeEditor: (String) -> Unit,
    onOpenSavedWallForHoldScoring: (String) -> Unit,
    onOpenSavedWallForChallenge: (String) -> Unit,
    onOpenSavedWallChallenges: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deletingWallId by remember { mutableStateOf<String?>(null) }
    var optionWallId by remember { mutableStateOf<String?>(null) }
    var isImageSourceDialogOpen by remember { mutableStateOf(false) }
    val footerOverlayPadding = 136.dp

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ScreenHeader(
                title = stringResource(R.string.wall_list_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                leadingContent = {
                    AppIconButton(onClick = onOpenMenu) {
                        Text(
                            text = "\u2630",
                            color = AppTextColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            if (savedWalls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = footerOverlayPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_saved_walls),
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = footerOverlayPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedWalls, key = { it.id }) { wall ->
                        SavedWallCard(
                            wall = wall,
                            onCreateChallenge = { onOpenSavedWallForChallenge(wall.id) },
                            onViewSavedChallenges = { onOpenSavedWallChallenges(wall.id) },
                            onOpenOptions = { optionWallId = wall.id }
                        )
                    }
                }
            }
        }

        BottomActionBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            AppButton(
                onClick = { isImageSourceDialogOpen = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.new_wall_button))
            }
        }
    }

    optionWallId?.let { wallId ->
        AppContentDialog(
            title = null,
            onDismissRequest = { optionWallId = null },
            dismissText = stringResource(R.string.cancel)
        ) {
            AppButton(
                onClick = {
                    optionWallId = null
                    onOpenSavedWallForReachCalibration(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_reach_calibration))
            }
            AppButton(
                onClick = {
                    optionWallId = null
                    onOpenSavedWallForHoldEditor(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_hold_editor))
            }
            AppButton(
                onClick = {
                    optionWallId = null
                    onOpenSavedWallForHoldAttributeEditor(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_hold_attribute_editor))
            }
            AppButton(
                onClick = {
                    optionWallId = null
                    onOpenSavedWallForHoldScoring(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_hold_scoring))
            }
            AppOutlinedButton(
                onClick = {
                    optionWallId = null
                    deletingWallId = wallId
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }

    if (isImageSourceDialogOpen) {
        AppContentDialog(
            title = stringResource(R.string.camera_title),
            onDismissRequest = { isImageSourceDialogOpen = false },
            dismissText = stringResource(R.string.cancel)
        ) {
            AppButton(
                onClick = {
                    isImageSourceDialogOpen = false
                    onTakePhoto()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.camera_take_photo))
            }
            AppButton(
                onClick = {
                    isImageSourceDialogOpen = false
                    onPickPhoto()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.camera_pick_photo))
            }
        }
    }

    deletingWallId?.let { wallId ->
        AppConfirmDialog(
            title = stringResource(R.string.delete_wall_title),
            message = stringResource(R.string.delete_wall_message),
            confirmText = stringResource(R.string.delete),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                deletingWallId = null
                onDeleteSavedWall(wallId)
            },
            onDismissRequest = { deletingWallId = null }
        )
    }
}

@Composable
private fun SavedWallCard(
    wall: SavedWallSummary,
    onCreateChallenge: () -> Unit,
    onViewSavedChallenges: () -> Unit,
    onOpenOptions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.updated_at_label,
                        formatWallTimestamp(
                            wall.updatedAt,
                            stringResource(R.string.wall_timestamp_format),
                            stringResource(R.string.wall_timestamp_unknown)
                        )
                    ),
                    color = AppSecondaryTextColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                WallListOptionCell(onClick = onOpenOptions)
            }

            WallThumbnail(
                imageFilePath = wall.imageFilePath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppButton(
                    onClick = onViewSavedChallenges,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_saved_challenges))
                }
                AppButton(
                    onClick = onCreateChallenge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.create_challenge))
                }
            }
        }
    }
}

@Composable
private fun WallListOptionCell(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val shape = RoundedCornerShape(12.dp)
    var showTapFlash by remember { mutableStateOf(false) }
    var isHandlingTap by remember { mutableStateOf(false) }

    val wrappedOnClick: () -> Unit = click@{
        if (isHandlingTap) return@click
        isHandlingTap = true
        showTapFlash = true
        scope.launch {
            try {
                delay(80)
                currentOnClick()
            } finally {
                showTapFlash = false
                isHandlingTap = false
            }
        }
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                color = if (isPressed || showTapFlash) AppSubtleSurfaceColor else Color.Transparent,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = wrappedOnClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u22EE",
            modifier = Modifier.fillMaxWidth(),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
