package com.ssccscanner.ui.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssccscanner.core.Confidence
import com.ssccscanner.core.ScanFields
import com.ssccscanner.core.ScanSource
import com.ssccscanner.core.SsccValidator
import com.ssccscanner.scan.BarcodeAnalyzer
import com.ssccscanner.scan.DetectedBarcode
import com.ssccscanner.scan.PendingScan
import com.ssccscanner.scan.ScanFlow
import com.ssccscanner.scan.ScanMode
import com.ssccscanner.scan.ScanViewModel
import com.ssccscanner.ui.AppIcons
import com.ssccscanner.ui.DocumentPickerSheet
import com.ssccscanner.ui.DocumentsViewModel
import com.ssccscanner.ui.LocalToast
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens
import java.io.File
import java.util.concurrent.Executors
import com.ssccscanner.ui.components.EditField
import com.ssccscanner.ui.components.DataField
import com.ssccscanner.ui.components.SmallField
import com.ssccscanner.ui.components.FieldLabel
import com.ssccscanner.ui.components.PrimaryButton
import com.ssccscanner.ui.components.SecondaryPill

// --- Result ---

@Composable
internal fun ResultView(
    scan: PendingScan,
    batchMode: Boolean,
    documentName: String,
    onScanAgain: () -> Unit,
    onSaveEdits: (ScanFields) -> Unit,
    onReportDamage: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val toast = LocalToast.current
    val fields = scan.fields

    var editing by remember(scan.timestamp) { mutableStateOf(false) }
    var editSscc by remember(scan.timestamp) { mutableStateOf(fields.sscc.orEmpty()) }
    var editBatch by remember(scan.timestamp) { mutableStateOf(fields.batchNo.orEmpty()) }
    var editGtin by remember(scan.timestamp) { mutableStateOf(fields.gtin.orEmpty()) }
    var editBestBefore by remember(scan.timestamp) { mutableStateOf(fields.bestBefore.orEmpty()) }
    var editQuantity by remember(scan.timestamp) { mutableStateOf(fields.quantity.orEmpty()) }
    var editArticle by remember(scan.timestamp) { mutableStateOf(fields.articleNo.orEmpty()) }

    val statusColor = when (fields.confidence) {
        Confidence.HIGH -> Tokens.Success
        Confidence.MEDIUM -> Tokens.Warning
        Confidence.LOW -> Tokens.Danger
    }

    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Tokens.Panel).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
            Text(
                text = "Saved to $documentName",
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = when (fields.source) {
                    ScanSource.BARCODE -> "BARCODE"
                    ScanSource.OCR -> "OCR"
                    ScanSource.MIXED -> "BARCODE+OCR"
                    ScanSource.MANUAL -> "MANUAL"
                },
                color = Tokens.ink(0.45f),
                fontFamily = PlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 0.08.em,
            )
            if (!editing) {
                Row(
                    modifier = Modifier
                        .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(100.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { editing = true }
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
            // Prefer the stored viewing-quality label photo; tap for full screen.
            val labelBmp = remember(scan.timestamp) {
                ScanViewModel.decodeThumbnail(scan.labelPhoto) ?: ScanViewModel.decodeThumbnail(scan.thumbnail)
            }
            var viewingLabel by remember(scan.timestamp) { mutableStateOf(false) }
            if (labelBmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = labelBmp.asImageBitmap(),
                    contentDescription = "Label photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Tokens.Panel, RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { viewingLabel = true },
                )
                if (viewingLabel) {
                    com.ssccscanner.ui.FullscreenPhotoOverlay(bitmap = labelBmp, onDismiss = { viewingLabel = false })
                }
            }

            if (!editing && fields.confidence == Confidence.LOW) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Tokens.DangerBg, RoundedCornerShape(9.dp))
                        .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(9.dp))
                        .padding(10.dp),
                ) {
                    Text(
                        text = "Low confidence — double-check against the printed label.",
                        color = Tokens.DangerText,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }

            if (editing) {
                // --- Edit mode: live-validated inputs ---
                val ssccStatus = SsccValidator.status(editSscc)
                val ssccOk = editSscc.isEmpty() || ssccStatus == SsccValidator.Status.OK
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FieldLabel("SSCC")
                    EditField(
                        value = editSscc,
                        onChange = { editSscc = it.filter { c -> c.isDigit() || c == ' ' } },
                        mono = true,
                        numeric = true,
                        isError = !ssccOk,
                        fontSize = 16.sp,
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
                    FieldLabel("Batch no.")
                    EditField(value = editBatch, onChange = { editBatch = it }, mono = true, fontSize = 15.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("GTIN/EAN", small = true)
                        EditField(value = editGtin, onChange = { editGtin = it.filter(Char::isDigit) }, mono = true, numeric = true, fontSize = 13.sp)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("Best before", small = true)
                        EditField(value = editBestBefore, onChange = { editBestBefore = it }, mono = true, fontSize = 13.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("Quantity", small = true)
                        EditField(value = editQuantity, onChange = { editQuantity = it.filter(Char::isDigit) }, mono = true, numeric = true, fontSize = 13.sp)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("Article no.", small = true)
                        EditField(value = editArticle, onChange = { editArticle = it }, mono = true, fontSize = 13.sp)
                    }
                }
            } else {
                DataField(label = "SSCC", value = fields.sscc, big = true, onCopy = { text ->
                    clipboard.setText(AnnotatedString(text))
                    toast.show("SSCC copied")
                })

                DataField(label = "Batch no.", value = fields.batchNo, onCopy = { text ->
                    clipboard.setText(AnnotatedString(text))
                    toast.show("Batch number copied")
                })

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallField(label = "GTIN/EAN", value = fields.gtin, modifier = Modifier.weight(1f))
                    SmallField(label = "Best before", value = fields.bestBefore, modifier = Modifier.weight(1f))
                    SmallField(label = "Quantity", value = fields.quantity, modifier = Modifier.weight(1f))
                }
                if (fields.articleNo != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SmallField(label = "Article no.", value = fields.articleNo, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.weight(2f))
                    }
                }

                // Damage flow: flags this pallet and jumps to its damage report
                // where photos of the damaged goods and a comment can be added.
                val scanId = scan.scanId
                if (scanId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Tokens.DangerBg, RoundedCornerShape(12.dp))
                            .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onReportDamage(scanId) }
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
            }
        }

        // Footer
        Row(
            modifier = Modifier.fillMaxWidth().background(Tokens.Panel).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (editing) {
                SecondaryPill(
                    text = "Cancel",
                    icon = null,
                    onClick = {
                        editing = false
                        editSscc = fields.sscc.orEmpty()
                        editBatch = fields.batchNo.orEmpty()
                        editGtin = fields.gtin.orEmpty()
                        editBestBefore = fields.bestBefore.orEmpty()
                        editQuantity = fields.quantity.orEmpty()
                    },
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.weight(1f)) {
                    PrimaryButton(
                        text = "Save changes",
                        icon = null,
                        onClick = {
                            onSaveEdits(
                                fields.copy(
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
                        },
                    )
                }
            } else {
                SecondaryPill(
                    text = "Share",
                    icon = AppIcons.Share,
                    onClick = { shareScan(context, scan) },
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.weight(if (batchMode) 1.4f else 1f)) {
                    PrimaryButton(
                        text = if (batchMode) "Next scan →" else "Scan another",
                        icon = null,
                        onClick = onScanAgain,
                    )
                }
            }
        }
    }
}
// --- Share ---

private fun shareScan(context: android.content.Context, scan: PendingScan) {
    val f = scan.fields
    val summary = buildString {
        appendLine("SSCC: ${f.sscc ?: "—"}")
        appendLine("Batch: ${f.batchNo ?: "—"}")
        appendLine("GTIN/EAN: ${f.gtin ?: "—"}")
        appendLine("Best before: ${f.bestBefore ?: "—"}")
        append("Quantity: ${f.quantity ?: "—"}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(intent, "Share scan"))
}
