/*
 * Ported from Zalith Launcher 2 (GPL-3.0 or later) - modified derivative work.
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors.
 *
 * Original project: https://github.com/ZalithLauncher/ZalithLauncher2
 * Per GPLv3 section 4, the original copyright notice is preserved here.
 * See NOTICE in the repository root for source-file mapping and terms.
 */

package com.modrinth.android.ui.components.colorpicker

import androidx.compose.ui.graphics.Color

/**
 * 在颜色渐变中，根据当前X坐标获取对应的颜色
 */
internal fun List<Color>.getGradientColorAtPosition(x: Float, widthPx: Float): Color {
    if (this.isEmpty()) return Color.Black
    if (x <= 0f) return this.first()
    if (x >= widthPx) return this.last()

    val fraction = x / widthPx
    val totalSegments = this.size - 1
    val segmentFraction = fraction * totalSegments
    val segmentIndex = segmentFraction.toInt()
    val t = segmentFraction - segmentIndex

    val startColor = this[segmentIndex]
    val endColor = this[segmentIndex + 1]

    return Color(
        red = startColor.red + (endColor.red - startColor.red) * t,
        green = startColor.green + (endColor.green - startColor.green) * t,
        blue = startColor.blue + (endColor.blue - startColor.blue) * t,
        alpha = startColor.alpha + (endColor.alpha - startColor.alpha) * t
    )
}
