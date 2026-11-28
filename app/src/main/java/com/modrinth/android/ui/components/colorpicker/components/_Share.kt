/*
 * Ported from Zalith Launcher 2 (GPL-3.0 or later) - modified derivative work.
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors.
 *
 * Original project: https://github.com/ZalithLauncher/ZalithLauncher2
 * Per GPLv3 section 4, the original copyright notice is preserved here.
 * See NOTICE in the repository root for source-file mapping and terms.
 */


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