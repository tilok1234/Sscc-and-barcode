package com.ssccscanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens

/**
 * The app's shared building blocks — one definition each for the buttons,
 * field labels, read-only field displays and edit inputs that every screen
 * uses. Keep visual changes here so screens stay consistent.
 */

@Composable
fun PrimaryButton(text: String, icon: ImageVector?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.Accent, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Tokens.OnAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Tokens.OnAccent,
            fontFamily = PlexSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
        )
    }
}

@Composable
fun SecondaryPill(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Tokens.ink(0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Tokens.TextPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = Tokens.TextPrimary,
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

/** Uppercase micro-label above a data field. */
@Composable
fun FieldLabel(text: String, small: Boolean = false) {
    Text(
        text = text.uppercase(),
        color = Tokens.ink(0.45f),
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = if (small) 10.sp else 11.sp,
        letterSpacing = 0.08.em,
    )
}

/** Large mono value with a label row and inline Copy action. */
@Composable
fun DataField(label: String, value: String?, big: Boolean = false, onCopy: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label)
            Spacer(modifier = Modifier.weight(1f))
            if (value != null) {
                Text(
                    text = "Copy",
                    color = Tokens.Accent,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onCopy(value) },
                )
            }
        }
        Text(
            text = value ?: "—",
            color = if (value != null) Tokens.TextBright else Tokens.ink(0.4f),
            fontFamily = PlexMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (big) 21.sp else 18.sp,
            letterSpacing = 0.02.em,
        )
    }
}

/** Compact label+value cell for the three-column field grids. */
@Composable
fun SmallField(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label, small = true)
        Text(
            text = value ?: "—",
            color = if (value != null) Tokens.TextBright else Tokens.ink(0.4f),
            fontFamily = PlexMono,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Single-line edit input in the app's field style; red-tinted when invalid. */
@Composable
fun EditField(
    value: String,
    onChange: (String) -> Unit,
    mono: Boolean = false,
    numeric: Boolean = false,
    isError: Boolean = false,
    fontSize: TextUnit = 14.sp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
            .border(
                1.dp,
                if (isError) Tokens.Danger.copy(alpha = 0.5f) else Tokens.ink(0.16f),
                RoundedCornerShape(9.dp),
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            textStyle = TextStyle(
                color = Tokens.TextBright,
                fontFamily = if (mono) PlexMono else PlexSans,
                fontSize = fontSize,
            ),
            cursorBrush = SolidColor(Tokens.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
