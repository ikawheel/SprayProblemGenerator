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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.model.SavedWallSummary
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.components.BottomActionBar
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
    onOpenSavedWallForManualStartGoalChallenge: (String) -> Unit,
    onOpenSavedWallForRandomStartGoalChallenge: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deletingWallId by remember { mutableStateOf<String?>(null) }
    var editingWallId by remember { mutableStateOf<String?>(null) }
    var challengeWallId by remember { mutableStateOf<String?>(null) }
    var isImageSourceDialogOpen by remember { mutableStateOf(false) }
    val footerOverlayPadding = 104.dp

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = AppSurfaceColor,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenMenu) {
                        Text(
                            text = "\u2630",
                            color = AppTextColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = stringResource(R.string.wall_list_title),
                        color = AppTextColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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
                    contentPadding = PaddingValues(bottom = footerOverlayPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedWalls, key = { it.id }) { wall ->
                        SavedWallCard(
                            wall = wall,
                            onEdit = { editingWallId = wall.id },
                            onCreateChallenge = { challengeWallId = wall.id },
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
        AlertDialog(
            onDismissRequest = { editingWallId = null },
            title = { Text(stringResource(R.string.edit_menu_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            },
            confirmButton = {},
            dismissButton = {
                AppOutlinedButton(onClick = { editingWallId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    challengeWallId?.let { wallId ->
        AlertDialog(
            onDismissRequest = { challengeWallId = null },
            title = { Text(stringResource(R.string.challenge_menu_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        onClick = {
                            challengeWallId = null
                            onOpenSavedWallForManualStartGoalChallenge(wallId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.challenge_method_manual_start_goal))
                    }
                    AppButton(
                        onClick = {
                            challengeWallId = null
                            onOpenSavedWallForRandomStartGoalChallenge(wallId)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.challenge_method_random_start_goal))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                AppOutlinedButton(onClick = { challengeWallId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (isImageSourceDialogOpen) {
        AlertDialog(
            onDismissRequest = { isImageSourceDialogOpen = false },
            title = { Text(stringResource(R.string.camera_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            },
            confirmButton = {},
            dismissButton = {
                AppOutlinedButton(onClick = { isImageSourceDialogOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    deletingWallId?.let { wallId ->
        AlertDialog(
            onDismissRequest = { deletingWallId = null },
            title = { Text(stringResource(R.string.delete_wall_title)) },
            text = { Text(stringResource(R.string.delete_wall_message)) },
            confirmButton = {
                AppButton(
                    onClick = {
                        deletingWallId = null
                        onDeleteSavedWall(wallId)
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                AppOutlinedButton(onClick = { deletingWallId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SavedWallCard(
    wall: SavedWallSummary,
    onEdit: () -> Unit,
    onCreateChallenge: () -> Unit,
    onDelete: () -> Unit
) {
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.edit_holds))
                }
                AppButton(onClick = onCreateChallenge, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.create_challenge))
                }
            }
            AppOutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}
