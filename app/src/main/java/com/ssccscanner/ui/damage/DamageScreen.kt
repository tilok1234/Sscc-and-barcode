package com.ssccscanner.ui.damage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ssccscanner.data.DamagePhotoEntity
import com.ssccscanner.data.DamageReportEntity
import com.ssccscanner.data.DamageReportWithScan
import com.ssccscanner.scan.ImageUtils
import com.ssccscanner.scan.ScanViewModel
import com.ssccscanner.ui.AppIcons
import com.ssccscanner.ui.FullscreenPhotoOverlay
import com.ssccscanner.ui.LocalToast
import com.ssccscanner.ui.NoteItem
import com.ssccscanner.ui.NotesSection
import com.ssccscanner.ui.PhotoItem
import com.ssccscanner.ui.PhotosSection
import com.ssccscanner.ui.SectionHeader
import com.ssccscanner.ui.relativeTime
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DamageScreen(viewModel: DamageViewModel) {
    val reports by viewModel.reports.collectAsState()
    var openReportId by rememberSaveable { mutableStateOf<String?>(null) }

    // Cross-tab "Report damage" jump from the Scan/Library screens.
    val openRequest by viewModel.openRequest.collectAsState()
    LaunchedEffect(openRequest) {
        if (openRequest != null) {
            openReportId = openRequest
            viewModel.consumeOpenRequest()
        }
    }

    val openReport = reports.firstOrNull { it.report.id == openReportId }
    if (openReport != null) {
        DamageDetail(
            viewModel = viewModel,
            entry = openReport,
            onBack = { openReportId = null },
            onDeleted = { openReportId = null },
        )
    } else {
        DamageList(reports = reports, onOpen = { openReportId = it.report.id })
    }
}

// --- Status helpers ---

private data class StatusLook(val icon: ImageVector, val tint: Color, val label: String)

private fun statusLook(status: String): StatusLook = when (status) {
    DamageReportEntity.STATUS_RESTORED -> StatusLook(AppIcons.Check, Tokens.Success, "Restored")
    DamageReportEntity.STATUS_SANITIZED -> StatusLook(AppIcons.Close, Tokens.Danger, "Sanitized")
    else -> StatusLook(AppIcons.Alert, Tokens.Warning, "Open")
}

// --- List ---

