package com.example.holddetector.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.holddetector.R
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.model.SavedWallSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val AppBackgroundColor = Color(0xFFF7F7F7)
private val AppSurfaceColor = Color.White
private val AppSubtleSurfaceColor = Color(0xFFF1F3F5)
private val AppTextColor = Color(0xFF1F2937)
private val AppSecondaryTextColor = Color(0xFF6B7280)
private val AppOverlayBackgroundColor = Color(0xEFFFFFFF)
private val AppOverlayStrokePreviewColor = Color(0x44222222)
private val AppBusyOverlayColor = Color(0x66FFFFFF)
private val CameraScreenBackgroundColor = Color.Black
private val CameraControlOverlayColor = Color(0x66000000)
private const val DefaultHoldStrokeWidth = 1f

class MainActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var orientationEventListener: OrientationEventListener
    private val cameraPermissionGranted = mutableStateOf(false)
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
                        cameraPermissionGranted = cameraPermissionGranted.value,
                        onRequestCameraPermission = {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onNewWallClick = viewModel::startNewWall,
                        onOpenSavedWallForEditing = viewModel::openSavedWallForEditing,
                        onOpenSavedWallForChallenge = viewModel::openSavedWallForChallenge,
                        onDeleteSavedWall = viewModel::deleteSavedWall,
                        onCaptureClick = ::capturePhoto,
                        onBindPreview = ::bindCameraPreview,
                        onBackToList = viewModel::requestBackToList,
                        onSaveWall = viewModel::saveWallAndReturnToList,
                        onSaveWallAndOpenChallenge = viewModel::saveWallAndOpenChallenge,
                        onWallTitleChanged = viewModel::onWallTitleChanged,
                        onDeleteSelectedHold = viewModel::removeSelectedHold,
                        onEditorHoldTapped = viewModel::onEditorHoldTapped,
                        onChallengeHoldTapped = viewModel::onChallengeHoldTapped,
                        onManualHoldCreated = viewModel::addManualHold,
                        onStartGoalSelection = viewModel::startChallengeStartGoalSelection,
                        onDrawClick = viewModel::drawRandomChallengeHolds,
                        onDrawCountChange = viewModel::onDrawCountChanged,
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

        val photoFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        currentImageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val rawBitmap = loadCorrectedBitmap(photoFile)
                    if (rawBitmap == null) {
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
                        viewModel.onPhotoCaptured(
                            bitmap = bitmap,
                            capturedOrientation = capturedOrientation,
                            capturedRotationDegrees = 0
                        )
                    }
                }

                override fun onError(unused: ImageCaptureException) = Unit
            }
        )
    }

    private fun loadCorrectedBitmap(file: File): Bitmap? {
        val rawBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return rawBitmap
        }

        return Bitmap.createBitmap(
            rawBitmap,
            0,
            0,
            rawBitmap.width,
            rawBitmap.height,
            matrix,
            true
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

private fun orientBitmapForCaptureRotation(
    bitmap: Bitmap,
    rotationDegrees: Int
): Bitmap {
    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360
    if (normalizedRotation == 0) return bitmap

    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        Matrix().apply { postRotate(normalizedRotation.toFloat()) },
        true
    )
}

