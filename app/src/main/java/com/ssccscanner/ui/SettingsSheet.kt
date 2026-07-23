package com.ssccscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens

/**
 * Storage & retention settings. Compression drops stored label photos from
 * old scans (data and thumbnails stay); deletion removes old scans entirely —
 * except pallets with damage reports, which are never auto-deleted.
 */
@Composable
fun SettingsSheet(
    viewModel: DocumentsViewModel,
    onDismiss: () -> Unit,
) {
    val compressDays by viewModel.compressAfterDays.collectAsState()
    val deleteDays by viewModel.deleteAfterDays.collectAsState()
    val storageBytes by viewModel.photoStorageBytes.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshStorageUsage() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Tokens.Void.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Column(
            modifier = Modifier
                .padding(top = 88.dp)
                .fillMaxWidth(0.88f)
                .align(Alignment.TopCenter)
                .background(Tokens.Panel, RoundedCornerShape(16.dp))
                .border(1.dp, Tokens.ink(0.14f), RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Storage & cleanup",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Close",
                    tint = Tokens.ink(0.6f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                )
            }

            Text(
                text = "Photos on disk: ${formatBytes(storageBytes)}",
                color = Tokens.ink(0.6f),
                fontFamily = PlexMono,
                fontSize = 12.sp,
            )

            SettingSection(
                title = "Auto-compress scans",
                description = "Removes the stored label photo from scans older than this. The extracted data and small thumbnail are kept.",
                options = listOf(0 to "Never", 30 to "30 d", 90 to "90 d", 180 to "180 d"),
                selected = compressDays,
                onSelect = viewModel::setCompressAfterDays,
            )

            SettingSection(
                title = "Auto-delete scans",
                description = "Deletes scans older than this. Pallets with damage reports are never auto-deleted.",
                options = listOf(0 to "Never", 90 to "90 d", 180 to "180 d", 365 to "1 y"),
                selected = deleteDays,
                onSelect = viewModel::setDeleteAfterDays,
            )

            Text(
                text = "Cleanup runs each time the app starts.",
                color = Tokens.ink(0.4f),
                fontFamily = PlexSans,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    description: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            color = Tokens.ink(0.45f),
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 0.08.em,
        )
        Text(
            text = description,
            color = Tokens.ink(0.55f),
            fontFamily = PlexSans,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) Tokens.Accent else Tokens.ink(0.06f),
                            RoundedCornerShape(100.dp),
                        )
                        .border(
                            1.dp,
                            if (isSelected) Tokens.Accent else Tokens.ink(0.16f),
                            RoundedCornerShape(100.dp),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(value) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Tokens.OnAccent else Tokens.ink(0.7f),
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