@Composable
private fun DamageList(
    reports: List<DamageReportWithScan>,
    onOpen: (DamageReportWithScan) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Damage log",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                val open = reports.count { it.report.status == DamageReportEntity.STATUS_OPEN }
                Text(
                    text = "${reports.size} total · $open open",
                    color = Tokens.ink(0.45f),
                    fontFamily = PlexMono,
                    fontSize = 12.sp,
                )
            }
        }

        if (reports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = AppIcons.Alert,
                        contentDescription = null,
                        tint = Tokens.ink(0.3f),
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = "No damage reports",
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "Scan a pallet's label, then tap \"Report damage\"\non the result to log damaged goods.",
                        color = Tokens.ink(0.5f),
                        fontFamily = PlexSans,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn {
                items(reports, key = { it.report.id }) { entry ->
                    val look = statusLook(entry.report.status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onOpen(entry) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val thumb = remember(entry.scan.id) { ScanViewModel.decodeThumbnail(entry.scan.thumbnail) }
                        if (thumb != null) {
                            Image(
                                bitmap = thumb.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).background(Tokens.Panel, RoundedCornerShape(8.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Tokens.DangerBg, RoundedCornerShape(8.dp))
                                    .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = look.icon,
                                    contentDescription = null,
                                    tint = look.tint,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.scan.sscc ?: entry.scan.gtin ?: entry.scan.batchNo ?: "—",
                                color = Tokens.TextBright,
                                fontFamily = PlexMono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val photoLabel = when (entry.photos.size) {
                                0 -> "No photos"
                                1 -> "1 photo"
                                else -> "${entry.photos.size} photos"
                            }
                            val lastNote = entry.notes.maxByOrNull { it.createdAt }?.text?.let { " · $it" }.orEmpty()
                            Text(
                                text = "$photoLabel · ${relativeTime(entry.report.createdAt)}$lastNote",
                                color = Tokens.ink(0.45f),
                                fontFamily = PlexSans,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = look.icon,
                            contentDescription = look.label,
                            tint = look.tint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

// --- Detail ---

@Composable
private fun DamageDetail(
    viewModel: DamageViewModel,
    entry: DamageReportWithScan,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val reportId = entry.report.id
    val look = statusLook(entry.report.status)

    var viewingPhoto by remember { mutableStateOf<DamagePhotoEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Set when Restored/Sanitized was just pressed: opens the status-note composer.
    var statusNoteKind by remember { mutableStateOf<String?>(null) }
    var statusNoteDraft by remember { mutableStateOf("") }
    // Quantity often changes when a pallet is restored (some units scrapped).
    var restoredQtyDraft by remember { mutableStateOf("") }

    // Camera capture into a FileProvider cache uri, then persisted by the VM.
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCaptureUri
        if (ok && uri != null) {
            viewModel.addPhoto(reportId, uri) { saved ->
                if (!saved) toast.show("Couldn't save photo")
            }
        }
        pendingCaptureUri = null
    }
    val capturePhoto = {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "damage_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCaptureUri = uri
        takePicture.launch(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            viewModel.addPhoto(reportId, it) { saved ->
                if (!saved) toast.show("Couldn't add that image")
            }
        }
    }

    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            kotlinx.coroutines.delay(2500)
            confirmDelete = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        // Header
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Damage report",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Text(
                    text = relativeTime(entry.report.createdAt),
                    color = Tokens.ink(0.45f),
                    fontFamily = PlexMono,
                    fontSize = 11.sp,
                )
            }
            Icon(
                imageVector = look.icon,
                contentDescription = look.label,
                tint = look.tint,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Status chip + actions
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .background(look.tint.copy(alpha = 0.14f), RoundedCornerShape(100.dp))
                            .border(1.dp, look.tint.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = look.icon,
                            contentDescription = null,
                            tint = look.tint,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = look.label.uppercase(),
                            color = look.tint,
                            fontFamily = PlexMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.08.em,
                        )
                    }
                    if (entry.report.status != DamageReportEntity.STATUS_OPEN) {
                        Text(
                            text = "Reopen",
                            color = Tokens.ink(0.55f),
                            fontFamily = PlexSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    viewModel.setStatus(reportId, DamageReportEntity.STATUS_OPEN)
                                    toast.show("Report reopened")
                                }
                                .padding(4.dp),
                        )
                    }
                }

                if (entry.report.status == DamageReportEntity.STATUS_OPEN) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusActionButton(
                            text = "Report restored",
                            tint = Tokens.Success,
                            modifier = Modifier.weight(1f),
                        ) {
                            viewModel.setStatus(reportId, DamageReportEntity.STATUS_RESTORED)
                            statusNoteKind = DamageReportEntity.STATUS_RESTORED
                            statusNoteDraft = ""
                            restoredQtyDraft = entry.scan.quantity.orEmpty()
                        }
                        StatusActionButton(
                            text = "Report sanitized",
                            tint = Tokens.Danger,
                            modifier = Modifier.weight(1f),
                        ) {
                            viewModel.setStatus(reportId, DamageReportEntity.STATUS_SANITIZED)
                            statusNoteKind = DamageReportEntity.STATUS_SANITIZED
                            statusNoteDraft = ""
                        }
                    }
                }

                // Status-note composer, opened right after pressing a status button
                val kind = statusNoteKind
                if (kind != null) {
                    val kindTint = if (kind == DamageReportEntity.STATUS_RESTORED) Tokens.Success else Tokens.Danger
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(kindTint.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .border(1.dp, kindTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionHeader(
                            if (kind == DamageReportEntity.STATUS_RESTORED) {
                                "Restoration note"
                            } else {
                                "Sanitation note"
                            },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
                                .border(1.dp, Tokens.ink(0.16f), RoundedCornerShape(9.dp))
                                .padding(10.dp),
                        ) {
                            if (statusNoteDraft.isEmpty()) {
                                Text(
                                    text = if (kind == DamageReportEntity.STATUS_RESTORED) {
                                        "e.g. restacked and shrink-wrapped, took 40 min, sent to line 2…"
                                    } else {
                                        "e.g. 12 cans leaking, whole layer scrapped and disposed…"
                                    },
                                    color = Tokens.ink(0.35f),
                                    fontFamily = PlexSans,
                                    fontSize = 12.5.sp,
                                )
                            }
                            BasicTextField(
                                value = statusNoteDraft,
                                onValueChange = { statusNoteDraft = it },
                                textStyle = TextStyle(
                                    color = Tokens.TextBright,
                                    fontFamily = PlexSans,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                ),
                                cursorBrush = SolidColor(Tokens.Accent),
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                            )
                        }
                        if (kind == DamageReportEntity.STATUS_RESTORED) {
                            // Restoration often changes the count (scrapped units).
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "QTY AFTER RESTORE",
                                    color = Tokens.ink(0.45f),
                                    fontFamily = PlexSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 0.08.em,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
                                        .border(1.dp, Tokens.ink(0.16f), RoundedCornerShape(9.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    BasicTextField(
                                        value = restoredQtyDraft,
                                        onValueChange = { v -> restoredQtyDraft = v.filter(Char::isDigit) },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Tokens.TextBright,
                                            fontFamily = PlexMono,
                                            fontSize = 13.sp,
                                        ),
                                        cursorBrush = SolidColor(Tokens.Accent),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (entry.scan.quantity != null && restoredQtyDraft != entry.scan.quantity) {
                                    Text(
                                        text = "was ${entry.scan.quantity}",
                                        color = Tokens.Warning,
                                        fontFamily = PlexMono,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Skip",
                                color = Tokens.ink(0.5f),
                                fontFamily = PlexSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { statusNoteKind = null }
                                    .padding(6.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .background(Tokens.Accent, RoundedCornerShape(9.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        if (statusNoteDraft.isNotBlank()) {
                                            viewModel.addNote(reportId, statusNoteDraft.trim(), kind)
                                        }
                                        if (kind == DamageReportEntity.STATUS_RESTORED &&
                                            restoredQtyDraft != entry.scan.quantity.orEmpty()
                                        ) {
                                            viewModel.updateQuantity(entry.scan.id, restoredQtyDraft.ifBlank { null })
                                        }
                                        statusNoteKind = null
                                        toast.show(
                                            if (kind == DamageReportEntity.STATUS_RESTORED) {
                                                "Marked as restored"
                                            } else {
                                                "Marked as sanitized"
                                            },
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "Save note",
                                    color = Tokens.OnAccent,
                                    fontFamily = PlexSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }

            // Label summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.Panel, RoundedCornerShape(10.dp))
                    .border(1.dp, Tokens.ink(0.12f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeader("Pallet label")
                val labelBmp = remember(entry.scan.id) {
                    entry.scan.labelPhotoPath?.let { ImageUtils.decodeFileScaled(it, maxWidth = 800) }
                }
                if (labelBmp != null) {
                    Image(
                        bitmap = labelBmp.asImageBitmap(),
                        contentDescription = "Label photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(90.dp).background(Tokens.Surface, RoundedCornerShape(8.dp)),
                    )
                }
                Text(
                    text = entry.scan.sscc ?: "—",
                    color = Tokens.TextBright,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.02.em,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniField("Batch", entry.scan.batchNo, Modifier.weight(1f))
                    MiniField("Best before", entry.scan.bestBefore, Modifier.weight(1f))
                    MiniField(
                        "Quantity",
                        entry.scan.quantity?.let { q ->
                            val orig = entry.scan.originalQuantity
                            if (orig != null && orig != q) "$q (was $orig)" else q
                        } ?: entry.scan.originalQuantity?.let { "— (was $it)" },
                        Modifier.weight(1f),
                    )
                }
            }

            // Notes log — tap a note to edit it
            NotesSection(
                notes = entry.notes.map { NoteItem(it.id, it.text, it.createdAt, it.kind) },
                onAdd = { viewModel.addNote(reportId, it) },
                onDelete = { viewModel.deleteNote(it.id) },
                onEdit = { note, text -> viewModel.editNote(note.id, text) },
            )

            // Photos
            PhotosSection(
                photos = entry.photos.map { PhotoItem(it.id, it.filePath) },
                onAddCamera = capturePhoto,
                onAddGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onOpen = { item -> viewingPhoto = entry.photos.firstOrNull { it.id == item.id } },
                title = "Photos of the damage",
            )
        }

        // Footer
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
                    ) {
                        if (confirmDelete) {
                            viewModel.deleteReport(reportId)
                            toast.show("Damage report deleted")
                            onDeleted()
                        } else {
                            confirmDelete = true
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (confirmDelete) "Sure?" else "Delete",
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
                    ) { shareReport(context, entry) }
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

    // Full-screen photo viewer
    val photo = viewingPhoto
    if (photo != null) {
        val bmp = remember(photo.id) { ImageUtils.decodeFileScaled(photo.filePath, maxWidth = 1600) }
        if (bmp != null) {
            FullscreenPhotoOverlay(
                bitmap = bmp,
                onDismiss = { viewingPhoto = null },
                onDelete = {
                    viewModel.deletePhoto(photo)
                    viewingPhoto = null
                },
            )
        } else {
            viewingPhoto = null
        }
    }
}

@Composable
private fun StatusActionButton(text: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (tint == Tokens.Success) AppIcons.Check else AppIcons.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = tint,
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
        )
    }
}

@Composable
private fun MiniField(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label.uppercase(),
            color = Tokens.ink(0.45f),
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            letterSpacing = 0.08.em,
        )
        Text(
            text = value ?: "—",
            color = if (value != null) Tokens.TextBright else Tokens.ink(0.4f),
            fontFamily = PlexMono,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun shareReport(context: Context, entry: DamageReportWithScan) {
    val s = entry.scan
    val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val summary = buildString {
        appendLine("DAMAGE REPORT — ${statusLook(entry.report.status).label.uppercase()}")
        appendLine("SSCC: ${s.sscc ?: "—"}")
        appendLine("Batch: ${s.batchNo ?: "—"}")
        appendLine("GTIN/EAN: ${s.gtin ?: "—"}")
        appendLine("Best before: ${s.bestBefore ?: "—"}")
        val notes = entry.notes.sortedBy { it.createdAt }
        if (notes.isNotEmpty()) {
            appendLine()
            appendLine("Notes:")
            notes.forEach { n ->
                val tag = when (n.kind) {
                    "restored" -> " [RESTORED]"
                    "sanitized" -> " [SANITIZED]"
                    else -> ""
                }
                appendLine("- ${dateFmt.format(Date(n.createdAt))}$tag ${n.text}")
            }
        }
        append("Photos attached: ${entry.photos.size + (if (s.labelPhotoPath != null) 1 else 0)}")
    }

    // Attach the label photo first, then the damage photos.
    val allPaths = listOfNotNull(s.labelPhotoPath) + entry.photos.map { it.filePath }
    val photoUris = ArrayList(
        allPaths.mapNotNull { path ->
            val f = File(path)
            if (f.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
            } else {
                null
            }
        },
    )

    val intent = if (photoUris.isEmpty()) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, summary)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoUris)
            putExtra(Intent.EXTRA_TEXT, summary)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share damage report"))
}