@Composable
private fun HoldDetectorApp(
    state: MainUiState,
    cameraPermissionGranted: Boolean,
    onRequestCameraPermission: () -> Unit,
    onNewWallClick: () -> Unit,
    onOpenSavedWallForEditing: (String) -> Unit,
    onOpenSavedWallForChallenge: (String) -> Unit,
    onDeleteSavedWall: (String) -> Unit,
    onCaptureClick: () -> Unit,
    onBindPreview: (PreviewView) -> Unit,
    onBackToList: () -> Unit,
    onSaveWall: () -> Unit,
    onSaveWallAndOpenChallenge: () -> Unit,
    onWallTitleChanged: (String) -> Unit,
    onDeleteSelectedHold: () -> Unit,
    onEditorHoldTapped: (Int?) -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    onStartGoalSelection: () -> Unit,
    onDrawClick: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onClearChallenge: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onDiscardChanges: () -> Unit
) {
    val contentPadding = if (state.currentScreen == AppScreen.CAMERA) 0.dp else 16.dp
    val backgroundColor = if (state.currentScreen == AppScreen.CAMERA) {
        CameraScreenBackgroundColor
    } else {
        AppBackgroundColor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(contentPadding)
    ) {
        when (state.currentScreen) {
            AppScreen.LIST -> {
                WallListScreen(
                    savedWalls = state.savedWalls,
                    onNewWallClick = onNewWallClick,
                    onOpenSavedWallForEditing = onOpenSavedWallForEditing,
                    onOpenSavedWallForChallenge = onOpenSavedWallForChallenge,
                    onDeleteSavedWall = onDeleteSavedWall,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.CAMERA -> {
                CameraFullscreenScreen(
                    cameraPermissionGranted = cameraPermissionGranted,
                    onRequestCameraPermission = onRequestCameraPermission,
                    onCaptureClick = onCaptureClick,
                    onBindPreview = onBindPreview,
                    onBackToList = onBackToList,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.HOLD_EDITOR -> {
                HoldEditorScreen(
                    state = state,
                    onWallTitleChanged = onWallTitleChanged,
                    onSaveWall = onSaveWall,
                    onSaveWallAndOpenChallenge = onSaveWallAndOpenChallenge,
                    onBackToList = onBackToList,
                    onDeleteSelectedHold = onDeleteSelectedHold,
                    onEditorHoldTapped = onEditorHoldTapped,
                    onManualHoldCreated = onManualHoldCreated,
                    modifier = Modifier.fillMaxSize()
                )
            }

            AppScreen.CHALLENGE_CREATOR -> {
                ChallengeCreatorScreen(
                    state = state,
                    onBackToList = onBackToList,
                    onChallengeHoldTapped = onChallengeHoldTapped,
                    onStartGoalSelection = onStartGoalSelection,
                    onDrawClick = onDrawClick,
                    onDrawCountChange = onDrawCountChange,
                    onClearChallenge = onClearChallenge,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (state.isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBusyOverlayColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscardDialog,
            title = { Text(stringResource(R.string.back_to_list)) },
            text = { Text(stringResource(R.string.discard_dialog_message)) },
            confirmButton = {
                Button(onClick = onDiscardChanges) {
                    Text(stringResource(R.string.discard_dialog_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissDiscardDialog) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun WallListScreen(
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
        val wall = savedWalls.firstOrNull { it.id == wallId }
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
                    text = stringResource(R.string.updated_at_label, formatTimestamp(wall.updatedAt)),
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

@Composable
private fun WallThumbnail(
    imageFilePath: String,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = imageFilePath) {
        value = withContext(Dispatchers.IO) {
            decodeThumbnailBitmap(imageFilePath, 400, 400)
        }
    }

    Box(
        modifier = modifier.background(AppSubtleSurfaceColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = stringResource(R.string.no_image),
                color = AppSecondaryTextColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CameraScreen(
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
                    Button(onClick = onRequestCameraPermission) {
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
            OutlinedButton(
                onClick = onBackToList,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.back_to_list))
            }

            Button(
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
private fun CameraFullscreenScreen(
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
                    Button(onClick = onRequestCameraPermission) {
                        Text(stringResource(R.string.allow_permission))
                    }
                }
            }
        }

        OutlinedButton(
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
            Button(
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
private fun HoldEditorScreen(
    state: MainUiState,
    onWallTitleChanged: (String) -> Unit,
    onSaveWall: () -> Unit,
    onSaveWallAndOpenChallenge: () -> Unit,
    onBackToList: () -> Unit,
    onDeleteSelectedHold: () -> Unit,
    onEditorHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap

    Column(modifier = modifier) {
        Text(
            text = stringResource(
                if (state.currentWallId == null) {
                    R.string.hold_editor_title_create
                } else {
                    R.string.hold_editor_title_edit
                }
            ),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = state.wallTitle,
            onValueChange = onWallTitleChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.wall_title_label)) }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp)
                .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                .clipToBounds()
        ) {
            if (bitmap != null) {
                HoldCanvasScreen(
                    bitmap = bitmap,
                    holds = state.holds,
                    selectedIndex = state.selectedHoldIndex,
                    challengeHoldIndices = emptySet(),
                    startHoldIndex = null,
                    goalHoldIndex = null,
                    routeSelectionMode = RouteSelectionMode.NONE,
                    onHoldTapped = onEditorHoldTapped,
                    onManualHoldCreated = onManualHoldCreated,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = stringResource(R.string.hold_editor_help),
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.hold_count_label, state.holds.size),
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackToList,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.back_to_list))
            }

            Button(
                onClick = onDeleteSelectedHold,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.delete_selected), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = onSaveWall,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    stringResource(
                        if (state.currentWallId == null) R.string.save else R.string.overwrite_save
                    )
                )
            }
        }

        Button(
            onClick = onSaveWallAndOpenChallenge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.save_and_open_challenge))
        }
    }
}

@Composable
private fun ChallengeCreatorScreen(
    state: MainUiState,
    onBackToList: () -> Unit,
    onChallengeHoldTapped: (Int?) -> Unit,
    onStartGoalSelection: () -> Unit,
    onDrawClick: () -> Unit,
    onDrawCountChange: (String) -> Unit,
    onClearChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = state.capturedBitmap

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.challenge_creator_title),
            color = AppTextColor,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = state.wallTitle,
            color = AppSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AppSubtleSurfaceColor, RoundedCornerShape(16.dp))
                .clipToBounds()
        ) {
            if (bitmap != null) {
                ChallengeCanvasScreen(
                    bitmap = bitmap,
                    holds = state.holds,
                    selectedIndex = state.selectedHoldIndex,
                    challengeHoldIndices = state.challengeHoldIndices,
                    startHoldIndex = state.startHoldIndex,
                    goalHoldIndex = state.goalHoldIndex,
                    routeSelectionMode = state.routeSelectionMode,
                    onHoldTapped = onChallengeHoldTapped,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = when (state.routeSelectionMode) {
                RouteSelectionMode.NONE -> stringResource(R.string.challenge_route_help_none)
                RouteSelectionMode.SELECTING_START -> stringResource(R.string.challenge_route_help_select_start)
                RouteSelectionMode.SELECTING_GOAL -> stringResource(R.string.challenge_route_help_select_goal)
            },
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        Text(
            text = stringResource(
                R.string.challenge_selection_summary,
                state.challengeHoldIndices.size,
                stringResource(
                    if (state.startHoldIndex != null) R.string.status_set else R.string.status_unset
                ),
                stringResource(
                    if (state.goalHoldIndex != null) R.string.status_set else R.string.status_unset
                )
            ),
            color = AppTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.drawCountInput,
                onValueChange = onDrawCountChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.draw_count_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = onDrawClick,
                modifier = Modifier.height(56.dp)
            ) {
                Text(stringResource(R.string.draw))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartGoalSelection,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = when (state.routeSelectionMode) {
                        RouteSelectionMode.NONE -> stringResource(R.string.start_goal_select)
                        RouteSelectionMode.SELECTING_START -> stringResource(R.string.selecting_start)
                        RouteSelectionMode.SELECTING_GOAL -> stringResource(R.string.selecting_goal)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onClearChallenge,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.clear_challenge))
            }
        }

        OutlinedButton(
            onClick = onBackToList,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.back_to_list))
        }
    }
}

@Composable
private fun CameraPreview(
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

private enum class CanvasMode {
    HOLD_EDITOR,
    CHALLENGE
}

@Composable
private fun HoldCanvasScreen(
    bitmap: Bitmap,
    holds: List<Hold>,
    selectedIndex: Int?,
    challengeHoldIndices: Set<Int>,
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    routeSelectionMode: RouteSelectionMode,
    onHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveCapturedImage(
        bitmap = bitmap,
        holds = holds,
        selectedIndex = selectedIndex,
        challengeHoldIndices = challengeHoldIndices,
        startHoldIndex = startHoldIndex,
        goalHoldIndex = goalHoldIndex,
        routeSelectionMode = routeSelectionMode,
        mode = CanvasMode.HOLD_EDITOR,
        onHoldTapped = onHoldTapped,
        onManualHoldCreated = onManualHoldCreated,
        modifier = modifier
    )
}

@Composable
private fun ChallengeCanvasScreen(
    bitmap: Bitmap,
    holds: List<Hold>,
    selectedIndex: Int?,
    challengeHoldIndices: Set<Int>,
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    routeSelectionMode: RouteSelectionMode,
    onHoldTapped: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    InteractiveCapturedImage(
        bitmap = bitmap,
        holds = holds,
        selectedIndex = selectedIndex,
        challengeHoldIndices = challengeHoldIndices,
        startHoldIndex = startHoldIndex,
        goalHoldIndex = goalHoldIndex,
        routeSelectionMode = routeSelectionMode,
        mode = CanvasMode.CHALLENGE,
        onHoldTapped = onHoldTapped,
        onManualHoldCreated = {},
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InteractiveCapturedImage(
    bitmap: Bitmap,
    holds: List<Hold>,
    selectedIndex: Int?,
    challengeHoldIndices: Set<Int>,
    startHoldIndex: Int?,
    goalHoldIndex: Int?,
    routeSelectionMode: RouteSelectionMode,
    mode: CanvasMode,
    onHoldTapped: (Int?) -> Unit,
    onManualHoldCreated: (Hold) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var activePointerCount by remember { mutableIntStateOf(0) }
    var draftPreviewPolygon by remember { mutableStateOf<LocalHoldPolygon?>(null) }
    var draftStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val density = LocalDensity.current
    val brushRadiusXLocal = 10f
    val brushRadiusYLocal = 8f
    val tapRadiusXLocal = 10f
    val tapRadiusYLocal = 8f
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 30f
            isAntiAlias = true
        }
    }

    val baseLayout = remember(bitmap.width, bitmap.height, containerSize) {
        calculateBaseImageLayout(
            containerWidth = containerSize.width.toFloat(),
            containerHeight = containerSize.height.toFloat(),
            imageWidth = bitmap.width.toFloat(),
            imageHeight = bitmap.height.toFloat()
        )
    }
    val selectedChallengePath = remember(holds, challengeHoldIndices, baseLayout, mode) {
        if (mode != CanvasMode.CHALLENGE || !baseLayout.isValid || challengeHoldIndices.isEmpty()) {
            null
        } else {
            Path().apply {
                challengeHoldIndices.sorted().forEach { index ->
                    holds.getOrNull(index)?.let { hold ->
                        addPath(hold.toLocalPolygon(baseLayout).toPath())
                    }
                }
            }
        }
    }
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    Box(
        modifier = modifier
            .background(AppBackgroundColor)
            .clipToBounds()
            .onSizeChanged { containerSize = it }
            .pointerInput(baseLayout, containerSize) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (!baseLayout.isValid) return@detectTransformGestures

                    val previousScale = zoomScale
                    val updatedScale = (previousScale * zoom).coerceIn(1f, 5f)
                    val zoomFactor = updatedScale / previousScale
                    val contentOrigin = Offset(baseLayout.left, baseLayout.top)

                    val candidatePanOffset = if (activePointerCount >= 2) {
                        (panOffset * zoomFactor) +
                            pan +
                            ((centroid - contentOrigin) * (1f - zoomFactor))
                    } else {
                        panOffset
                    }

                    zoomScale = updatedScale
                    panOffset = clampPanOffset(
                        candidate = candidatePanOffset,
                        containerSize = containerSize,
                        baseLayout = baseLayout,
                        zoomScale = updatedScale
                    )
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        activePointerCount = event.changes.count { it.pressed }
                    } while (activePointerCount > 0)
                    activePointerCount = 0
                }
            }
            .pointerInput(baseLayout, zoomScale, panOffset, holds, routeSelectionMode, mode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    if (!baseLayout.isValid || activePointerCount > 1) {
                        draftPreviewPolygon = null
                        draftStrokePoints = emptyList()
                        return@awaitEachGesture
                    }

                    val startLocal = screenToLocalPoint(
                        screenPoint = down.position,
                        baseLayout = baseLayout,
                        panOffset = panOffset,
                        zoomScale = zoomScale
                    )

                    if (!isInsideLocalBounds(startLocal, baseLayout)) {
                        draftPreviewPolygon = null
                        draftStrokePoints = emptyList()
                        return@awaitEachGesture
                    }

                    if (mode == CanvasMode.CHALLENGE) {
                        var movedEnough = false
                        var multiTouchDetected = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size > 1) {
                                multiTouchDetected = true
                                break
                            }
                            val currentLocal = screenToLocalPoint(
                                screenPoint = pressed.first().position,
                                baseLayout = baseLayout,
                                panOffset = panOffset,
                                zoomScale = zoomScale
                            )
                            val dx = currentLocal.x - startLocal.x
                            val dy = currentLocal.y - startLocal.y
                            if (abs(dx) > 4f || abs(dy) > 4f) {
                                movedEnough = true
                            }
                        }
                        if (!multiTouchDetected && !movedEnough) {
                            onHoldTapped(
                                findTappedIndexFromLocal(
                                    localPoint = startLocal,
                                    holds = holds,
                                    baseLayout = baseLayout
                                )
                            )
                        }
                        return@awaitEachGesture
                    }

                    var currentLocal = startLocal
                    var movedEnough = false
                    var multiTouchDetected = false
                    draftPreviewPolygon = null
                    draftStrokePoints = listOf(startLocal)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isEmpty()) {
                            break
                        }

                        if (pressed.size > 1) {
                            multiTouchDetected = true
                            draftPreviewPolygon = null
                            draftStrokePoints = emptyList()
                            break
                        }

                        currentLocal = screenToLocalPoint(
                            screenPoint = pressed.first().position,
                            baseLayout = baseLayout,
                            panOffset = panOffset,
                            zoomScale = zoomScale
                        )

                        val dx = currentLocal.x - startLocal.x
                        val dy = currentLocal.y - startLocal.y

                        if (abs(dx) > 4f || abs(dy) > 4f) {
                            movedEnough = true
                            val updatedPoints = appendPointIfNeeded(
                                draftStrokePoints,
                                currentLocal,
                                minDistance = 3f,
                                baseLayout = baseLayout
                            )
                            draftStrokePoints = updatedPoints
                            draftPreviewPolygon = buildContourPolygonFromBrushPoints(
                                points = updatedPoints,
                                brushRadiusX = brushRadiusXLocal,
                                brushRadiusY = brushRadiusYLocal,
                                baseLayout = baseLayout
                            )
                        }
                    }

                    if (!multiTouchDetected) {
                        if (movedEnough) {
                            createManualHoldFromBrushPoints(
                                points = draftStrokePoints,
                                brushRadiusX = brushRadiusXLocal,
                                brushRadiusY = brushRadiusYLocal,
                                baseLayout = baseLayout,
                                imageWidth = bitmap.width,
                                imageHeight = bitmap.height,
                                onManualHoldCreated = onManualHoldCreated
                            )
                        } else {
                            val tappedIndex = findTappedIndexFromLocal(
                                localPoint = startLocal,
                                holds = holds,
                                baseLayout = baseLayout
                            )
                            if (tappedIndex != null) {
                                onHoldTapped(tappedIndex)
                            } else {
                                createTapHoldFromLocalPoint(
                                    localPoint = startLocal,
                                    localRadiusX = tapRadiusXLocal,
                                    localRadiusY = tapRadiusYLocal,
                                    baseLayout = baseLayout,
                                    imageWidth = bitmap.width,
                                    imageHeight = bitmap.height,
                                    onManualHoldCreated = onManualHoldCreated
                                )
                            }
                        }
                    }

                    draftPreviewPolygon = null
                    draftStrokePoints = emptyList()
                }
            }
    ) {
        if (baseLayout.isValid) {
            val transformedOffset = IntOffset(
                (baseLayout.left + panOffset.x).roundToInt(),
                (baseLayout.top + panOffset.y).roundToInt()
            )

            val imageWidthDp = with(density) { baseLayout.drawWidth.toDp() }
            val imageHeightDp = with(density) { baseLayout.drawHeight.toDp() }

            Box(
                modifier = Modifier
                    .offset { transformedOffset }
                    .size(imageWidthDp, imageHeightDp)
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (mode == CanvasMode.CHALLENGE && selectedChallengePath != null) {
                        grayscaleFilter
                    } else {
                        null
                    }
                )

                if (mode == CanvasMode.CHALLENGE && selectedChallengePath != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                val selectedPath = selectedChallengePath
                                if (selectedPath != null) {
                                    clipPath(selectedPath) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            }
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    holds.forEachIndexed { index, hold ->
                        val shouldDrawOutline = mode == CanvasMode.HOLD_EDITOR ||
                            challengeHoldIndices.contains(index)
                        if (!shouldDrawOutline) return@forEachIndexed

                        val polygon = hold.toLocalPolygon(baseLayout)
                        val strokeColor = when {
                            index == startHoldIndex -> Color(0xFF0284C7)
                            index == goalHoldIndex -> Color(0xFFCA8A04)
                            index == selectedIndex -> Color.Red
                            challengeHoldIndices.contains(index) -> Color.Yellow
                            else -> Color.Green
                        }

                        drawPath(
                            path = polygon.toPath(),
                            color = strokeColor,
                            style = Stroke(width = DefaultHoldStrokeWidth)
                        )

                        val label = buildList {
                            if (index == startHoldIndex) add("S")
                            if (index == goalHoldIndex) add("G")
                        }.joinToString("/")

                        if (label.isNotEmpty()) {
                            val labelTop = (polygon.minY - 36f).coerceAtLeast(0f)
                            val labelLeft = polygon.minX.coerceAtLeast(0f)
                            val labelWidth = max(textPaint.measureText(label) + 18f, 32f)

                            drawRect(
                                color = AppOverlayBackgroundColor,
                                topLeft = Offset(labelLeft, labelTop),
                                size = androidx.compose.ui.geometry.Size(labelWidth, 34f)
                            )

                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    label,
                                    labelLeft + 8f,
                                    (labelTop + 25f).coerceAtLeast(24f),
                                    textPaint
                                )
                            }
                        }
                    }

                    if (mode == CanvasMode.HOLD_EDITOR && routeSelectionMode == RouteSelectionMode.NONE) {
                        draftStrokePoints.forEach { point ->
                            drawOval(
                                color = AppOverlayStrokePreviewColor,
                                topLeft = Offset(point.x - brushRadiusXLocal, point.y - brushRadiusYLocal),
                                size = androidx.compose.ui.geometry.Size(
                                    brushRadiusXLocal * 2f,
                                    brushRadiusYLocal * 2f
                                )
                            )
                        }

                        draftPreviewPolygon?.let { polygon ->
                            drawPath(
                                path = polygon.toPath(),
                                color = Color.Cyan,
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                }
            }
        }

    }
}

