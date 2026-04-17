package com.example.holddetector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.model.SavedWallSummary
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.components.WallThumbnail
import com.example.holddetector.ui.selectors.findWallPendingDeletion
import com.example.holddetector.ui.selectors.formatWallTimestamp

@Composable
fun WallListScreen(
    savedWalls: List<SavedWallSummary>,
    onNewWallClick: () -> Unit,
    onOpenSavedWallForEditing: (String) -> Unit,
    onOpenSavedWallForChallenge: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var deletingWallId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.wall_list_title),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.wall_list_description),
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )

        Button(
            onClick = onNewWallClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.new_wall_button))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (savedWalls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
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
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedWalls, key = { it.id }) { wall ->
                    SavedWallCard(
                        wall = wall,
                        onEdit = { onOpenSavedWallForEditing(wall.id) },
                        onCreateChallenge = { onOpenSavedWallForChallenge(wall.id) },
                        onDelete = { deletingWallId = wall.id }
                    )
                }
            }
        }
    }

    deletingWallId?.let { wallId ->
        val wall = findWallPendingDeletion(savedWalls, wallId)
        AlertDialog(
            onDismissRequest = { deletingWallId = null },
            title = { Text(stringResource(R.string.delete_wall_title)) },
            text = { Text(stringResource(R.string.delete_wall_message, wall?.title ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        deletingWallId = null
                        onDeleteSavedWall(wallId)
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingWallId = null }) {
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
                    text = wall.title,
                    color = AppTextColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.updated_at_label, formatWallTimestamp(wall.updatedAt)),
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
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.edit_holds))
                }
                Button(onClick = onCreateChallenge, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.create_challenge))
                }
            }
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}
