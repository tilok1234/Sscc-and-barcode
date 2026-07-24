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

// --- Scan detail ---

@Composable
internal fun ScanDetail(
    viewModel: DocumentsViewModel,
    scan: ScanEntity,
    documentName: String,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onReportDamage: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val toast = LocalToast.current

    val notes by remember(scan.id) { viewModel.scanNotes(scan.id) }.collectAsState(initial = emptyList())
    val photos by remember(scan.id) { viewModel.scanPhotos(scan.id) }.collectAsState(initial = emptyList())
    val fieldEdits by remember(scan.id) { viewModel.fieldEdits(scan.id) }.collectAsState(initial = emptyList())
    var viewingPhoto by remember { mutableStateOf<ScanPhotoEntity?>(null) }

    // Full field editing — every change lands in the immutable edit ledger.
    var editing by remember(scan.id) { mutableStateOf(false) }
    var editSscc by remember(scan.id) { mutableStateOf(scan.sscc.orEmpty()) }
    var editBatch by remember(scan.id) { mutableStateOf(scan.batchNo.orEmpty()) }
    var editGtin by remember(scan.id) { mutableStateOf(scan.gtin.orEmpty()) }
    var editBestBefore by remember(scan.id) { mutableStateOf(scan.bestBefore.orEmpty()) }
    var editQuantity by remember(scan.id) { mutableStateOf(scan.quantity.orEmpty()) }
    var editArticle by remember(scan.id) { mutableStateOf(scan.articleNo.orEmpty()) }

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = pendingCaptureUri
        if (ok && uri != null) {
            viewModel.addScanPhoto(scan.id, uri) { saved ->
                if (!saved) toast.show("Couldn't save photo")
            }
        }
        pendingCaptureUri = null
    }
    val capturePhoto = {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        pendingCaptureUri = uri
        takePicture.launch(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addScanPhoto(scan.id, it) { saved ->
                if (!saved) toast.show("Couldn't add that image")
            }
        }
    }

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
                modifier = Modifier.weight(1f),
            )
            if (!editing) {
                Row(
                    modifier = Modifier
                        .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(100.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            editSscc = scan.sscc.orEmpty()
                            editBatch = scan.batchNo.orEmpty()
                            editGtin = scan.gtin.orEmpty()
                            editBestBefore = scan.bestBefore.orEmpty()
                            editQuantity = scan.quantity.orEmpty()
                            editArticle = scan.articleNo.orEmpty()
                            editing = true
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.Pencil,
                        contentDescription = "Edit",
                        tint = Tokens.TextPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Edit",
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Full label photo when stored; falls back to the list thumbnail.
            val bmp = remember(scan.id) {
                scan.labelPhotoPath?.let { ImageUtils.decodeFileScaled(it, maxWidth = 1600) }
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
                    FullscreenPhotoOverlay(bitmap = bmp, onDismiss = { viewingLabel = false })
                }
            }

            if (editing) {
                val ssccStatus = SsccValidator.status(editSscc)
                val ssccOk = editSscc.isEmpty() || ssccStatus == SsccValidator.Status.OK
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("SSCC", small = true)
                    EditField(mono = true, 
                        value = editSscc,
                        onChange = { editSscc = it.filter { c -> c.isDigit() || c == ' ' } },
                        isError = !ssccOk,
                    )
                    if (!ssccOk) {
                        Text(
                            text = when (ssccStatus) {
                                SsccValidator.Status.WRONG_LENGTH -> "SSCC should be exactly 18 digits."
                                else -> "Check digit doesn't match — one digit may be wrong."
                            },
                            color = Tokens.DangerText,
                            fontFamily = PlexSans,
                            fontSize = 10.5.sp,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("Batch no.", small = true)
                    EditField(mono = true, value = editBatch, onChange = { editBatch = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("GTIN/EAN", small = true)
                        EditField(mono = true, value = editGtin, onChange = { editGtin = it.filter(Char::isDigit) })
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("Best before", small = true)
                        EditField(mono = true, value = editBestBefore, onChange = { editBestBefore = it })
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("Quantity", small = true)
                        EditField(mono = true, value = editQuantity, onChange = { editQuantity = it.filter(Char::isDigit) })
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("Article no.", small = true)
                    EditField(mono = true, value = editArticle, onChange = { editArticle = it })
                }
                Text(
                    text = "Every change is recorded in the edit history below — the original values always stay on file.",
                    color = Tokens.ink(0.45f),
                    fontFamily = PlexSans,
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                )
            } else {
                DataField(label = "SSCC", value = scan.sscc, big = true) {
                    clipboard.setText(AnnotatedString(it))
                    toast.show("SSCC copied")
                }
                DataField(label = "Batch no.", value = scan.batchNo) {
                    clipboard.setText(AnnotatedString(it))
                    toast.show("Batch number copied")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallField("GTIN/EAN", scan.gtin, Modifier.weight(1f))
                    SmallField("Best before", scan.bestBefore, Modifier.weight(1f))
                    // The as-scanned count stays visible after any quantity edit.
                    SmallField(
                        "Quantity",
                        scan.quantity?.let { q ->
                            val orig = scan.originalQuantity
                            if (orig != null && orig != q) "$q (was $orig)" else q
                        } ?: scan.originalQuantity?.let { "— (was $it)" },
                        Modifier.weight(1f),
                    )
                }
                if (scan.articleNo != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallField("Article no.", scan.articleNo, Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(2f))
                    }
                }
            }

            if (fieldEdits.isNotEmpty()) {
                EditHistorySection(edits = fieldEdits)
            }

            if (!editing) {
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

                NotesSection(
                    notes = notes.map { NoteItem(it.id, it.text, it.createdAt) },
                    onAdd = { viewModel.addScanNote(scan.id, it) },
                    onDelete = { viewModel.deleteScanNote(it.id) },
                    onEdit = { note, text -> viewModel.editScanNote(note.id, text) },
                )

                PhotosSection(
                    photos = photos.map { PhotoItem(it.id, it.filePath) },
                    onAddCamera = capturePhoto,
                    onAddGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    onOpen = { item -> viewingPhoto = photos.firstOrNull { it.id == item.id } },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Tokens.Panel).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (editing) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Tokens.ink(0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { editing = false }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancel",
                        color = Tokens.TextPrimary,
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
                            viewModel.updateScan(
                                scan.id,
                                ScannerRepository.fieldsOf(scan).copy(
                                    sscc = editSscc.filter(Char::isDigit).ifEmpty { null },
                                    batchNo = editBatch.trim().ifEmpty { null },
                                    gtin = editGtin.trim().ifEmpty { null },
                                    bestBefore = editBestBefore.trim().ifEmpty { null },
                                    quantity = editQuantity.trim().ifEmpty { null },
                                    articleNo = editArticle.trim().ifEmpty { null },
                                ),
                            )
                            editing = false
                            toast.show("Changes saved")
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Save changes",
                        color = Tokens.OnAccent,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            } else {
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

    // Full-screen viewer for the scan's extra photos (overlays the screen).
    val vp = viewingPhoto
    if (vp != null) {
        val vpBmp = remember(vp.id) { ImageUtils.decodeFileScaled(vp.filePath, maxWidth = 1600) }
        if (vpBmp != null) {
            FullscreenPhotoOverlay(
                bitmap = vpBmp,
                onDismiss = { viewingPhoto = null },
                onDelete = {
                    viewModel.deleteScanPhoto(vp)
                    viewingPhoto = null
                },
            )
        } else {
            viewingPhoto = null
        }
    }
}
private val FIELD_LABELS = mapOf(
    "sscc" to "SSCC",
    "batchNo" to "Batch no.",
    "gtin" to "GTIN/EAN",
    "bestBefore" to "Best before",
    "quantity" to "Quantity",
    "articleNo" to "Article no.",
)

/** Read-only, append-only record of every manual field change on this scan. */
@Composable
private fun EditHistorySection(edits: List<FieldEditEntity>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.ink(0.04f), RoundedCornerShape(10.dp))
            .border(1.dp, Tokens.Warning.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "EDIT HISTORY",
            color = Tokens.Warning,
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.08.em,
        )
        edits.sortedBy { it.createdAt }.forEach { e ->
            Text(
                text = buildString {
                    append(relativeTime(e.createdAt))
                    append(" · ")
                    append(FIELD_LABELS[e.field] ?: e.field)
                    append(": ")
                    append(e.oldValue ?: "—")
                    append(" → ")
                    append(e.newValue ?: "—")
                },
                color = Tokens.ink(0.65f),
                fontFamily = PlexMono,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
            )
        }
    }
}
