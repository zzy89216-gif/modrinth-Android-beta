package com.modrinth.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = ModboxPrimary,
    onPrimary = ModboxOnPrimary,
    primaryContainer = ModboxPrimaryContainer,
    background = ModboxBackground,
    surface = ModboxSurface,
    surfaceVariant = ModboxSurfaceVariant,
    onBackground = ModboxOnBackground,
    onSurface = ModboxOnSurface,
    onSurfaceVariant = ModboxOnSurfaceVariant,
    outline = ModboxOutline,
    error = ModboxError
)

private val LightColors = lightColorScheme(
    primary = ModboxPrimary,
    onPrimary = ModboxOnPrimary,
    primaryContainer = ModboxPrimaryContainer,
    error = ModboxError
)

/**
 * ModBox 主题：默认深色（品牌底色 #0F1115），
 * [forceDark] = null 时跟随系统，true/false 强制。
 */
@Composable
fun ModBoxTheme(
    forceDark: Boolean? = null,
    content: @Composable () -> Unit
) {
    val dark = forceDark ?: isSystemInDarkTheme()
    val scheme = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = scheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
