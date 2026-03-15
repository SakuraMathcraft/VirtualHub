package com.virtual.tensorhub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 🎨 Apple Style Colors
val AppleBlue = Color(0xFF007AFF)
val AppleGray = Color(0xFF8E8E93)
val AppleLightGray = Color(0xFFE5E5EA) // System Gray 6
val AppleBackgroundLight = Color(0xFFF2F2F7) // Grouped Background
val AppleBackgroundDark = Color(0xFF000000)
val AppleSurfaceLight = Color(0xFFFFFFFF)
val AppleSurfaceDark = Color(0xFF1C1C1E) // System Gray 6 Dark

// 🌞 亮色模式配色
private val LightColors = lightColorScheme(
    primary = AppleBlue,
    secondary = AppleBlue,
    background = AppleBackgroundLight,
    surface = AppleSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// 🌙 暗色模式配色
private val DarkColors = darkColorScheme(
    primary = AppleBlue,
    secondary = AppleBlue,
    background = AppleBackgroundDark,
    surface = AppleSurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val AppleTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        letterSpacing = (-0.4).sp
    )
)

/**
 * TensorHub 全局主题
 * @param darkTheme 是否启用暗色模式（可手动控制）
 * @param content 内容
 */
@Composable
fun TensorHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // 默认跟随系统
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppleTypography,
        content = content
    )
}
