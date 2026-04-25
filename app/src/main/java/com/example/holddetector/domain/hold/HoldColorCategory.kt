package com.example.holddetector.domain.hold

enum class HoldColorCategory(
    val hueCenter: Float?,
    val referenceValue: Float,
    val isNeutral: Boolean
) {
    WHITE(hueCenter = null, referenceValue = 0.94f, isNeutral = true),
    BLACK(hueCenter = null, referenceValue = 0.12f, isNeutral = true),
    ORANGE(hueCenter = 28f, referenceValue = 0.84f, isNeutral = false),
    RED(hueCenter = 0f, referenceValue = 0.72f, isNeutral = false),
    PURPLE(hueCenter = 286f, referenceValue = 0.68f, isNeutral = false),
    BLUE(hueCenter = 220f, referenceValue = 0.68f, isNeutral = false),
    CYAN(hueCenter = 190f, referenceValue = 0.82f, isNeutral = false),
    YELLOW(hueCenter = 56f, referenceValue = 0.90f, isNeutral = false),
    GREEN(hueCenter = 124f, referenceValue = 0.68f, isNeutral = false),
    LIME(hueCenter = 88f, referenceValue = 0.82f, isNeutral = false)
}

fun defaultSelectedAutoExtractionColors(): Set<HoldColorCategory> {
    return HoldColorCategory.values().toSet()
}
