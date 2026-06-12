package com.ikeansoft.sprayproblemgenerator.ui

import android.graphics.Bitmap
import com.ikeansoft.sprayproblemgenerator.domain.hold.AutoExtractionTuning
import com.ikeansoft.sprayproblemgenerator.domain.challenge.RouteGenerationTuning
import com.ikeansoft.sprayproblemgenerator.model.CapturedOrientation
import com.ikeansoft.sprayproblemgenerator.model.DEFAULT_HOLD_DIFFICULTY_SCORE
import com.ikeansoft.sprayproblemgenerator.model.DEFAULT_REACH_REFERENCE_LENGTH_CM
import com.ikeansoft.sprayproblemgenerator.model.Hold
import com.ikeansoft.sprayproblemgenerator.model.HoldPoint
import com.ikeansoft.sprayproblemgenerator.model.MAX_HOLD_DIFFICULTY_SCORE
import com.ikeansoft.sprayproblemgenerator.model.MIN_HOLD_DIFFICULTY_SCORE
import com.ikeansoft.sprayproblemgenerator.model.ReachCalibrationReference
import com.ikeansoft.sprayproblemgenerator.model.SavedChallengeSummary
import com.ikeansoft.sprayproblemgenerator.model.SavedWallSummary

enum class AppScreen {
    LIST,
    DISPLAY_COLOR_SETTINGS,
    LICENSES,
    SAVED_CHALLENGE_LIST,
    SAVED_CHALLENGE_DETAIL,
    CAMERA,
    IMAGE_CROP,
    HOLD_REGISTRATION_METHOD,
    AUTO_HOLD_EXTRACTION,
    REACH_CALIBRATION,
    HOLD_EDITOR,
    HOLD_EDIT_OPERATION,
    HOLD_ATTRIBUTE_EDITOR,
    HOLD_SCORING,
    WALL_IMAGE_REPLACEMENT,
    CHALLENGE_CREATOR
}

enum class RouteSelectionMode {
    NONE,
    SELECTING_START,
    SELECTING_GOAL
}

enum class HoldTapAreaSize {
    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

enum class HoldEditorTool {
    ADD,
    EXTEND,
    ERASE,
    DELETE
}

enum class ChallengeGenerationMethod {
    MANUAL_START_GOAL,
    RANDOM_START_GOAL
}

enum class ChallengeFlowStep {
    METHOD_SELECT,
    COMMON_SETTINGS,
    GENERATION,
    RESULT,
    TUNING
}

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.LIST,
    val screenBackStack: List<AppScreen> = emptyList(),
    val savedWalls: List<SavedWallSummary> = emptyList(),
    val savedChallenges: List<SavedChallengeSummary> = emptyList(),
    val isBusy: Boolean = false,
    val currentWallId: String? = null,
    val capturedBitmap: Bitmap? = null,
    val replacementBitmap: Bitmap? = null,
    val capturedOrientation: CapturedOrientation = CapturedOrientation.PORTRAIT,
    val capturedRotationDegrees: Int = 0,
    val holds: List<Hold> = emptyList(),
    val autoExtractedHolds: List<Hold> = emptyList(),
    val autoExtractionTuning: AutoExtractionTuning = AutoExtractionTuning(),
    val autoExtractionWallSamplePoints: List<HoldPoint> = emptyList(),
    val isAutoExtractionWallSamplingMode: Boolean = false,
    val reachCalibrationReference: ReachCalibrationReference? = null,
    val reachCalibrationLengthInput: String = DEFAULT_REACH_REFERENCE_LENGTH_CM.toString(),
    val pendingReachCalibrationPoint: HoldPoint? = null,
    val isReachCalibrationSelectionMode: Boolean = false,
    val reachCalibrationReturnToHoldEditor: Boolean = false,
    val reachCalibrationReturnToAutoExtraction: Boolean = false,
    val selectedHoldIndex: Int? = null,
    val challengeHoldIndices: Set<Int> = emptySet(),
    val challengeOrderedHoldIndices: List<Int> = emptyList(),
    val lastGeneratedIntermediateHoldIndices: Set<Int> = emptySet(),
    val challengeGenerationMethod: ChallengeGenerationMethod? = null,
    val challengeFlowStep: ChallengeFlowStep = ChallengeFlowStep.METHOD_SELECT,
    val challengeFlowBackStack: List<ChallengeFlowStep> = emptyList(),
    val drawTargetHoldIndices: Set<Int> = emptySet(),
    val hasDrawTargetSelection: Boolean = false,
    val startHoldIndex: Int? = null,
    val goalHoldIndex: Int? = null,
    val routeSelectionMode: RouteSelectionMode = RouteSelectionMode.NONE,
    val isDrawTargetSelectionMode: Boolean = false,
    val drawCountInput: String = "10",
    val holdTapAreaSize: HoldTapAreaSize = HoldTapAreaSize.MEDIUM,
    val holdEditorTool: HoldEditorTool = HoldEditorTool.ADD,
    val displayColorSettings: DisplayColorSettings = DisplayColorSettings(),
    val challengeDifficultyScoreMin: Int = MIN_HOLD_DIFFICULTY_SCORE,
    val challengeDifficultyScoreMax: Int = DEFAULT_HOLD_DIFFICULTY_SCORE.coerceAtMost(MAX_HOLD_DIFFICULTY_SCORE),
    val routeTuning: RouteGenerationTuning = RouteGenerationTuning(),
    val isHoldEditorDirty: Boolean = false,
    val holdScoringPosition: Int = 0,
    val showDiscardDialog: Boolean = false,
    val discardReturnToList: Boolean = false,
    val message: String? = null
)
