package com.ssccscanner.ui.library

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ssccscanner.data.DocumentSummary
import com.ssccscanner.data.ScanEntity
import com.ssccscanner.scan.ScanViewModel
import com.ssccscanner.ui.AppIcons
import com.ssccscanner.ui.DocumentsViewModel
import com.ssccscanner.ui.LocalToast
import com.ssccscanner.ui.relativeTime
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens
import kotlinx.coroutines.delay
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.Dp
import androidx.core.content.FileProvider
import com.ssccscanner.core.BatchAnalyzer
import com.ssccscanner.core.SsccValidator
import com.ssccscanner.data.FieldEditEntity
import com.ssccscanner.data.ScanPhotoEntity
import com.ssccscanner.data.ScannerRepository
import com.ssccscanner.scan.ImageUtils
import com.ssccscanner.ui.DocumentPickerSheet
import com.ssccscanner.ui.FullscreenPhotoOverlay
import com.ssccscanner.ui.NoteItem
import com.ssccscanner.ui.NotesSection
import com.ssccscanner.ui.PhotoItem
import com.ssccscanner.ui.PhotosSection
import com.ssccscanner.ui.SettingsSheet
import com.ssccscanner.ui.components.DataField
import com.ssccscanner.ui.components.EditField
import com.ssccscanner.ui.components.FieldLabel
import com.ssccscanner.ui.components.PrimaryButton
import com.ssccscanner.ui.components.SecondaryPill
import com.ssccscanner.ui.components.SmallField
import java.io.File

@Composable
fun LibraryScreen(
    viewModel: DocumentsViewModel,
    onReportDamage: (String) -> Unit = {},
) {
    var openDocId by rememberSaveable { mutableStateOf<String?>(null) }
    var openScanId by rememberSaveable { mutableStateOf<String?>(null) }

    val documents by viewModel.documents.collectAsState()
    val openDoc = documents.firstOrNull { it.id == openDocId }

    when {
        openDoc == null -> DocumentList(
            viewModel = viewModel,
            documents = documents,
            onOpen = { openDocId = it.id },
        )

        else -> {
            val scans by remember(openDoc.id) { viewModel.scansFor(openDoc.id) }.collectAsState(initial = emptyList())
            val openScan = scans.firstOrNull { it.id == openScanId }
            if (openScan != null) {
                ScanDetail(
                    viewModel = viewModel,
                    scan = openScan,
                    documentName = openDoc.name,
                    onBack = { openScanId = null },
                    onDelete = {
                        viewModel.deleteScan(openScan.id)
                        openScanId = null
                    },
                    onReportDamage = onReportDamage,
                )
            } else {
                DocumentDetail(
                    viewModel = viewModel,
                    document = openDoc,
                    scans = scans,
                    onBack = { openDocId = null },
                    onOpenScan = { openScanId = it.id },
                )
            }
        }
    }
}

// --- Document list ---

@Composable
private fun DocumentList(
    viewModel: DocumentsViewModel,
    documents: List<DocumentSummary>,
    onOpen: (DocumentSummary) -> Unit,
) {
    val toast = LocalToast.current
    var pickerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    // Two-tap delete arms for ~2.5s, then reverts (handoff behavior).
    LaunchedEffect(confirmDeleteId) {
        if (confirmDeleteId != null) {
            delay(2500)
            confirmDeleteId = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Tokens.Surface)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Library",
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    )
                    Text(
                        text = if (documents.size == 1) "1 document" else "${documents.size} documents",
                        color = Tokens.ink(0.45f),
                        fontFamily = PlexMono,
                        fontSize = 12.sp,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = AppIcons.Sliders,
                    contentDescription = "Settings",
                    tint = Tokens.ink(0.6f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { settingsOpen = true },
                )
                Spacer(modifier = Modifier.size(12.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(100.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { pickerOpen = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "+ New",
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }

            if (documents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No documents yet — scan a label to get started.",
                        color = Tokens.ink(0.5f),
                        fontFamily = PlexSans,
                        fontSize = 12.5.sp,
                    )
                }
            } else {
                LazyColumn {
                    items(documents, key = { it.id }) { doc ->
                        DocumentRow(
                            doc = doc,
                            armed = confirmDeleteId == doc.id,
                            onClick = { onOpen(doc) },
                            onDelete = {
                                if (confirmDeleteId == doc.id) {
                                    viewModel.deleteDocument(doc.id)
                                    confirmDeleteId = null
                                    toast.show("Document deleted")
                                } else {
                                    confirmDeleteId = doc.id
                                }
                            },
                        )
                    }
                }
            }
        }

        if (settingsOpen) {
            SettingsSheet(viewModel = viewModel, onDismiss = { settingsOpen = false })
        }

        if (pickerOpen) {
            val activeId by viewModel.activeDocumentId.collectAsState()
            DocumentPickerSheet(
                documents = documents,
                activeDocumentId = activeId,
                onSelect = {
                    viewModel.setActiveDocument(it.id)
                    toast.show("Filing to ${it.name}")
                    pickerOpen = false
                },
                onCreate = {
                    viewModel.createDocument(it)
                    toast.show("Filing to $it")
                    pickerOpen = false
                },
                onDismiss = { pickerOpen = false },
            )
        }
    }
}

@Composable
private fun DocumentRow(
    doc: DocumentSummary,
    armed: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Thumb(doc.lastThumbnail, size = 48.dp)
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
                if (doc.isBatch) BatchChip()
            }
            val scansLabel = if (doc.scanCount == 1) "1 scan" else "${doc.scanCount} scans"
            val timeLabel = doc.lastScanAt?.let { " · ${relativeTime(it)}" }.orEmpty()
            Text(
                text = scansLabel + timeLabel,
                color = Tokens.ink(0.45f),
                fontFamily = PlexSans,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier = Modifier
                .background(
                    if (armed) Tokens.DangerBg else Tokens.ink(0.06f),
                    RoundedCornerShape(100.dp),
                )
                .border(
                    1.dp,
                    if (armed) Tokens.DangerBorder else Tokens.ink(0.14f),
                    RoundedCornerShape(100.dp),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDelete,
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = if (armed) "Sure?" else "✕",
                color = if (armed) Tokens.DangerText else Tokens.ink(0.6f),
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
        }
    }
}
// --- Small shared pieces ---

@Composable
internal fun Thumb(bytes: ByteArray?, size: Dp) {
    val bmp = remember(bytes) { ScanViewModel.decodeThumbnail(bytes) }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).background(Tokens.Panel, RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(Tokens.ink(0.06f), RoundedCornerShape(8.dp))
                .border(1.dp, Tokens.ink(0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Folder,
                contentDescription = null,
                tint = Tokens.ink(0.4f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
@Composable
internal fun BatchChip() {
    Box(
        modifier = Modifier
            .background(Tokens.Accent.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
            .border(1.dp, Tokens.Accent.copy(alpha = 0.45f), RoundedCornerShape(100.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = "BATCH",
            color = Tokens.Accent,
            fontFamily = PlexMono,
            fontWeight = FontWeight.Bold,
            fontSize = 8.5.sp,
            letterSpacing = 0.06.em,
        )
    }
}
