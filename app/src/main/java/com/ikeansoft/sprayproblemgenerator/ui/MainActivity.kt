package com.ikeansoft.sprayproblemgenerator.ui

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.model.CapturedOrientation
import com.ikeansoft.sprayproblemgenerator.ui.canvas.loadCorrectedBitmap
import com.ikeansoft.sprayproblemgenerator.ui.screens.SprayProblemGeneratorApp
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val AppBackgroundColor = Color(0xFFF7F7F7)
val AppSurfaceColor = Color.White
val AppSubtleSurfaceColor = Color(0xFFF1F3F5)
val AppSectionSurfaceColor = Color(0xFFE5E7EB)
val AppTextColor = Color(0xFF1F2937)
val AppSecondaryTextColor = Color(0xFF6B7280)
val AppOverlayBackgroundColor = Color(0xEFFFFFFF)
val AppCoreHighlightBackgroundColor = Color(0x26F59E0B)
val AppCoreLabelBackgroundColor = Color(0x99FDE68A)
val AppOverlayStrokePreviewColor = Color(0x44222222)
val AppBusyOverlayColor = Color(0x66FFFFFF)

class MainActivity : ComponentActivity() {
    private sealed class ImageImportTarget {
        object NewWall : ImageImportTarget()
        data class ReplaceWallImage(val wallId: String) : ImageImportTarget()
    }

    // 外部カメラや画像選択の読込中だけ全画面インジケータを出します。
    private val isCaptureProcessing = mutableStateOf(false)

    // 標準カメラへ渡した一時保存先 Uri を保持します。
    private var pendingCaptureUri: Uri? = null
    private var pendingImageImportTarget: ImageImportTarget = ImageImportTarget.NewWall

    // 画面状態を管理する ViewModel です。
    private val viewModel: MainViewModel by viewModels()

