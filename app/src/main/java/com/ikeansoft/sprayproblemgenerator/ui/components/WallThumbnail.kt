package com.ikeansoft.sprayproblemgenerator.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ikeansoft.sprayproblemgenerator.R
import com.ikeansoft.sprayproblemgenerator.ui.AppSecondaryTextColor
import com.ikeansoft.sprayproblemgenerator.ui.AppSubtleSurfaceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun WallThumbnail(
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

private fun decodeThumbnailBitmap(
    imageFilePath: String,
    maxWidth: Int,
    maxHeight: Int
): Bitmap? {
    val file = File(imageFilePath)
    if (!file.exists()) return null

    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

    if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
        return null
    }

    var sampleSize = 1
    val halfWidth = boundsOptions.outWidth / 2
    val halfHeight = boundsOptions.outHeight / 2

    while (halfWidth / sampleSize >= maxWidth && halfHeight / sampleSize >= maxHeight) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize.coerceAtLeast(1)
    }

    return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
}
