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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ssccscanner.data.DamagePhotoEntity
import com.ssccscanner.data.DamageReportWithScan
import com.ssccscanner.scan.ImageUtils
import com.ssccscanner.scan.ScanViewModel
import com.ssccscanner.ui.AppIcons
import com.ssccscanner.ui.LocalToast
import com.ssccscanner.ui.relativeTime
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens
import java.io.File

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
                Text(
                    text = if (reports.size == 1) "1 report" else "${reports.size} reports",
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn {
                items(reports, key = { it.report.id }) { entry ->
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
                                    imageVector = AppIcons.Alert,
                                    contentDescription = null,
                                    tint = Tokens.DangerText,
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
                            val commentPreview = entry.report.comment.ifBlank { null }?.let { " · $it" }.orEmpty()
                            Text(
                                text = "$photoLabel · ${relativeTime(entry.report.createdAt)}$commentPreview",
                                color = Tokens.ink(0.45f),
                                fontFamily = PlexSans,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = AppIcons.Alert,
                            contentDescription = null,
                            tint = Tokens.Warning,
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

    var comment by remember(reportId) { mutableStateOf(entry.report.comment) }
    var viewingPhoto by remember { mutableStateOf<DamagePhotoEntity?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

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
                imageVector = AppIcons.Alert,
                contentDescription = null,
                tint = Tokens.Warning,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Label summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.Panel, RoundedCornerShape(10.dp))
                    .border(1.dp, Tokens.ink(0.12f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Pallet label")
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
                    MiniField("GTIN/EAN", entry.scan.gtin, Modifier.weight(1f))
                    MiniField("Best before", entry.scan.bestBefore, Modifier.weight(1f))
                }
            }

            // Comment
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Comment")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
                        .border(1.dp, Tokens.ink(0.16f), RoundedCornerShape(9.dp))
                        .padding(12.dp),
                ) {
                    if (comment.isEmpty()) {
                        Text(
                            text = "e.g. 6 crushed crates on the north corner, leaking cans…",
                            color = Tokens.ink(0.35f),
                            fontFamily = PlexSans,
                            fontSize = 13.sp,
                        )
                    }
                    BasicTextField(
                        value = comment,
                        onValueChange = {
                            comment = it
                            viewModel.setComment(reportId, it)
                        },
                        textStyle = TextStyle(
                            color = Tokens.TextBright,
                            fontFamily = PlexSans,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        ),
                        cursorBrush = SolidColor(Tokens.Accent),
                        modifier = Modifier.fillMaxWidth().height(76.dp),
                    )
                }
            }

            // Photos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Photos of the damage")
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
                            ) {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            }
                            .padding(6.dp),
                    )
                }
                PhotoGrid(
                    photos = entry.photos,
                    onAdd = capturePhoto,
                    onOpen = { viewingPhoto = it },
                )
            }
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
                    ) { shareReport(context, entry, comment) }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Tokens.Void.copy(alpha = 0.96f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { viewingPhoto = null },
            contentAlignment = Alignment.Center,
        ) {
            val bmp = remember(photo.id) { ImageUtils.decodeFileScaled(photo.filePath, maxWidth = 1600) }
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Damage photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(vertical = 60.dp),
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(Tokens.DangerBg, RoundedCornerShape(100.dp))
                        .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(100.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            viewModel.deletePhoto(photo)
                            viewingPhoto = null
                        }
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
                Box(
                    modifier = Modifier
                        .background(Tokens.ink(0.1f), RoundedCornerShape(100.dp))
                        .border(1.dp, Tokens.ink(0.2f), RoundedCornerShape(100.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { viewingPhoto = null }
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
}

@Composable
private fun PhotoGrid(
    photos: List<DamagePhotoEntity>,
    onAdd: () -> Unit,
    onOpen: (DamagePhotoEntity) -> Unit,
) {
    val cells: List<DamagePhotoEntity?> = photos + listOf(null) // trailing null = "add" tile
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
                                    onClick = onAdd,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    contentDescription = "Damage photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
                // Pad short rows so cells keep equal width
                repeat(3 - rowCells.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = Tokens.ink(0.45f),
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.08.em,
    )
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

private fun shareReport(context: Context, entry: DamageReportWithScan, comment: String) {
    val s = entry.scan
    val summary = buildString {
        appendLine("DAMAGE REPORT")
        appendLine("SSCC: ${s.sscc ?: "—"}")
        appendLine("Batch: ${s.batchNo ?: "—"}")
        appendLine("GTIN/EAN: ${s.gtin ?: "—"}")
        appendLine("Best before: ${s.bestBefore ?: "—"}")
        if (comment.isNotBlank()) {
            appendLine()
            appendLine("Comment: $comment")
        }
        append("Photos attached: ${entry.photos.size}")
    }

    val photoUris = ArrayList(
        entry.photos.mapNotNull { photo ->
            val f = File(photo.filePath)
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