private data class BaseImageLayout(
    val left: Float,
    val top: Float,
    val drawWidth: Float,
    val drawHeight: Float,
    val fitScale: Float
) {
    val isValid: Boolean get() = drawWidth > 0f && drawHeight > 0f && fitScale > 0f
}

private data class LocalHoldPolygon(
    val points: List<Offset>
) {
    init {
        require(points.size >= 3) { "Polygon requires at least 3 points" }
    }

    val minX: Float get() = points.minOf { it.x }
    val maxX: Float get() = points.maxOf { it.x }
    val minY: Float get() = points.minOf { it.y }
    val maxY: Float get() = points.maxOf { it.y }

    fun toPath(): Path {
        return Path().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 1 until points.size) {
                lineTo(points[index].x, points[index].y)
            }
            close()
        }
    }
}

private fun calculateBaseImageLayout(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float
): BaseImageLayout {
    if (containerWidth <= 0f || containerHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return BaseImageLayout(0f, 0f, 0f, 0f, 0f)
    }

    val fitScale = minOf(containerWidth / imageWidth, containerHeight / imageHeight)
    val drawWidth = imageWidth * fitScale
    val drawHeight = imageHeight * fitScale

    return BaseImageLayout(
        left = (containerWidth - drawWidth) / 2f,
        top = (containerHeight - drawHeight) / 2f,
        drawWidth = drawWidth,
        drawHeight = drawHeight,
        fitScale = fitScale
    )
}

