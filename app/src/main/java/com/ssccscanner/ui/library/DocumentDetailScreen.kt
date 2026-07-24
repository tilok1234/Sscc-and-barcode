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

// --- Document detail ---

@Composable
internal fun DocumentDetail(
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = document.name,
                            color = Tokens.TextPrimary,
                            fontFamily = PlexSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (document.isBatch) BatchChip()
                    }
                    Text(
                        text = if (document.scanCount == 1) "1 scan" else "${document.scanCount} scans",
                        color = Tokens.ink(0.45f),
                        fontFamily = PlexMono,
                        fontSize = 11.sp,
                    )
                }
                if (!document.isBatch) {
                    // Opt-in batch summary for normal documents
                    Text(
                        text = "Σ",
                        color = if (document.showSummary) Tokens.OnAccent else Tokens.ink(0.6f),
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .background(
                                if (document.showSummary) Tokens.Accent else Tokens.ink(0.06f),
                                RoundedCornerShape(100.dp),
                            )
                            .border(
                                1.dp,
                                if (document.showSummary) Tokens.Accent else Tokens.ink(0.16f),
                                RoundedCornerShape(100.dp),
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { viewModel.setDocumentSummary(document.id, !document.showSummary) }
                            .padding(horizontal = 9.dp, vertical = 4.dp),
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

        if ((document.isBatch || document.showSummary) && scans.isNotEmpty()) {
            BatchSummaryCard(scans)
            Spacer(modifier = Modifier.size(10.dp))
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
// --- Batch summary ---

/**
 * Per-batch overview of a document's pallets with discrepancy detection:
 * mixed best-before dates inside one batch, expired dates, missing batch
 * numbers. Always on for batch documents, opt-in for normal ones.
 */
@Composable
private fun BatchSummaryCard(scans: List<ScanEntity>) {
    val summary = remember(scans) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        BatchAnalyzer.analyze(
            scans.map {
                BatchAnalyzer.ScanInfo(
                    batchNo = it.batchNo,
                    bestBefore = it.bestBefore,
                    quantity = it.quantity,
                )
            },
            todayIso = today,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Tokens.Panel, RoundedCornerShape(10.dp))
            .border(1.dp, Tokens.ink(0.12f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BATCH SUMMARY",
                color = Tokens.ink(0.45f),
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.08.em,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${summary.batchCount} batches · ${scans.size} pallets",
                color = Tokens.ink(0.5f),
                fontFamily = PlexMono,
                fontSize = 10.5.sp,
            )
        }

        summary.discrepancies.forEach { d ->
            val (bg, borderC, textC) = if (d.severity == BatchAnalyzer.Severity.ERROR) {
                Triple(Tokens.DangerBg, Tokens.DangerBorder, Tokens.DangerText)
            } else {
                Triple(Tokens.Warning.copy(alpha = 0.1f), Tokens.Warning.copy(alpha = 0.35f), Tokens.Warning)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg, RoundedCornerShape(8.dp))
                    .border(1.dp, borderC, RoundedCornerShape(8.dp))
                    .padding(8.dp),
            ) {
                Text(
                    text = d.message,
                    color = textC,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        summary.groups.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = g.batchNo?.let { "Batch $it" } ?: "No batch no.",
                    color = if (g.batchNo != null) Tokens.TextBright else Tokens.ink(0.5f),
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(if (g.palletCount == 1) "1 pallet" else "${g.palletCount} pallets")
                        g.totalQuantity?.let { append(" · $it units") }
                    },
                    color = Tokens.ink(0.6f),
                    fontFamily = PlexSans,
                    fontSize = 11.sp,
                )
                Text(
                    text = when (g.bestBefores.size) {
                        0 -> "BB —"
                        1 -> "BB ${g.bestBefores.first()}"
                        else -> "BB ×${g.bestBefores.size}!"
                    },
                    color = if (g.bestBefores.size > 1) Tokens.Danger else Tokens.ink(0.6f),
                    fontFamily = PlexMono,
                    fontSize = 10.5.sp,
                )
            }
        }
    }
}
