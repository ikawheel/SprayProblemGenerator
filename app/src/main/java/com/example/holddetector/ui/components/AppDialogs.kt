package com.example.holddetector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.ui.AppTextColor

@Composable
fun AppContentDialog(
    title: String,
    onDismissRequest: () -> Unit,
    dismissText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                color = AppTextColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        },
        confirmButton = {},
        dismissButton = {
            AppOutlinedButton(onClick = onDismissRequest) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun AppMessageDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
    dismissText: String
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                color = AppTextColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                color = AppTextColor,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {},
        dismissButton = {
            AppOutlinedButton(onClick = onDismissRequest) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                color = AppTextColor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message,
                    color = AppTextColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                AppButton(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(confirmText)
                }

                AppOutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(dismissText)
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