private fun clampPanOffset(
    candidate: Offset,
    containerSize: IntSize,
    baseLayout: BaseImageLayout,
    zoomScale: Float
): Offset {
    if (!baseLayout.isValid) return Offset.Zero

    val scaledWidth = baseLayout.drawWidth * zoomScale
    val scaledHeight = baseLayout.drawHeight * zoomScale
    val minX = containerSize.width - baseLayout.left - scaledWidth
    val maxX = -baseLayout.left
    val minY = containerSize.height - baseLayout.top - scaledHeight
    val maxY = -baseLayout.top

    val clampedX = candidate.x.coerceIn(
        minimumValue = minOf(minX, maxX),
        maximumValue = maxOf(minX, maxX)
    )

    val clampedY = candidate.y.coerceIn(
        minimumValue = minOf(minY, maxY),
        maximumValue = maxOf(minY, maxY)
    )

    return Offset(
        x = clampedX,
        y = clampedY
    )
}

private fun screenToLocalPoint(
    screenPoint: Offset,
    baseLayout: BaseImageLayout,
    panOffset: Offset,
    zoomScale: Float
): Offset {
    return Offset(
        x = (screenPoint.x - baseLayout.left - panOffset.x) / zoomScale,
        y = (screenPoint.y - baseLayout.top - panOffset.y) / zoomScale
    )
}

