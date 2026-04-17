package com.example.holddetector.ui.selectors

import com.example.holddetector.model.SavedWallSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun findWallPendingDeletion(
    savedWalls: List<SavedWallSummary>,
    deletingWallId: String
): SavedWallSummary? {
    return savedWalls.firstOrNull { it.id == deletingWallId }
}

internal fun formatWallTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
}
