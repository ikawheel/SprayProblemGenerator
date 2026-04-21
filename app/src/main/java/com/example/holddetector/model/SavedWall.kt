package com.example.holddetector.model

import android.graphics.Bitmap
enum class CapturedOrientation {
    PORTRAIT,
    LANDSCAPE
}

data class SavedWallSummary(
    val id: String,
    val title: String,
    val imageFilePath: String,
    val holdCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

const val DEFAULT_REACH_REFERENCE_LENGTH_CM = 160

data class ReachCalibrationReference(
    val firstPoint: HoldPoint,
    val secondPoint: HoldPoint,
    val referenceLengthCm: Int = DEFAULT_REACH_REFERENCE_LENGTH_CM
)

data class SavedWallDetail(
    val id: String,
    val title: String,
    val imageFilePath: String,
    val bitmap: Bitmap,
    val holds: List<Hold>,
    val reachCalibrationReference: ReachCalibrationReference?,
    val capturedOrientation: CapturedOrientation,
    val capturedRotationDegrees: Int,
    val createdAt: Long,
    val updatedAt: Long
)