private fun isInsideLocalBounds(
    localPoint: Offset,
    baseLayout: BaseImageLayout
): Boolean {
    return localPoint.x in 0f..baseLayout.drawWidth &&
        localPoint.y in 0f..baseLayout.drawHeight
}

private fun Hold.toLocalPolygon(baseLayout: BaseImageLayout): LocalHoldPolygon {
    return LocalHoldPolygon(
        points = points.map { point ->
            Offset(
                x = point.x * baseLayout.fitScale,
                y = point.y * baseLayout.fitScale
            )
        }
    )
}

private fun appendPointIfNeeded(
    currentPoints: List<Offset>,
    candidatePoint: Offset,
    minDistance: Float,
    baseLayout: BaseImageLayout
): List<Offset> {
    val clamped = Offset(
        x = candidatePoint.x.coerceIn(0f, baseLayout.drawWidth),
        y = candidatePoint.y.coerceIn(0f, baseLayout.drawHeight)
    )

    val lastPoint = currentPoints.lastOrNull() ?: return listOf(clamped)
    val dx = clamped.x - lastPoint.x
    val dy = clamped.y - lastPoint.y
    return if (dx * dx + dy * dy >= minDistance * minDistance) {
        currentPoints + clamped
    } else {
        currentPoints
    }
}

