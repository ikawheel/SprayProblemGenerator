package com.ikeansoft.holddetector.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ikeansoft.holddetector.R
import com.mikepenz.aboutlibraries.ui.compose.android.rememberLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

@Composable
fun LicensesScreen(
    modifier: Modifier = Modifier
) {
    val libraries by rememberLibraries(R.raw.aboutlibraries)

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
                .navigationBarsPadding()
        ) {
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
