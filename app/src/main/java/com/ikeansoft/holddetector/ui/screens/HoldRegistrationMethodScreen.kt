package com.ikeansoft.holddetector.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ikeansoft.holddetector.R
import com.ikeansoft.holddetector.ui.AppSubtleSurfaceColor
import com.ikeansoft.holddetector.ui.AppTextColor
import com.ikeansoft.holddetector.ui.components.AppButton

@Composable
fun HoldRegistrationMethodScreen(
    bitmap: Bitmap?,
    onBackToCamera: () -> Unit,
    onOpenManualRegistration: () -> Unit,
    onOpenAutoExtraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (bitmap != null && bitmap.height > 0) {
                            Modifier.aspectRatio(
                                wallImageDisplayAspectRatio(
                                    imageWidth = bitmap.width,
                                    imageHeight = bitmap.height
                                )
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                    .clipToBounds()
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .navigationBarsPadding()
            ) {
                AppButton(
                    onClick = onOpenManualRegistration,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.registration_method_manual))
                }

                AppButton(
                    onClick = onOpenAutoExtraction,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(stringResource(R.string.registration_method_auto))
                }
            }
        }
    }
}