private fun buildContourPolygonFromBrushPoints(
    points: List<Offset>,
    brushRadiusX: Float,
    brushRadiusY: Float,
    baseLayout: BaseImageLayout
): LocalHoldPolygon? {
    if (points.isEmpty() || !baseLayout.isValid) return null

    val strokeMask = rasterizeBrushStrokeToMask(
        points = points,
        brushRadiusX = brushRadiusX,
        brushRadiusY = brushRadiusY,
        baseLayout = baseLayout
    )

    val boundaryPixels = traceBoundaryPixels(strokeMask) ?: return null
    val contourPoints = boundaryPixels.map { pixel ->
        Offset(
            x = strokeMask.localLeft + (pixel.x - 1) + 0.5f,
            y = strokeMask.localTop + (pixel.y - 1) + 0.5f
        )
    }

    val thinnedPoints = thinBoundaryPoints(
        points = contourPoints,
        minDistance = 2f
    )

    return if (thinnedPoints.size >= 3) {
        LocalHoldPolygon(thinnedPoints)
    } else {
        null
    }
}

private data class StrokeMask(
    val width: Int,
    val height: Int,
    val localLeft: Float,
    val localTop: Float,
    val pixels: BooleanArray
) {
    fun isFilled(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        return pixels[y * width + x]
    }

    fun setFilled(x: Int, y: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        pixels[y * width + x] = true
    }
}

private data class RasterPixel(
    val x: Int,
    val y: Int
)

