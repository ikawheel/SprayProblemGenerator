package com.ikeansoft.sprayproblemgenerator.ui.screens

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
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.ui.AppTextColor
import com.ikeansoft.sprayproblemgenerator.ui.MainUiState
import com.ikeansoft.sprayproblemgenerator.ui.components.AppButton

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
