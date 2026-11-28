// Ported from ZalithLauncher2 (GPL-3.0), see NOTICE

package com.modrinth.android.ui.components.colorpicker.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 公开可用的透明棋格背景
 */
@Composable
fun TransparentChecker(
    modifier: Modifier = Modifier,
    gridSize: Float = 20f,
) {
    Canvas(modifier) {
        transparentCheckerBackground(
            width = size.width,
            height = size.height,
            gridSize = gridSize
        )
    }
}