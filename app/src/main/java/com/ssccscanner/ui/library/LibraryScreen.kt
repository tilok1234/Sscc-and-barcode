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
            com.ssccscanner.ui.SettingsSheet(viewModel = viewModel, onDismiss = { settingsOpen = false })
        }

        if (pickerOpen) {
            val activeId by viewModel.activeDocumentId.collectAsState()
            com.ssccscanner.ui.DocumentPickerSheet(
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
            Text(
                text = doc.name,
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

// --- Document detail ---

@Composable
private fun DocumentDetail(
    viewModel: DocumentsViewModel,
    document: DocumentSummary,
    scans: List<ScanEntity>,
    onBack: () -> Unit,
    onOpenScan: (ScanEntity) -> Unit,
) {
    val context = LocalContext.current
    val toast = LocalToast.current
    var renaming by remember { mutableStateOf(false) }
    var renameValue by remember { mutableStateOf(document.name) }

    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                tint = Tokens.TextPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
            )
            if (renaming) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
                        .border(1.dp, Tokens.ink(0.16f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    BasicTextField(
                        value = renameValue,
                        onValueChange = { renameValue = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Tokens.TextBright, fontFamily = PlexSans, fontSize = 13.sp),
                        cursorBrush = SolidColor(Tokens.Accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = "Save",
                    color = Tokens.Accent,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        viewModel.renameDocument(document.id, renameValue)
                        renaming = false
                        toast.show("Document renamed")
                    },
                )
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Cancel rename",
                    tint = Tokens.ink(0.6f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { renaming = false },
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.name,
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (document.scanCount == 1) "1 scan" else "${document.scanCount} scans",
                        color = Tokens.ink(0.45f),
                        fontFamily = PlexMono,
                        fontSize = 11.sp,
                    )
                }
                Icon(
                    imageVector = AppIcons.Pencil,
                    contentDescription = "Rename",
                    tint = Tokens.ink(0.6f),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            renameValue = document.name
                            renaming = true
                        },
                )
                val exportEnabled = scans.isNotEmpty()
                Box(
                    modifier = Modifier
                        .background(
                            Tokens.Accent.copy(alpha = if (exportEnabled) 1f else 0.45f),
                            RoundedCornerShape(100.dp),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = exportEnabled,
                        ) { viewModel.exportCsv(context, document) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = "Export CSV",
                        color = Tokens.OnAccent,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                    )
                }
            }
        }

        if (scans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "No scans in this document yet",
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "Scans you take while filing here will show up in this list.",
                        color = Tokens.ink(0.5f),
                        fontFamily = PlexSans,
                        fontSize = 11.5.sp,
                    )
                }
            }
        } else {
            LazyColumn {
                items(scans, key = { it.id }) { scan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onOpenScan(scan) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Thumb(scan.thumbnail, size = 48.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scan.sscc ?: scan.gtin ?: "—",
                                color = Tokens.TextBright,
                                fontFamily = PlexMono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val batchLabel = scan.batchNo?.let { "Batch $it · " }.orEmpty()
                            Text(
                                text = batchLabel + relativeTime(scan.timestamp),
                                color = Tokens.ink(0.45f),
                                fontFamily = PlexSans,
                                fontSize = 11.sp,
                            )
                        }
                        // Scan delete is immediate — lower stakes than a whole document.
                        Text(
                            text = "✕",
                            color = Tokens.ink(0.6f),
                            fontFamily = PlexSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { viewModel.deleteScan(scan.id) }
                                .padding(6.dp),
                        )
                    }
                }
            }
        }
    }
}

// --- Scan detail ---

@Composable
private fun ScanDetail(
    scan: ScanEntity,
    documentName: String,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onReportDamage: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val toast = LocalToast.current

    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = "Back",
                tint = Tokens.TextPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
            )
            Text(
                text = "${relativeTime(scan.timestamp)} · $documentName",
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Full label photo when stored; falls back to the list thumbnail.
            val bmp = remember(scan.id) {
                scan.labelPhotoPath?.let { com.ssccscanner.scan.ImageUtils.decodeFileScaled(it, maxWidth = 1600) }
                    ?: ScanViewModel.decodeThumbnail(scan.thumbnail)
            }
            var viewingLabel by remember(scan.id) { mutableStateOf(false) }
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Label photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Tokens.Panel, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { viewingLabel = true },
                )
                if (viewingLabel) {
                    com.ssccscanner.ui.FullscreenPhotoOverlay(bitmap = bmp, onDismiss = { viewingLabel = false })
                }
            }

            DetailField(label = "SSCC", value = scan.sscc, big = true) {
                clipboard.setText(AnnotatedString(it))
                toast.show("SSCC copied")
            }
            DetailField(label = "Batch no.", value = scan.batchNo) {
                clipboard.setText(AnnotatedString(it))
                toast.show("Batch number copied")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniField("GTIN/EAN", scan.gtin, Modifier.weight(1f))
                MiniField("Best before", scan.bestBefore, Modifier.weight(1f))
                MiniField("Quantity", scan.quantity, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.DangerBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onReportDamage(scan.id) }
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Alert,
                    contentDescription = null,
                    tint = Tokens.DangerText,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Report damage on this pallet",
                    color = Tokens.DangerText,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Tokens.Panel).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Tokens.DangerBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDelete,
                    )
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Delete",
                    color = Tokens.DangerText,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Tokens.Accent, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        val summary = buildString {
                            appendLine("SSCC: ${scan.sscc ?: "—"}")
                            appendLine("Batch: ${scan.batchNo ?: "—"}")
                            appendLine("GTIN/EAN: ${scan.gtin ?: "—"}")
                            appendLine("Best before: ${scan.bestBefore ?: "—"}")
                            append("Quantity: ${scan.quantity ?: "—"}")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summary)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share scan"))
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Share",
                    color = Tokens.OnAccent,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

// --- Small shared pieces ---

@Composable
private fun Thumb(bytes: ByteArray?, size: androidx.compose.ui.unit.Dp) {
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
private fun DetailField(label: String, value: String?, big: Boolean = false, onCopy: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label.uppercase(),
                color = Tokens.ink(0.45f),
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.08.em,
            )
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

@Composable
private fun MiniField(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            color = Tokens.ink(0.45f),
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.08.em,
        )
        Text(
            text = value ?: "—",
            color = if (value != null) Tokens.TextBright else Tokens.ink(0.4f),
            fontFamily = PlexMono,
            fontSize = 13.sp,
        )
    }
}
