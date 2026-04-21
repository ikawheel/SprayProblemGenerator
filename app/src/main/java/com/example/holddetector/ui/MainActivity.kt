package com.example.holddetector.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Rational
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.ui.canvas.loadCorrectedBitmap
import com.example.holddetector.ui.canvas.orientBitmapForCaptureRotation
import com.example.holddetector.ui.screens.HoldDetectorApp
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val AppBackgroundColor = Color(0xFFF7F7F7)
val AppSurfaceColor = Color.White
val AppSubtleSurfaceColor = Color(0xFFF1F3F5)
val AppTextColor = Color(0xFF1F2937)
val AppSecondaryTextColor = Color(0xFF6B7280)
val AppOverlayBackgroundColor = Color(0xEFFFFFFF)
val AppStartGoalLabelBackgroundColor = Color(0x55FFFFFF)
val AppCoreHighlightBackgroundColor = Color(0x26F59E0B)
val AppCoreLabelBackgroundColor = Color(0x99FDE68A)
val AppOverlayStrokePreviewColor = Color(0x44222222)
val AppBusyOverlayColor = Color(0x66FFFFFF)
val CameraScreenBackgroundColor = Color.Black
val CameraControlOverlayColor = Color(0x66000000)
const val DefaultHoldStrokeWidth = 1f

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var orientationEventListener: OrientationEventListener
    private val cameraPermissionGranted = mutableStateOf(false)
    private val isCaptureProcessing = mutableStateOf(false)
    private var latestCapturedRotationDegrees = 0

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            cameraPermissionGranted.value = granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        cameraExecutor = Executors.newSingleThreadExecutor()
        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                latestCapturedRotationDegrees = quantizeRotationDegrees(orientation)
            }
        }
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }

        cameraPermissionGranted.value =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

        if (!cameraPermissionGranted.value) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    BackHandler(enabled = uiState.currentScreen != AppScreen.LIST) {
                        viewModel.onBackPressed()
                    }

                    HoldDetectorApp(
                        state = uiState,
                        isExternalBusy = isCaptureProcessing.value,
                        cameraPermissionGranted = cameraPermissionGranted.value,
                        onRequestCameraPermission = {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onNewWallClick = viewModel::startNewWall,
                        onOpenSavedWallForReachCalibration = viewModel::openSavedWallForReachCalibration,
                        onOpenSavedWallForHoldEditor = viewModel::openSavedWallForHoldEditor,
                        onOpenSavedWallForHoldScoring = viewModel::openSavedWallForHoldScoring,
                        onOpenSavedWallForChallenge = viewModel::openSavedWallForChallenge,
                        onDeleteSavedWall = viewModel::deleteSavedWall,
                        onCaptureClick = ::capturePhoto,
                        onBindPreview = ::bindCameraPreview,
                        onBackToList = viewModel::requestBackToList,
                        onSaveWall = viewModel::saveWallAndReturnToList,
                        onOpenHoldScoring = viewModel::openHoldScoring,
                        onBackFromHoldScoring = viewModel::returnToHoldEditorFromScoring,
                        onDifficultyScoreSelected = viewModel::setCurrentHoldDifficultyScore,
                        onSaveWallAndOpenChallenge = viewModel::saveWallAndOpenChallenge,
                        onWallTitleChanged = viewModel::onWallTitleChanged,
                        onHoldTapAreaSizeChange = viewModel::onHoldTapAreaSizeChanged,
                        onDeleteSelectedHold = viewModel::removeSelectedHold,
                        onEditorHoldTapped = viewModel::onEditorHoldTapped,
                        onChallengeHoldTapped = viewModel::onChallengeHoldTapped,
                        onManualHoldCreated = viewModel::addManualHold,
                        onContinueToHoldEditorFromReachCalibration = viewModel::continueToHoldEditorFromReachCalibration,
                        onBackFromReachCalibration = viewModel::backFromReachCalibration,
                        onStartReachCalibrationSelection = viewModel::startReachCalibrationSelection,
                        onReachCalibrationLengthInputChange = viewModel::onReachCalibrationLengthInputChanged,
                        onClearReachCalibration = viewModel::clearReachCalibration,
                        onReachCalibrationPointSelected = viewModel::onReachCalibrationPointSelected,
                        onStartGoalSelection = viewModel::startChallengeStartGoalSelection,
                        onStartDrawTargetSelection = viewModel::startDrawTargetSelection,
                        onDrawTargetSelectionCompleted = viewModel::applyDrawTargetSelection,
                        onDrawClick = viewModel::drawRandomChallengeHolds,
                        onDrawCountChange = viewModel::onDrawCountChanged,
                        onChallengeDifficultyRangeChange = viewModel::onChallengeDifficultyRangeChanged,
                        onDetourStrengthChange = viewModel::onDetourStrengthChanged,
                        onRouteWavinessChange = viewModel::onRouteWavinessChanged,
                        onStepDistanceVarianceChange = viewModel::onStepDistanceVarianceChanged,
                        onCorridorWidthChange = viewModel::onCorridorWidthChanged,
                        onClearChallenge = viewModel::clearChallengeSelection,
                        onDismissDiscardDialog = viewModel::dismissDiscardDialog,
                        onDiscardChanges = viewModel::discardEditorAndReturnToList
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView) ?: return
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun bindCameraPreview(previewView: PreviewView) {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (previewView.width <= 0 || previewView.height <= 0) {
            previewView.post { bindCameraPreview(previewView) }
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                val previewViewWidth = previewView.width.coerceAtLeast(1)
                val previewViewHeight = previewView.height.coerceAtLeast(1)
                val viewPort = ViewPort.Builder(
                    Rational(previewViewWidth, previewViewHeight),
                    targetRotation
                )
                    .setScaleType(ViewPort.FILL_CENTER)
                    .build()

                val preview = Preview.Builder()
                    .setTargetRotation(targetRotation)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageCaptureUseCase = ImageCapture.Builder()
                    .setTargetRotation(targetRotation)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = imageCaptureUseCase

                val useCaseGroup = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(imageCaptureUseCase)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCaseGroup
                )
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun capturePhoto() {
        val currentImageCapture = imageCapture ?: run {
            return
        }
        isCaptureProcessing.value = true

        val photoFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        currentImageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val rawBitmap = loadCorrectedBitmap(photoFile)
                    if (rawBitmap == null) {
                        runOnUiThread {
                            isCaptureProcessing.value = false
                        }
                        return
                    }
                    val capturedRotationDegrees = currentCapturedRotationDegrees()
                    val bitmap = orientBitmapForCaptureRotation(
                        bitmap = rawBitmap,
                        rotationDegrees = capturedRotationDegrees
                    )
                    val capturedOrientation = if (bitmap.width > bitmap.height) {
                        CapturedOrientation.LANDSCAPE
                    } else {
                        CapturedOrientation.PORTRAIT
                    }

                    runOnUiThread {
                        isCaptureProcessing.value = false
                        viewModel.onPhotoCaptured(
                            bitmap = bitmap,
                            capturedOrientation = capturedOrientation,
                            capturedRotationDegrees = 0
                        )
                    }
                }

                override fun onError(unused: ImageCaptureException) {
                    runOnUiThread {
                        isCaptureProcessing.value = false
                    }
                }
            }
        )
    }

    private fun currentCapturedRotationDegrees(): Int {
        return latestCapturedRotationDegrees
    }

    private fun quantizeRotationDegrees(orientation: Int): Int {
        return when (orientation) {
            in 45..134 -> 90
            in 135..224 -> 180
            in 225..314 -> 270
            else -> 0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationEventListener.disable()
        cameraExecutor.shutdown()
    }
}



