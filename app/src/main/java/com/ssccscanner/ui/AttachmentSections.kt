package com.ssccscanner.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ssccscanner.scan.ImageUtils
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens

/** Presentation shape for a note row, decoupled from the Room entities. */
data class NoteItem(
    val id: String,
    val text: String,
    val createdAt: Long,
    val kind: String = "note", // note | restored | sanitized
)

/** Presentation shape for a photo tile. */
data class PhotoItem(val id: String, val filePath: String)

/**
 * Timestamped, appendable note log. Status-change notes ("restored"/
 * "sanitized") are labeled and tinted.
 */
@Composable
fun NotesSection(
    notes: List<NoteItem>,
    onAdd: (String) -> Unit,
    onDelete: (NoteItem) -> Unit,
    title: String = "Notes",
) {
    var draft by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title)

        notes.sortedBy { it.createdAt }.forEach { note ->
            val (label, labelColor) = when (note.kind) {
                "restored" -> "Restored · ${relativeTime(note.createdAt)}" to Tokens.Success
                "sanitized" -> "Sanitized · ${relativeTime(note.createdAt)}" to Tokens.Danger
                else -> relativeTime(note.createdAt) to Tokens.ink(0.4f)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.ink(0.05f), RoundedCornerShape(9.dp))
                    .border(
                        1.dp,
                        when (note.kind) {
                            "restored" -> Tokens.Success.copy(alpha = 0.35f)
                            "sanitized" -> Tokens.DangerBorder
                            else -> Tokens.ink(0.12f)
                        },
                        RoundedCornerShape(9.dp),
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label.uppercase(),
                        color = labelColor,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.08.em,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "✕",
                        color = Tokens.ink(0.4f),
                        fontFamily = PlexSans,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onDelete(note) }
                            .padding(horizontal = 4.dp),
                    )
                }
                Text(
                    text = note.text,
                    color = Tokens.TextBright,
                    fontFamily = PlexSans,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        // Composer
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
                    .border(1.dp, Tokens.ink(0.16f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                if (draft.isEmpty()) {
                    Text(
                        text = "Add a note…",
                        color = Tokens.ink(0.35f),
                        fontFamily = PlexSans,
                        fontSize = 12.5.sp,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(
                        color = Tokens.TextBright,
                        fontFamily = PlexSans,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    ),
                    cursorBrush = SolidColor(Tokens.Accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val enabled = draft.isNotBlank()
            Box(
                modifier = Modifier
                    .background(Tokens.Accent.copy(alpha = if (enabled) 1f else 0.45f), RoundedCornerShape(9.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                    ) {
                        onAdd(draft.trim())
                        draft = ""
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Add",
                    color = Tokens.OnAccent,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

/** 3-per-row photo grid with a camera "add" tile and a Gallery text action. */
@Composable
fun PhotosSection(
    photos: List<PhotoItem>,
    onAddCamera: () -> Unit,
    onAddGallery: () -> Unit,
    onOpen: (PhotoItem) -> Unit,
    title: String = "Photos",
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Gallery",
                color = Tokens.Accent,
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddGallery,
                    )
                    .padding(6.dp),
            )
        }

        val cells: List<PhotoItem?> = photos + listOf(null) // trailing null = add tile
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cells.chunked(3).forEach { rowCells ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowCells.forEach { cell ->
                        if (cell == null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(Tokens.ink(0.06f), RoundedCornerShape(10.dp))
                                    .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onAddCamera,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Plus,
                                        contentDescription = "Take photo",
                                        tint = Tokens.Accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = "Photo",
                                        color = Tokens.ink(0.6f),
                                        fontFamily = PlexSans,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                    )
                                }
                            }
                        } else {
                            val bmp = remember(cell.id) { ImageUtils.decodeFileScaled(cell.filePath, maxWidth = 500) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(Tokens.Panel, RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { onOpen(cell) },
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                    repeat(3 - rowCells.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = Tokens.ink(0.45f),
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em,
    )
}