    // 標準カメラアプリで撮影し、指定 Uri へ保存してもらいます。
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val captureUri = pendingCaptureUri
            pendingCaptureUri = null
            if (!success || captureUri == null) {
                pendingImageImportTarget = ImageImportTarget.NewWall
                isCaptureProcessing.value = false
                return@registerForActivityResult
            }
            loadCapturedImageFromUri(captureUri)
        }

    // 端末内の画像や写真から 1 枚選んでもらいます。
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                pendingImageImportTarget = ImageImportTarget.NewWall
                return@registerForActivityResult
            }
            isCaptureProcessing.value = true
            loadCapturedImageFromUri(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ステータスバー領域の色付けと余白制御は Compose 側で統一します。
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                SideEffect {
                    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    val savedChallengeMessage = getString(R.string.message_saved_challenge)
                    LaunchedEffect(uiState.message) {
                        if (uiState.message == savedChallengeMessage) {
                            Toast.makeText(
                                this@MainActivity,
                                savedChallengeMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearMessage()
                        }
                    }

                    val isChallengeGenerationBusy =
                        uiState.currentScreen == AppScreen.CHALLENGE_CREATOR && uiState.isBusy
                    val isAutoHoldExtractionBusy =
                        uiState.currentScreen == AppScreen.AUTO_HOLD_EXTRACTION && uiState.isBusy
                    val isWallImageReplacementBusy =
                        uiState.currentScreen == AppScreen.WALL_IMAGE_REPLACEMENT && uiState.isBusy
                    val isBusyBackBlocked =
                        isChallengeGenerationBusy ||
                            isAutoHoldExtractionBusy ||
                            isWallImageReplacementBusy

                    // 課題作成中はシステムバックを無効化します。
                    BackHandler(enabled = isBusyBackBlocked) {
                        Unit
                    }

                    // 一覧以外ではシステムバックを ViewModel 側へ流します。
                    BackHandler(
                        enabled = uiState.currentScreen != AppScreen.LIST && !isBusyBackBlocked
                    ) {
                        viewModel.onBackPressed()
                    }

                    SprayProblemGeneratorApp(
                        state = uiState,
                        isExternalBusy = isCaptureProcessing.value,
                        onOpenSavedWallForReachCalibration = viewModel::openSavedWallForReachCalibration,
                        onOpenSavedWallForHoldEditor = viewModel::openSavedWallForHoldEditor,
                        onOpenSavedWallForHoldAttributeEditor = viewModel::openSavedWallForHoldAttributeEditor,
                        onOpenSavedWallForHoldScoring = viewModel::openSavedWallForHoldScoring,
                        onOpenSavedWallForChallenge = viewModel::openSavedWallForChallenge,
                        onOpenSavedWallChallenges = viewModel::openSavedWallChallenges,
                        onOpenSavedChallenge = viewModel::openSavedChallenge,
                        onSaveSavedChallengeImage = ::saveSavedChallengeImageToGallery,
                        onDeleteSavedChallenge = viewModel::deleteSavedChallenge,
                        onDeleteSavedWall = viewModel::deleteSavedWall,
                        onTakeReplacementPhoto = ::launchSystemCameraForWallImageReplacement,
                        onPickReplacementPhoto = ::launchPhotoPickerForWallImageReplacement,
                        onOpenDisplayColorSettings = viewModel::openDisplayColorSettings,
                        onOpenLicenses = viewModel::openLicenses,
                        onUpdateDisplayColor = viewModel::updateDisplayColor,
                        onUpdateDisplayStrokeWidth = viewModel::updateDisplayStrokeWidth,
                        onResetDisplayColorSettings = viewModel::resetDisplayColorSettings,
                        onTakePhoto = ::launchSystemCamera,
                        onPickPhoto = ::launchPhotoPicker,
                        onApplyCapturedImageCropManual = viewModel::applyCapturedImageCropAndOpenManual,
                        onApplyCapturedImageCropAuto = viewModel::applyCapturedImageCropAndOpenAuto,
                        onOpenManualHoldRegistrationAfterCapture = viewModel::openManualHoldRegistrationAfterCapture,
                        onOpenAutoHoldExtractionAfterCapture = viewModel::openAutoHoldExtractionAfterCapture,
                        onBackToCameraFromHoldRegistrationMethod = viewModel::requestBackToList,
                        onBackToHoldRegistrationMethodSelection = viewModel::backToHoldRegistrationMethodSelection,
                        onAutoExtractedHoldTapped = viewModel::onAutoExtractedHoldTapped,
                        onStartAutoExtractionWallSampling = viewModel::startAutoExtractionWallSampling,
                        onStopAutoExtractionWallSampling = viewModel::stopAutoExtractionWallSampling,
                        onAutoExtractionWallSamplePointSelected = viewModel::onAutoExtractionWallSamplePointSelected,
                        onAutoExtractionTuningChange = viewModel::onAutoExtractionTuningChanged,
                        onApplyAutoExtractedHoldsAndContinue = viewModel::applyAutoExtractedHoldsAndContinue,
                        onBackToList = viewModel::requestBackToList,
                        onReturnToList = viewModel::onBackPressed,
                        onSaveWall = viewModel::saveWallAndReturnToList,
                        onOpenReachCalibrationScreen = viewModel::openReachCalibrationScreen,
                        onSaveEditedHoldsInHoldEditor = viewModel::saveEditedHoldsInHoldEditor,
                        onBackFromHoldAttributeEditor = viewModel::returnToHoldEditorFromAttributeEditor,
                        onOpenHoldScoring = viewModel::openHoldScoring,
                        onBackFromHoldScoring = viewModel::returnToHoldAttributeEditorFromScoring,
                        onDifficultyScoreSelected = viewModel::setCurrentHoldDifficultyScore,
                        onSaveWallAndOpenChallenge = viewModel::saveWallAndOpenChallenge,
                        onHoldTapAreaSizeChange = viewModel::onHoldTapAreaSizeChanged,
                        onOpenHoldEditOperation = viewModel::openHoldEditOperation,
                        onDeleteSelectedHold = viewModel::removeSelectedHold,
                        onEditorHoldTapped = viewModel::onEditorHoldTapped,
                        onAssignHoldAsStartCandidate = viewModel::assignHoldAsStartCandidate,
                        onAssignHoldAsGoalCandidate = viewModel::assignHoldAsGoalCandidate,
                        onClearHoldAttributes = viewModel::clearHoldAttributes,
                        onChallengeHoldTapped = viewModel::onChallengeHoldTapped,
                        onApplyEditedHoldsAndReturn = viewModel::applyEditedHoldsAndReturnToHoldEditor,
                        onBackFromHoldEditOperation = viewModel::onBackPressed,
                        onSaveWallImageReplacement = viewModel::saveWallImageReplacement,
                        onContinueToHoldEditorFromReachCalibration = viewModel::continueToHoldEditorFromReachCalibration,
                        onBackFromReachCalibration = viewModel::backFromReachCalibration,
                        onStartReachCalibrationSelection = viewModel::startReachCalibrationSelection,
                        onReachCalibrationLengthInputChange = viewModel::onReachCalibrationLengthInputChanged,
                        onClearReachCalibration = viewModel::clearReachCalibration,
                        onReachCalibrationPointSelected = viewModel::onReachCalibrationPointSelected,
                        onSelectManualStartGoalChallengeMethod = viewModel::selectManualStartGoalChallengeMethod,
                        onSelectRandomStartGoalChallengeMethod = viewModel::selectRandomStartGoalChallengeMethod,
                        onOpenChallengeCommonSettings = viewModel::openChallengeCommonSettings,
                        onOpenChallengeGeneration = viewModel::openChallengeGeneration,
                        onOpenChallengeTuning = viewModel::openChallengeTuning,
                        onStartGoalSelection = viewModel::startChallengeStartGoalSelection,
                        onDrawWithRandomStartGoal = viewModel::drawRandomChallengeWithRandomStartGoal,
                        onStartDrawTargetSelection = viewModel::startDrawTargetSelection,
                        onDrawTargetSelectionCompleted = viewModel::applyDrawTargetSelection,
                        onDrawTargetSelectionProcessingChange = viewModel::setDrawTargetSelectionProcessing,
                        onDrawClick = viewModel::drawRandomChallengeHolds,
                        onRerunCurrentChallengeGeneration = viewModel::rerunCurrentChallengeGeneration,
                        onSaveCurrentChallenge = viewModel::saveCurrentChallenge,
                        onDrawCountChange = viewModel::onDrawCountChanged,
                        onChallengeDifficultyRangeChange = viewModel::onChallengeDifficultyRangeChanged,
                        onRouteVariationChange = viewModel::onRouteVariationChanged,
                        onExcludePreviouslyGeneratedHoldsChange = viewModel::onExcludePreviouslyGeneratedHoldsChanged,
                        onRandomStartGoalPairLimitChange = viewModel::onRandomStartGoalPairLimitChanged,
                        onRouteGenerationAttemptLimitChange = viewModel::onRouteGenerationAttemptLimitChanged,
                        onClearChallenge = viewModel::clearChallengeSelection,
                        onDismissDiscardDialog = viewModel::dismissDiscardDialog,
                        onDiscardChanges = viewModel::discardEditorAndReturnToList
                    )
                }
            }
        }
    }

    // 標準カメラアプリを起動します。
    private fun launchSystemCamera() {
        launchSystemCameraForTarget(ImageImportTarget.NewWall)
    }

    private fun launchSystemCameraForWallImageReplacement(wallId: String) {
        launchSystemCameraForTarget(ImageImportTarget.ReplaceWallImage(wallId))
    }

    private fun launchSystemCameraForTarget(target: ImageImportTarget) {
        val captureUri = createTemporaryCaptureUri() ?: run {
            pendingImageImportTarget = ImageImportTarget.NewWall
            return
        }
        pendingCaptureUri = captureUri
        pendingImageImportTarget = target
        isCaptureProcessing.value = true
        takePictureLauncher.launch(captureUri)
    }

    // 端末内の画像選択 UI を開きます。
    private fun launchPhotoPicker() {
        launchPhotoPickerForTarget(ImageImportTarget.NewWall)
    }

    private fun launchPhotoPickerForWallImageReplacement(wallId: String) {
        launchPhotoPickerForTarget(ImageImportTarget.ReplaceWallImage(wallId))
    }

    private fun launchPhotoPickerForTarget(target: ImageImportTarget) {
        pendingImageImportTarget = target
        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // 取得した画像 Uri から Bitmap を読み込み、既存の撮影後フローへつなぎます。
    private fun loadCapturedImageFromUri(uri: Uri) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadCorrectedBitmap(contentResolver = contentResolver, uri = uri)
            }
            val importTarget = pendingImageImportTarget
            pendingImageImportTarget = ImageImportTarget.NewWall

            if (bitmap == null) {
                isCaptureProcessing.value = false
                return@launch
            }

            val capturedOrientation = if (bitmap.width > bitmap.height) {
                CapturedOrientation.LANDSCAPE
            } else {
                CapturedOrientation.PORTRAIT
            }

            isCaptureProcessing.value = false
            when (importTarget) {
                ImageImportTarget.NewWall -> {
                    viewModel.onPhotoCaptured(
                        bitmap = bitmap,
                        capturedOrientation = capturedOrientation,
                        capturedRotationDegrees = 0
                    )
                }

                is ImageImportTarget.ReplaceWallImage -> {
                    viewModel.openWallImageReplacement(
                        wallId = importTarget.wallId,
                        replacementBitmap = bitmap
                    )
                }
            }
        }
    }

    // 標準カメラアプリに渡す一時保存先 Uri を作ります。
    private fun createTemporaryCaptureUri(): Uri? {
        return try {
            val captureDirectory = File(cacheDir, "captured").apply { mkdirs() }
            val captureFile = File.createTempFile("capture_", ".jpg", captureDirectory)
            FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                captureFile
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveSavedChallengeImageToGallery(bitmap: android.graphics.Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(
                this,
                getString(R.string.message_save_challenge_image_failed),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { saveBitmapToGallery(bitmap) }.isSuccess
            }
            Toast.makeText(
                this@MainActivity,
                getString(
                    if (saved) {
                        R.string.message_saved_challenge_image
                    } else {
                        R.string.message_save_challenge_image_failed
                    }
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Throws(IOException::class)
    private fun saveBitmapToGallery(bitmap: android.graphics.Bitmap) {
        val fileName = "hold-detector-challenge-${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/SprayProblemGenerator"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val itemUri = contentResolver.insert(collection, contentValues)
            ?: throw IOException("Could not create MediaStore entry")

        try {
            contentResolver.openOutputStream(itemUri)?.use { outputStream ->
                if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    throw IOException("Could not compress bitmap")
                }
            } ?: throw IOException("Could not open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completedValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(itemUri, completedValues, null, null)
            }
        } catch (throwable: Throwable) {
            contentResolver.delete(itemUri, null, null)
            throw throwable
        }
    }
}
