package com.ikeansoft.holddetector.ui.canvas

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File

// ファイルから画像を読み込み、Exif の回転情報を反映した Bitmap を返します。
internal fun loadCorrectedBitmap(file: File): Bitmap? {
    // まずは通常の Bitmap として読み込みます。
    val rawBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

    // 保存ファイルに付いている Exif 回転情報を取得します。
    val exif = ExifInterface(file.absolutePath)

    // 必要なら回転を反映して返します。
    return applyExifOrientation(
        bitmap = rawBitmap,
        orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    )
}

// ContentResolver から画像を読み込み、Exif の回転情報を反映した Bitmap を返します。
internal fun loadCorrectedBitmap(
    contentResolver: ContentResolver,
    uri: Uri
): Bitmap? {
    // 画像本体は別ストリームで Bitmap として読み込みます。
    val rawBitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    } ?: return null

    // Exif は InputStream から読むため、もう一度ストリームを開きます。
    val orientation = contentResolver.openInputStream(uri)?.use { inputStream ->
        ExifInterface(inputStream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    // 必要なら回転を反映して返します。
    return applyExifOrientation(bitmap = rawBitmap, orientation = orientation)
}

// Exif の向き情報を Bitmap へ反映します。
private fun applyExifOrientation(
    bitmap: Bitmap,
    orientation: Int
): Bitmap {
    // 回転が不要なら元の Bitmap をそのまま返します。
    val rotationDegrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }

    // 回転が必要な場合だけ新しい Bitmap を作ります。
    return Bitmap.createBitmap(
        bitmap,
        0,
        0,
        bitmap.width,
        bitmap.height,
        Matrix().apply { postRotate(rotationDegrees) },
        true
    )
}

// 明示的な回転角を後から反映したいときの共通ヘルパーです。
internal fun orientBitmapForCaptureRotation(
    bitmap: Bitmap,
    rotationDegrees: Int
): Bitmap {
    // 負数や 360 超えを正規化します。
    val normalizedRotation = ((rotationDegrees % 360) + 360) % 360

    // 回転が不要ならそのまま返します。
    if (normalizedRotation == 0) return bitmap

    // 指定角度だけ回転させた Bitmap を返します。
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
