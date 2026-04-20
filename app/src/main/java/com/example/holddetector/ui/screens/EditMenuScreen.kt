package com.example.holddetector.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.MainUiState
import com.example.holddetector.ui.components.AppButton

@Composable
fun EditMenuScreen(
    state: MainUiState,
    onOpenReachCalibration: () -> Unit,
    onOpenHoldEditor: () -> Unit,
    onOpenHoldScoring: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.edit_menu_title),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = state.wallTitle,
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = stringResource(R.string.edit_menu_description),
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        AppButton(
            onClick = onOpenReachCalibration,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.edit_menu_reach_calibration))
        }

        AppButton(
            onClick = onOpenHoldEditor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.edit_menu_hold_editor))
        }

        AppButton(
            onClick = onOpenHoldScoring,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.edit_menu_hold_scoring))
        }
    }
}
