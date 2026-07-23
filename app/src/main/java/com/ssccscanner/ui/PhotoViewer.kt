package com.ssccscanner.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens

/**
 * Full-screen photo overlay; tap anywhere or the Close pill to dismiss.
 * Pass [onDelete] to also offer a delete action.
 */
@Composable
fun FullscreenPhotoOverlay(bitmap: Bitmap, onDismiss: () -> Unit, onDelete: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.Void.copy(alpha = 0.96f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(vertical = 60.dp),
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .background(Tokens.DangerBg, RoundedCornerShape(100.dp))
                        .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(100.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete,
                        )
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Delete photo",
                        color = Tokens.DangerText,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(Tokens.ink(0.1f), RoundedCornerShape(100.dp))
                    .border(1.dp, Tokens.ink(0.2f), RoundedCornerShape(100.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Close",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