private fun rasterizeBrushStrokeToMask(
    points: List<Offset>,
    brushRadiusX: Float,
    brushRadiusY: Float,
    baseLayout: BaseImageLayout
): StrokeMask {
    val safeRadiusX = brushRadiusX.coerceAtLeast(1f)
    val safeRadiusY = brushRadiusY.coerceAtLeast(1f)

    val minPointX = points.minOf { it.x }
    val maxPointX = points.maxOf { it.x }
    val minPointY = points.minOf { it.y }
    val maxPointY = points.maxOf { it.y }

    val localLeft = kotlin.math.floor((minPointX - safeRadiusX - 2f).coerceAtLeast(0f))
    val localTop = kotlin.math.floor((minPointY - safeRadiusY - 2f).coerceAtLeast(0f))
    val localRight = kotlin.math.ceil((maxPointX + safeRadiusX + 2f).coerceAtMost(baseLayout.drawWidth))
    val localBottom = kotlin.math.ceil((maxPointY + safeRadiusY + 2f).coerceAtMost(baseLayout.drawHeight))

    val usableWidth = (localRight - localLeft).toInt().coerceAtLeast(1)
    val usableHeight = (localBottom - localTop).toInt().coerceAtLeast(1)
    val maskWidth = usableWidth + 2
    val maskHeight = usableHeight + 2

    val mask = StrokeMask(
        width = maskWidth,
        height = maskHeight,
        localLeft = localLeft,
        localTop = localTop,
        pixels = BooleanArray(maskWidth * maskHeight)
    )

    val densePoints = densifyStrokePoints(
        points = points,
        step = (minOf(safeRadiusX, safeRadiusY) * 0.45f).coerceAtLeast(2f)
    )

    densePoints.forEach { point ->
        fillEllipseOnMask(
            mask = mask,
            centerX = point.x - localLeft + 1f,
            centerY = point.y - localTop + 1f,
            radiusX = safeRadiusX,
            radiusY = safeRadiusY
        )
    }

    return mask
}

private fun densifyStrokePoints(
    points: List<Offset>,
    step: Float
): List<Offset> {
    if (points.isEmpty()) return emptyList()
    if (points.size == 1) return points

    val densePoints = mutableListOf(points.first())
    val safeStep = step.coerceAtLeast(1f)

    for (index in 0 until points.lastIndex) {
        val start = points[index]
        val end = points[index + 1]
        val dx = end.x - start.x
        val dy = end.y - start.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val segments = kotlin.math.ceil(distance / safeStep).toInt().coerceAtLeast(1)

        for (segment in 1..segments) {
            val t = segment.toFloat() / segments.toFloat()
            densePoints += Offset(
                x = start.x + dx * t,
                y = start.y + dy * t
            )
        }
    }

    return densePoints
}

private fun fillEllipseOnMask(
    mask: StrokeMask,
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float
) {
    val safeRadiusX = radiusX.coerceAtLeast(1f)
    val safeRadiusY = radiusY.coerceAtLeast(1f)

    val left = kotlin.math.floor(centerX - safeRadiusX).toInt()
    val right = kotlin.math.ceil(centerX + safeRadiusX).toInt()
    val top = kotlin.math.floor(centerY - safeRadiusY).toInt()
    val bottom = kotlin.math.ceil(centerY + safeRadiusY).toInt()

    for (y in top..bottom) {
        for (x in left..right) {
            val normalizedX = ((x + 0.5f) - centerX) / safeRadiusX
            val normalizedY = ((y + 0.5f) - centerY) / safeRadiusY
            if (normalizedX * normalizedX + normalizedY * normalizedY <= 1f) {
                mask.setFilled(x, y)
            }
        }
    }
}

private fun traceBoundaryPixels(mask: StrokeMask): List<RasterPixel>? {
    val start = findFirstBoundaryPixel(mask) ?: return null
    val directions = listOf(
        RasterPixel(-1, 0),
        RasterPixel(-1, -1),
        RasterPixel(0, -1),
        RasterPixel(1, -1),
        RasterPixel(1, 0),
        RasterPixel(1, 1),
        RasterPixel(0, 1),
        RasterPixel(-1, 1)
    )

    var current = start
    var previous = RasterPixel(start.x - 1, start.y)
    val startPrevious = previous
    val boundary = mutableListOf<RasterPixel>()
    var guard = 0

    while (guard < mask.width * mask.height * 4) {
        guard++
        boundary += current

        val relativePrevious = RasterPixel(previous.x - current.x, previous.y - current.y)
        val startDirectionIndex = directions.indexOf(relativePrevious).takeIf { it >= 0 } ?: 0

        var nextDirectionIndex: Int? = null
        for (offset in directions.indices) {
            val candidateIndex = (startDirectionIndex + offset) % directions.size
            val direction = directions[candidateIndex]
            val candidate = RasterPixel(current.x + direction.x, current.y + direction.y)
            if (mask.isFilled(candidate.x, candidate.y)) {
                nextDirectionIndex = candidateIndex
                break
            }
        }

        val foundDirectionIndex = nextDirectionIndex ?: break
        val nextDirection = directions[foundDirectionIndex]
        val previousDirection = directions[(foundDirectionIndex - 1 + directions.size) % directions.size]

        previous = RasterPixel(current.x + previousDirection.x, current.y + previousDirection.y)
        current = RasterPixel(current.x + nextDirection.x, current.y + nextDirection.y)

        if (current == start && previous == startPrevious && boundary.size > 2) {
            break
        }
    }

    return boundary.takeIf { it.size >= 3 }
}

