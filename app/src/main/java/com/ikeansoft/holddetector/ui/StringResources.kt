package com.ikeansoft.holddetector.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun stringResourceByName(name: String, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val resourceId = context.resources.getIdentifier(name, "string", context.packageName)
    if (resourceId == 0) return name
    return context.getString(resourceId, *formatArgs)
}
