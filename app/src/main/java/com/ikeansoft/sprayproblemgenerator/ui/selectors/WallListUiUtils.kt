package com.ikeansoft.sprayproblemgenerator.ui.selectors

import com.ikeansoft.sprayproblemgenerator.model.SavedWallSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun findWallPendingDeletion(
    savedWalls: List<SavedWallSummary>,
    deletingWallId: String
): SavedWallSummary? {
    return savedWalls.firstOrNull { it.id == deletingWallId }
}

internal fun formatWallTimestamp(
    timestamp: Long,
    timestampPattern: String,
    unknownValue: String
): String {
    if (timestamp <= 0L) return unknownValue
    return SimpleDateFormat(timestampPattern, Locale.JAPAN).format(Date(timestamp))
}
