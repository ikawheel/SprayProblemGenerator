package com.example.holddetector.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.holddetector.model.CapturedOrientation
import com.example.holddetector.model.DEFAULT_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.DEFAULT_REACH_REFERENCE_LENGTH_CM
import com.example.holddetector.model.Hold
import com.example.holddetector.model.HoldPoint
import com.example.holddetector.model.MAX_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.MIN_HOLD_DIFFICULTY_SCORE
import com.example.holddetector.model.ReachCalibrationReference
import com.example.holddetector.model.SavedWallDetail
import com.example.holddetector.model.SavedWallSummary
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class WallStorageRepository(context: Context) {

    private val rootDir = File(context.filesDir, "saved_routes")
    private val imageDir = File(rootDir, "images")
    private val metadataDir = File(rootDir, "metadata")

    init {
        imageDir.mkdirs()
        metadataDir.mkdirs()
    }

    fun loadAllSummaries(): List<SavedWallSummary> {
        val metadataFiles = metadataDir.listFiles { file -> file.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        return metadataFiles.mapNotNull { file ->
            runCatching { parseSummary(file.readText()) }.getOrNull()
        }.sortedByDescending { it.updatedAt }
    }

    fun loadWall(wallId: String): SavedWallDetail? {
        val metadataFile = metadataFile(wallId)
        if (!metadataFile.exists()) return null

        val json = runCatching { JSONObject(metadataFile.readText()) }.getOrNull() ?: return null
        val imagePath = json.optString(KEY_IMAGE_PATH)
        if (imagePath.isBlank()) return null

        val imageFile = File(imagePath)
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return null

        val holdsJson = json.optJSONArray(KEY_HOLDS) ?: JSONArray()
        val holds = buildList {
            for (i in 0 until holdsJson.length()) {
                val item = holdsJson.optJSONObject(i) ?: continue
                val pointsJson = item.optJSONArray(KEY_POINTS) ?: continue
                val points = buildList {
                    for (j in 0 until pointsJson.length()) {
                        val pointJson = pointsJson.optJSONObject(j) ?: continue
                        add(
                            HoldPoint(
                                x = pointJson.optInt(KEY_X),
                                y = pointJson.optInt(KEY_Y)
                            )
                        )
                    }
                }
                if (points.size >= 3) {
                    add(
                        Hold(
                            points = points,
                            difficultyScore = item.optInt(
                                KEY_DIFFICULTY_SCORE,
                                DEFAULT_HOLD_DIFFICULTY_SCORE
                            ).coerceIn(
                                MIN_HOLD_DIFFICULTY_SCORE,
                                MAX_HOLD_DIFFICULTY_SCORE
                            ),
                            isStartCandidate = item.optBoolean(KEY_IS_START_CANDIDATE, false),
                            isGoalCandidate = item.optBoolean(KEY_IS_GOAL_CANDIDATE, false)
                        )
                    )
                }
            }
        }

        return SavedWallDetail(
            id = json.optString(KEY_ID),
            title = json.optString(KEY_TITLE),
            imageFilePath = imageFile.absolutePath,
            bitmap = bitmap,
            holds = holds,
            reachCalibrationReference = parseReachCalibrationReference(json.optJSONObject(KEY_REACH_CALIBRATION_REFERENCE)),
            capturedOrientation = parseCapturedOrientation(
                rotationDegrees = parseCapturedRotationDegrees(
                    rawRotation = json.optInt(KEY_CAPTURED_ROTATION_DEGREES, -1),
                    rawOrientation = json.optString(KEY_CAPTURED_ORIENTATION),
                    bitmap = bitmap
                ),
                bitmap = bitmap
            ),
            capturedRotationDegrees = parseCapturedRotationDegrees(
                rawRotation = json.optInt(KEY_CAPTURED_ROTATION_DEGREES, -1),
                rawOrientation = json.optString(KEY_CAPTURED_ORIENTATION),
                bitmap = bitmap
            ),
            createdAt = json.optLong(KEY_CREATED_AT),
            updatedAt = json.optLong(KEY_UPDATED_AT)
        )
    }

    fun saveWall(
        wallId: String?,
        title: String,
        bitmap: Bitmap,
        holds: List<Hold>,
        reachCalibrationReference: ReachCalibrationReference?,
        capturedOrientation: CapturedOrientation,
        capturedRotationDegrees: Int
    ): SavedWallSummary {
        val now = System.currentTimeMillis()
        val resolvedId = wallId ?: UUID.randomUUID().toString()
        val existingJson = metadataFile(resolvedId).takeIf { it.exists() }
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }

        val createdAt = existingJson?.optLong(KEY_CREATED_AT)?.takeIf { it > 0L } ?: now
        val safeTitle = title.ifBlank { "壁_$now" }
        val imageFile = existingJson
            ?.optString(KEY_IMAGE_PATH)
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?: File(imageDir, "$resolvedId.jpg")

        saveBitmap(bitmap, imageFile)

        val metadataJson = JSONObject().apply {
            put(KEY_ID, resolvedId)
            put(KEY_TITLE, safeTitle)
            put(KEY_IMAGE_PATH, imageFile.absolutePath)
            put(KEY_CAPTURED_ORIENTATION, capturedOrientation.name)
            put(KEY_CAPTURED_ROTATION_DEGREES, normalizeRotationDegrees(capturedRotationDegrees))
            put(
                KEY_REACH_CALIBRATION_REFERENCE,
                reachCalibrationReference?.toJson()
            )
            put(KEY_HOLDS, JSONArray().apply {
                holds.forEach { hold ->
                    put(
                        JSONObject().apply {
                            put(KEY_DIFFICULTY_SCORE, hold.difficultyScore)
                            put(KEY_IS_START_CANDIDATE, hold.isStartCandidate)
                            put(KEY_IS_GOAL_CANDIDATE, hold.isGoalCandidate)
                            put(KEY_POINTS, JSONArray().apply {
                                hold.points.forEach { point ->
                                    put(
                                        JSONObject().apply {
                                            put(KEY_X, point.x)
                                            put(KEY_Y, point.y)
                                        }
                                    )
                                }
                            })
                        }
                    )
                }
            })
            put(KEY_CREATED_AT, createdAt)
            put(KEY_UPDATED_AT, now)
        }

        metadataFile(resolvedId).writeText(metadataJson.toString(2))

        return SavedWallSummary(
            id = resolvedId,
            title = safeTitle,
            imageFilePath = imageFile.absolutePath,
            holdCount = holds.size,
            createdAt = createdAt,
            updatedAt = now
        )
    }

    fun deleteWall(wallId: String) {
        val metadataFile = metadataFile(wallId)
        val imagePath = if (metadataFile.exists()) {
            runCatching { JSONObject(metadataFile.readText()).optString(KEY_IMAGE_PATH) }.getOrNull()
        } else {
            null
        }

        metadataFile.delete()
        imagePath?.takeIf { it.isNotBlank() }?.let { File(it).delete() }
    }

    private fun metadataFile(wallId: String): File = File(metadataDir, "$wallId.json")

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            output.flush()
        }
    }

    private fun parseSummary(rawJson: String): SavedWallSummary {
        val json = JSONObject(rawJson)
        return SavedWallSummary(
            id = json.optString(KEY_ID),
            title = json.optString(KEY_TITLE),
            imageFilePath = json.optString(KEY_IMAGE_PATH),
            holdCount = json.optJSONArray(KEY_HOLDS)?.length() ?: 0,
            createdAt = json.optLong(KEY_CREATED_AT),
            updatedAt = json.optLong(KEY_UPDATED_AT)
        )
    }

    private fun parseCapturedRotationDegrees(
        rawRotation: Int,
        rawOrientation: String?,
        bitmap: Bitmap
    ): Int {
        val normalizedRotation = normalizeRotationDegrees(rawRotation)
        if (rawRotation >= 0 && normalizedRotation in setOf(0, 90, 180, 270)) {
            return normalizedRotation
        }

        return when (CapturedOrientation.entries.firstOrNull { it.name == rawOrientation }) {
            CapturedOrientation.LANDSCAPE -> 90
            CapturedOrientation.PORTRAIT -> 0
            null -> if (bitmap.width > bitmap.height) 90 else 0
        }
    }

    private fun parseCapturedOrientation(
        rotationDegrees: Int,
        bitmap: Bitmap
    ): CapturedOrientation {
        return if (normalizeRotationDegrees(rotationDegrees) in setOf(90, 270)) {
            CapturedOrientation.LANDSCAPE
        } else if (bitmap.width > bitmap.height) {
            CapturedOrientation.LANDSCAPE
        } else {
            CapturedOrientation.PORTRAIT
        }
    }

    private fun parseReachCalibrationReference(json: JSONObject?): ReachCalibrationReference? {
        val firstPoint = json?.optJSONObject(KEY_FIRST_POINT)?.toHoldPoint() ?: return null
        val secondPoint = json.optJSONObject(KEY_SECOND_POINT)?.toHoldPoint() ?: return null
        return ReachCalibrationReference(
            firstPoint = firstPoint,
            secondPoint = secondPoint,
            referenceLengthCm = json.optInt(
                KEY_REFERENCE_LENGTH_CM,
                DEFAULT_REACH_REFERENCE_LENGTH_CM
            ).coerceAtLeast(1)
        )
    }

    private fun ReachCalibrationReference.toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_FIRST_POINT, firstPoint.toJson())
            put(KEY_SECOND_POINT, secondPoint.toJson())
            put(KEY_REFERENCE_LENGTH_CM, referenceLengthCm)
        }
    }

    private fun HoldPoint.toJson(): JSONObject {
        return JSONObject().apply {
            put(KEY_X, x)
            put(KEY_Y, y)
        }
    }

    private fun JSONObject.toHoldPoint(): HoldPoint {
        return HoldPoint(
            x = optInt(KEY_X),
            y = optInt(KEY_Y)
        )
    }

    private fun normalizeRotationDegrees(rotationDegrees: Int): Int {
        val normalized = rotationDegrees % 360
        return if (normalized < 0) normalized + 360 else normalized
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_IMAGE_PATH = "imageFilePath"
        private const val KEY_CAPTURED_ORIENTATION = "capturedOrientation"
        private const val KEY_CAPTURED_ROTATION_DEGREES = "capturedRotationDegrees"
        private const val KEY_REACH_CALIBRATION_REFERENCE = "reachCalibrationReference"
        private const val KEY_HOLDS = "holds"
        private const val KEY_DIFFICULTY_SCORE = "difficultyScore"
        private const val KEY_IS_START_CANDIDATE = "isStartCandidate"
        private const val KEY_IS_GOAL_CANDIDATE = "isGoalCandidate"
        private const val KEY_POINTS = "points"
        private const val KEY_FIRST_POINT = "firstPoint"
        private const val KEY_SECOND_POINT = "secondPoint"
        private const val KEY_REFERENCE_LENGTH_CM = "referenceLengthCm"
        private const val KEY_CREATED_AT = "createdAt"
        private const val KEY_UPDATED_AT = "updatedAt"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
    }
}
