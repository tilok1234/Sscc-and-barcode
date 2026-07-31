package com.ssccscanner.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssccscanner.ui.AppIcons
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens

/**
 * The experimental toolbox: a hub of small standalone tools that grow with
 * field needs. Each tool is its own screen; the hub is just the doorway, so
 * new tools slot in as extra cards without touching navigation.
 */
@Composable
fun ToolsScreen(
    scheduleViewModel: ScheduleViewModel,
    articlesViewModel: ArticlesViewModel,
) {
    // null = hub; otherwise the open tool's key.
    var openTool by rememberSaveable { mutableStateOf<String?>(null) }

    when (openTool) {
        "schedule" -> ScheduleScreen(viewModel = scheduleViewModel, onBack = { openTool = null })
        "articles" -> ArticlesScreen(viewModel = articlesViewModel, onBack = { openTool = null })
        else -> ToolsHub(
            scheduleViewModel = scheduleViewModel,
            articlesViewModel = articlesViewModel,
            onOpen = { openTool = it },
        )
    }
}

@Composable
private fun ToolsHub(
    scheduleViewModel: ScheduleViewModel,
    articlesViewModel: ArticlesViewModel,
    onOpen: (String) -> Unit,
) {
    val upcoming by scheduleViewModel.upcomingCount.collectAsState()
    val articleCount by articlesViewModel.count.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = "Tools",
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Text(
                text = "Handy extras, growing as needed",
                color = Tokens.ink(0.45f),
                fontFamily = PlexMono,
                fontSize = 12.sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ToolCard(
                icon = AppIcons.Calendar,
                title = "Schedule",
                subtitle = when (upcoming) {
                    0 -> "No upcoming appointments"
                    1 -> "1 upcoming appointment"
                    else -> "$upcoming upcoming appointments"
                },
                onClick = { onOpen("schedule") },
            )
            ToolCard(
                icon = AppIcons.Tag,
                title = "Articles",
                subtitle = when (articleCount) {
                    0 -> "No articles registered yet"
                    1 -> "1 article registered"
                    else -> "$articleCount articles registered"
                },
                onClick = { onOpen("articles") },
            )
        }
    }
}

@Composable
private fun ToolCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.Panel, RoundedCornerShape(12.dp))
            .border(1.dp, Tokens.ink(0.12f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Tokens.Accent.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .border(1.dp, Tokens.Accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Tokens.Accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
            )
            Text(
                text = subtitle,
                color = Tokens.ink(0.45f),
                fontFamily = PlexSans,
                fontSize = 11.5.sp,
            )
        }
        // Chevron-down rotated into a "go" chevron-right.
        Icon(
            imageVector = AppIcons.ChevronDown,
            contentDescription = null,
            tint = Tokens.ink(0.35f),
            modifier = Modifier.size(14.dp).rotate(-90f),
        )
    }
}
