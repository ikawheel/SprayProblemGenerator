package com.example.holddetector.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.holddetector.R
import com.example.holddetector.ui.components.AppButton
import com.example.holddetector.ui.components.AppOutlinedButton
import com.example.holddetector.ui.AppOverlayBackgroundColor
import com.example.holddetector.ui.AppSecondaryTextColor
import com.example.holddetector.ui.AppSubtleSurfaceColor
import com.example.holddetector.ui.AppTextColor
import com.example.holddetector.ui.CameraControlOverlayColor
import com.example.holddetector.ui.CameraScreenBackgroundColor

@Composable
fun CameraScreen(
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onCaptureClick: () -> Unit,
    onBindPreview: (PreviewView) -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.camera_title),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.camera_subtitle),
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (cameraPermissionGranted) {
                CameraPreview(
                    onBindPreview = onBindPreview,
                    cameraPermissionGranted = cameraPermissionGranted,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_missing),
                        color = AppTextColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    AppButton(onClick = onRequestCameraPermission) {
                        Text(stringResource(R.string.allow_permission))
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppOutlinedButton(
                onClick = onBackToList,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.back_to_list))
            }

            AppButton(
                onClick = onCaptureClick,
                enabled = cameraPermissionGranted,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.capture))
            }
        }
    }
}

@Composable
fun CameraFullscreenScreen(
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onCaptureClick: () -> Unit,
    onBindPreview: (PreviewView) -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraScreenBackgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSubtleSurfaceColor)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            if (cameraPermissionGranted) {
                CameraPreview(
                    onBindPreview = onBindPreview,
                    cameraPermissionGranted = cameraPermissionGranted,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_missing),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    AppButton(onClick = onRequestCameraPermission) {
                        Text(stringResource(R.string.allow_permission))
                    }
                }
            }
        }

        AppOutlinedButton(
            onClick = onBackToList,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .background(AppOverlayBackgroundColor, RoundedCornerShape(999.dp))
        ) {
            Text(stringResource(R.string.back_to_list))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(CameraControlOverlayColor)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            AppButton(
                onClick = onCaptureClick,
                enabled = cameraPermissionGranted,
                shape = CircleShape,
                modifier = Modifier.size(84.dp)
            ) {
                Text(stringResource(R.string.capture), maxLines = 1)
            }
        }
    }
}

@Composable
fun CameraPreview(
    onBindPreview: (PreviewView) -> Unit,
    cameraPermissionGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentBindPreview by rememberUpdatedState(onBindPreview)

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(previewView, cameraPermissionGranted) {
        if (cameraPermissionGranted) {
            currentBindPreview(previewView)
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
