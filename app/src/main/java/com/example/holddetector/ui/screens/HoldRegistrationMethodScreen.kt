package com.example.holddetector.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.holddetector.R
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSurfaceColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.components.AppButton

@Composable
fun HoldRegistrationMethodScreen(
    bitmap: Bitmap?,
    onBackToCamera: () -> Unit,
    onOpenManualRegistration: () -> Unit,
    onOpenAutoExtraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = AppSurfaceColor,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.registration_method_title),
                        color = AppTextColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = stringResource(R.string.registration_method_description),
                        color = AppSecondaryTextColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp)
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
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 20.dp)
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
