package com.example.holddetector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.holddetector.R
import com.example.holddetector.model.SavedWallSummary
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppIconButton
import com.example.holddetector.ui.components.AppConfirmDialog
import com.example.holddetector.ui.components.AppContentDialog
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.BottomActionBar
import com.example.holddetector.ui.components.ScreenHeader
import com.example.holddetector.ui.components.WallThumbnail
import com.example.holddetector.ui.selectors.formatWallTimestamp

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
    var editingWallId by remember { mutableStateOf<String?>(null) }
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
                            onEdit = { editingWallId = wall.id },
                            onCreateChallenge = { onOpenSavedWallForChallenge(wall.id) },
                            onViewSavedChallenges = { onOpenSavedWallChallenges(wall.id) },
                            onDelete = { deletingWallId = wall.id }
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

    editingWallId?.let { wallId ->
        AppContentDialog(
            title = stringResource(R.string.edit_menu_title),
            onDismissRequest = { editingWallId = null },
            dismissText = stringResource(R.string.cancel)
        ) {
            AppButton(
                onClick = {
                    editingWallId = null
                    onOpenSavedWallForReachCalibration(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_reach_calibration))
            }
            AppButton(
                onClick = {
                    editingWallId = null
                    onOpenSavedWallForHoldEditor(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_hold_editor))
            }
            AppButton(
                onClick = {
                    editingWallId = null
                    onOpenSavedWallForHoldAttributeEditor(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_hold_attribute_editor))
            }
            AppButton(
                onClick = {
                    editingWallId = null
                    onOpenSavedWallForHoldScoring(wallId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.edit_menu_hold_scoring))
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
    onEdit: () -> Unit,
    onCreateChallenge: () -> Unit,
    onViewSavedChallenges: () -> Unit,
    onDelete: () -> Unit
) {
    val compactButtonPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WallThumbnail(
                imageFilePath = wall.imageFilePath,
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(1f)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.hold_count_label, wall.holdCount),
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = compactButtonPadding,
                    minHeight = 24.dp
                ) {
                    Text(stringResource(R.string.edit_holds))
                }
                AppButton(
                    onClick = onCreateChallenge,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = compactButtonPadding,
                    minHeight = 24.dp
                ) {
                    Text(stringResource(R.string.create_challenge))
                }
                AppButton(
                    onClick = onViewSavedChallenges,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = compactButtonPadding,
                    minHeight = 24.dp
                ) {
                    Text(stringResource(R.string.open_saved_challenges))
                }
                AppOutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = compactButtonPadding,
                    minHeight = 24.dp
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}
