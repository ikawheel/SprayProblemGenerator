package com.example.holddetector.ui.screens

internal fun wallImageDisplayAspectRatio(
    imageWidth: Int,
    imageHeight: Int
): Float {
    if (imageWidth <= 0 || imageHeight <= 0) return 1f

    val baseAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
    return if (imageWidth > imageHeight) {
        baseAspectRatio / 1.25f
    } else {
        baseAspectRatio
    }
}
