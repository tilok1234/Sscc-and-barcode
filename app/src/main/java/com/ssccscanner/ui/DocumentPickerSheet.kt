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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssccscanner.data.DocumentSummary
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens

/**
 * "File to document" modal sheet, anchored near the TOP of the screen per the
 * handoff (bottom sheets get pushed below the fold on phones).
 */
@Composable
fun DocumentPickerSheet(
    documents: List<DocumentSummary>,
    activeDocumentId: String?,
    onSelect: (DocumentSummary) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }

    // Scrim
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
        // Card
        Column(
            modifier = Modifier
                .padding(top = 88.dp)
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.64f)
                .align(Alignment.TopCenter)
                .background(Tokens.Panel, RoundedCornerShape(16.dp))
                .border(1.dp, Tokens.ink(0.14f), RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "File to document",
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

            // Document list
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(documents, key = { it.id }) { doc ->
                    val selected = doc.id == activeDocumentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(doc) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(
                                    width = if (selected) 5.dp else 1.5.dp,
                                    color = if (selected) Tokens.Accent else Tokens.ink(0.4f),
                                    shape = CircleShape,
                                ),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = doc.name,
                                    color = Tokens.TextPrimary,
                                    fontFamily = PlexSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (doc.isBatch) {
                                    Box(
                                        modifier = Modifier
                                            .background(Tokens.Accent.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                                            .border(1.dp, Tokens.Accent.copy(alpha = 0.45f), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            text = "BATCH",
                                            color = Tokens.Accent,
                                            fontFamily = PlexSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (doc.scanCount == 1) "1 scan" else "${doc.scanCount} scans",
                                color = Tokens.ink(0.45f),
                                fontFamily = PlexSans,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            // Footer: create new document
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
                        .border(1.dp, Tokens.ink(0.16f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                ) {
                    if (newName.isEmpty()) {
                        Text(
                            text = "New document name",
                            color = Tokens.ink(0.4f),
                            fontFamily = PlexSans,
                            fontSize = 13.sp,
                        )
                    }
                    BasicTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Tokens.TextBright,
                            fontFamily = PlexSans,
                            fontSize = 13.sp,
                        ),
                        cursorBrush = SolidColor(Tokens.Accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val enabled = newName.isNotBlank()
                Box(
                    modifier = Modifier
                        .background(
                            Tokens.Accent.copy(alpha = if (enabled) 1f else 0.45f),
                            RoundedCornerShape(9.dp),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = enabled,
                        ) {
                            onCreate(newName.trim())
                            newName = ""
                        }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                ) {
                    Text(
                        text = "Create",
                        color = Tokens.OnAccent,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
