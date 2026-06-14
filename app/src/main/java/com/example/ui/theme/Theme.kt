package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = IslamicEmerald,
    secondary = SandGold,
    tertiary = IslamicGold,
    background = EmeraldDeepDark,
    surface = EmeraldContainer,
    onPrimary = EmeraldDeepDark,
    onSecondary = EmeraldDeepDark,
    onBackground = SandText,
    onSurface = SandText
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    secondary = IslamicGold,
    tertiary = IslamicEmerald,
    background = LightSandBg,
    surface = LightEmeraldContainer,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = LightText,
    onBackground = LightText,
    onSurface = LightText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  ThemeColors.isDark = darkTheme
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
