package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object ThemeColors {
    var isDark by mutableStateOf(false)

    val emeraldDeepDark: Color
        get() = if (isDark) Color(0xFF12180F) else Color(0xFFFAF5EA)

    val emeraldContainer: Color
        get() = if (isDark) Color(0xFF1F261C) else Color(0xFFFFFDFC)

    val islamicEmerald: Color
        get() = if (isDark) Color(0xFF8BA67A) else Color(0xFF3B4E2F)

    val islamicGold: Color
        get() = if (isDark) Color(0xFFABBC9C) else Color(0xFF4A5D3F)

    val sandGold: Color
        get() = if (isDark) Color(0xFFC5D1BC) else Color(0xFF6E8062)

    val sandText: Color
        get() = if (isDark) Color(0xFFFAF5EA) else Color(0xFF1B2616)

    val slateGray: Color
        get() = if (isDark) Color(0xFF8FA189) else Color(0xFF596A53)
}

val EmeraldDeepDark: Color
    get() = ThemeColors.emeraldDeepDark

val EmeraldContainer: Color
    get() = ThemeColors.emeraldContainer

val IslamicEmerald: Color
    get() = ThemeColors.islamicEmerald

val IslamicGold: Color
    get() = ThemeColors.islamicGold

val SandGold: Color
    get() = ThemeColors.sandGold

val SandText: Color
    get() = ThemeColors.sandText

val SlateGray: Color
    get() = ThemeColors.slateGray

val LightSandBg = Color(0xFFFAF5EA)          // Cream background (light mode)
val LightEmeraldContainer = Color(0xFFFFFDFC)// Pearl/ivory surface container
val LightPrimary = Color(0xFF3B4E2F)         // Olive green primary
val LightText = Color(0xFF1B2616)            // Deep olive-charcoal text


