package com.example.holddetector.ui.canvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

internal fun loadCorrectedBitmap(file: File): Bitmap? {
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

internal fun orientBitmapForCaptureRotation(
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
