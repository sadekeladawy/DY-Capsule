package com.example

import android.os.Build
import android.view.DisplayCutout
import android.view.WindowInsets
import androidx.annotation.RequiresApi

data class CutoutRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
}

object CutoutHelper {
    @RequiresApi(Build.VERSION_CODES.P)
    fun extractTopCutout(insets: WindowInsets): CutoutRect? {
        val displayCutout: DisplayCutout = insets.displayCutout ?: return null
        val rects = displayCutout.boundingRects
        if (rects.isEmpty()) return null

        // Find the top-most cutout
        val topRect = rects.minByOrNull { it.top } ?: return null
        return CutoutRect(topRect.left, topRect.top, topRect.right, topRect.bottom)
    }
}
