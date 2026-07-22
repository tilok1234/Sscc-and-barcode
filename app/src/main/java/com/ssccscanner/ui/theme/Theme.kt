package com.ssccscanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val ColorScheme = darkColorScheme(
    primary = Tokens.Accent,
    onPrimary = Tokens.OnAccent,
    background = Tokens.Surface,
    onBackground = Tokens.TextPrimary,
    surface = Tokens.Panel,
    onSurface = Tokens.TextPrimary,
    error = Tokens.Danger,
    onError = Tokens.TextBright,
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontFamily = PlexSans, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = PlexSans, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = PlexSans, fontSize = 11.sp),
    titleLarge = TextStyle(fontFamily = PlexSans, fontSize = 17.sp),
    titleMedium = TextStyle(fontFamily = PlexSans, fontSize = 15.sp),
    labelLarge = TextStyle(fontFamily = PlexSans, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = PlexSans, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = PlexSans, fontSize = 10.5.sp),
)

@Composable
fun SsccScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = AppTypography,
        content = content,
    )
}