private fun findFirstBoundaryPixel(mask: StrokeMask): RasterPixel? {
    for (y in 1 until mask.height - 1) {
        for (x in 1 until mask.width - 1) {
            if (!mask.isFilled(x, y)) continue
            if (
                !mask.isFilled(x - 1, y) ||
                    !mask.isFilled(x + 1, y) ||
                    !mask.isFilled(x, y - 1) ||
                    !mask.isFilled(x, y + 1)
            ) {
                return RasterPixel(x, y)
            }
        }
    }
    return null
}

private fun thinBoundaryPoints(
    points: List<Offset>,
    minDistance: Float
): List<Offset> {
    if (points.size <= 3) return points.distinct()

    val kept = mutableListOf(points.first())
    val minDistanceSquared = minDistance * minDistance

    for (index in 1 until points.size) {
        val point = points[index]
        val last = kept.last()
        val dx = point.x - last.x
        val dy = point.y - last.y
        if (dx * dx + dy * dy >= minDistanceSquared) {
            kept += point
        }
    }

    if (kept.size >= 3) {
        val first = kept.first()
        val last = kept.last()
        val dx = first.x - last.x
        val dy = first.y - last.y
        if (dx * dx + dy * dy < minDistanceSquared) {
            kept.removeAt(kept.lastIndex)
        }
    }

    return kept.distinct()
}

private fun createTapHoldFromLocalPoint(
    localPoint: Offset,
    localRadiusX: Float,
    localRadiusY: Float,
    baseLayout: BaseImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    onManualHoldCreated: (Hold) -> Unit
) {
    val polygon = buildContourPolygonFromBrushPoints(
        points = listOf(localPoint),
        brushRadiusX = localRadiusX,
        brushRadiusY = localRadiusY,
        baseLayout = baseLayout
    ) ?: return

    onManualHoldCreated(
        localPolygonToHold(
            polygon = polygon,
            baseLayout = baseLayout,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    )
}

private fun createManualHoldFromBrushPoints(
    points: List<Offset>,
    brushRadiusX: Float,
    brushRadiusY: Float,
    baseLayout: BaseImageLayout,
    imageWidth: Int,
    imageHeight: Int,
    onManualHoldCreated: (Hold) -> Unit
) {
    val polygon = buildContourPolygonFromBrushPoints(
        points = points,
        brushRadiusX = brushRadiusX,
        brushRadiusY = brushRadiusY,
        baseLayout = baseLayout
    ) ?: return

    onManualHoldCreated(
        localPolygonToHold(
            polygon = polygon,
            baseLayout = baseLayout,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    )
}

private fun localPolygonToHold(
    polygon: LocalHoldPolygon,
    baseLayout: BaseImageLayout,
    imageWidth: Int,
    imageHeight: Int
): Hold {
    val imagePoints = polygon.points
        .map { point ->
            HoldPoint(
                x = (point.x / baseLayout.fitScale).roundToInt().coerceIn(0, imageWidth),
                y = (point.y / baseLayout.fitScale).roundToInt().coerceIn(0, imageHeight)
            )
        }
        .distinct()

    return Hold(
        points = if (imagePoints.size >= 3) {
            imagePoints
        } else {
            listOf(
                HoldPoint(0, 0),
                HoldPoint(1, 0),
                HoldPoint(0, 1)
            )
        }
    )
}

private fun findTappedIndexFromLocal(
    localPoint: Offset,
    holds: List<Hold>,
    baseLayout: BaseImageLayout
): Int? {
    return holds.indexOfLast { hold ->
        isPointInsidePolygon(
            point = localPoint,
            polygon = hold.toLocalPolygon(baseLayout).points
        )
    }.takeIf { it >= 0 }
}

private fun isPointInsidePolygon(
    point: Offset,
    polygon: List<Offset>
): Boolean {
    if (polygon.size < 3) return false

    var inside = false
    var previous = polygon.last()

    for (current in polygon) {
        if (isPointOnSegment(point, previous, current)) {
            return true
        }

        val intersects = ((current.y > point.y) != (previous.y > point.y)) &&
            (point.x < (previous.x - current.x) * (point.y - current.y) / ((previous.y - current.y).takeIf { it != 0f } ?: 0.0001f) + current.x)

        if (intersects) {
            inside = !inside
        }
        previous = current
    }

    return inside
}

private fun isPointOnSegment(
    point: Offset,
    start: Offset,
    end: Offset,
    tolerance: Float = 3f
): Boolean {
    val cross = (point.y - start.y) * (end.x - start.x) - (point.x - start.x) * (end.y - start.y)
    if (abs(cross) > tolerance) return false

    val dot = (point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)
    if (dot < 0f) return false

    val squaredLength = (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
    if (dot > squaredLength) return false

    return true
}

private fun decodeThumbnailBitmap(
    path: String,
    reqWidth: Int,
    reqHeight: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
        inJustDecodeBounds = false
    }

    return BitmapFactory.decodeFile(path, options)
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize.coerceAtLeast(1)
}

private fun formatTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
}
