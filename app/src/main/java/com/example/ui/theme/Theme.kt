package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = VvtGold,
    onPrimary = VvtNavyDark,
    primaryContainer = VvtNavyLight,
    onPrimaryContainer = Color.White,
    secondary = VvtOrange,
    onSecondary = Color.White,
    background = VvtBgDark,
    onBackground = VvtTextPrimaryDark,
    surface = VvtSurfaceDark,
    onSurface = VvtTextPrimaryDark,
    error = VvtError,
    onError = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VvtNavy,
    onPrimary = Color.White,
    primaryContainer = VvtNavyLight,
    onPrimaryContainer = Color.White,
    secondary = VvtOrange,
    onSecondary = Color.White,
    background = VvtBgLight,
    onBackground = VvtTextPrimaryLight,
    surface = VvtSurfaceLight,
    onSurface = VvtTextPrimaryLight,
    error = VvtError,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
