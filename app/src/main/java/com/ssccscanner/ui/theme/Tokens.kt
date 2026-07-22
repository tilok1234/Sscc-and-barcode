package com.ssccscanner.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens from the design handoff (docs/design_handoff/README.md).
 */
object Tokens {
    // Surfaces
    val Void = Color(0xFF05070A)            // page/void background
    val Surface = Color(0xFF0B0D0C)         // app surface
    val CameraSurface = Color(0xFF0E1113)   // camera-idle surface
    val Panel = Color(0xFF101314)           // header / sheet / nav surface
    val ToastBg = Color(0xF214171A)         // rgba(20,23,26,.95)

    // Text
    val TextPrimary = Color(0xFFEDEDE9)
    val TextBright = Color(0xFFF5F5F0)

    // Accent (amber default per handoff)
    val Accent = Color(0xFFF2A93C)
    val OnAccent = Color(0xFF1A1204)

    // Status
    val Success = Color(0xFF5FBF83)
    val Warning = Color(0xFFF2C94C)
    val Danger = Color(0xFFE5484D)
    val DangerText = Color(0xFFF5A3A6)
    val DangerBg = Color(0x1FE5484D)        // rgba(229,72,77,.12)
    val DangerBorder = Color(0x4DE5484D)    // rgba(229,72,77,.3)

    /** White-based border/text at the handoff's opacity steps, e.g. text(0.45f). */
    fun ink(alpha: Float) = Color(0xFFEDEDE9).copy(alpha = alpha)
}
