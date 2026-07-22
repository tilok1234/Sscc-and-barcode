package com.ssccscanner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens
import kotlinx.coroutines.delay

/** The handoff's global transient toast — dark pill near the bottom, ~1.8s. */
class ToastController {
    var message by mutableStateOf<String?>(null)
        private set
    var shownAt by mutableLongStateOf(0L)
        private set

    fun show(text: String) {
        message = text
        shownAt = System.currentTimeMillis()
    }

    fun clear() {
        message = null
    }
}

val LocalToast = staticCompositionLocalOf<ToastController> {
    error("ToastController not provided")
}

@Composable
fun ToastHost(controller: ToastController) {
    val message = controller.message
    LaunchedEffect(message, controller.shownAt) {
        if (message != null) {
            delay(1800)
            controller.clear()
        }
    }
    Box(modifier = Modifier.fillMaxSize().padding(bottom = 78.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .background(Tokens.ToastBg, RoundedCornerShape(100.dp))
                    .border(1.dp, Tokens.ink(0.14f), RoundedCornerShape(100.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.orEmpty(),
                    color = Tokens.TextBright,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** "just now", "5m ago", "3h ago", "2d ago" — matching the prototype's relative times. */
fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - timestamp
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "just now"
        diff < hour -> "${diff / minute}m ago"
        diff < day -> "${diff / hour}h ago"
        else -> "${diff / day}d ago"
    }
}
