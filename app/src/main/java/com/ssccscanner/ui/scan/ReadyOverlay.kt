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

// --- Ready-state overlay ---

@Composable
internal fun ReadyOverlay(
    scanMode: ScanMode,
    batchMode: Boolean,
    batchCount: Int,
    documentName: String,
    zoomRatio: Float,
    onCycleZoom: () -> Unit,
    onSetMode: (ScanMode) -> Unit,
    onToggleBatch: (Boolean) -> Unit,
    onUpload: () -> Unit,
    onCapture: () -> Unit,
    onOpenPicker: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) { Wordmark() }
            // Filing pill, top-center per handoff
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(100.dp))
                    .border(1.dp, Tokens.ink(0.22f), RoundedCornerShape(100.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenPicker,
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Folder,
                    contentDescription = null,
                    tint = Tokens.TextPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Filing to $documentName",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp),
                )
                Icon(
                    imageVector = AppIcons.ChevronDown,
                    contentDescription = null,
                    tint = Tokens.TextPrimary,
                    modifier = Modifier.size(10.dp),
                )
            }
            if (batchMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .background(Tokens.Accent, RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "BATCH · $batchCount",
                        color = Tokens.OnAccent,
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Scanning reticle — barcode-shaped, or taller to frame a whole label.
        // The zoom pill sits on the reticle's corner for one-thumb reach.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(
                        width = if (scanMode == ScanMode.BARCODE) 310.dp else 310.dp,
                        height = if (scanMode == ScanMode.BARCODE) 195.dp else 350.dp,
                    )
                    .border(1.5.dp, Tokens.ink(0.4f), RoundedCornerShape(14.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Tokens.ink(0.3f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onCycleZoom,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "${zoomRatio.toInt()}×",
                        color = if (zoomRatio > 1f) Tokens.Accent else Tokens.TextPrimary,
                        fontFamily = PlexMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = when {
                scanMode == ScanMode.BARCODE && batchMode ->
                    "Batch on — each pallet saves instantly.\nAim the frame at the barcode you want."
                scanMode == ScanMode.BARCODE ->
                    "Aim the frame at the barcode you want.\nOnly codes inside it are read."
                batchMode ->
                    "Batch on — each shutter press saves instantly.\nFrame the whole label first."
                else ->
                    "Frame the whole label, then press the shutter.\nBarcodes and printed text are read together."
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Tokens.ink(0.7f),
            fontFamily = PlexSans,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Mode toggle: instant barcode auto-scan vs shutter-driven full-label read
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(100.dp))
                    .border(1.dp, Tokens.ink(0.22f), RoundedCornerShape(100.dp))
                    .padding(3.dp),
            ) {
                ModeSegment(
                    text = "Barcode",
                    selected = scanMode == ScanMode.BARCODE,
                    onClick = { onSetMode(ScanMode.BARCODE) },
                )
                ModeSegment(
                    text = "Label",
                    selected = scanMode == ScanMode.LABEL,
                    onClick = { onSetMode(ScanMode.LABEL) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryPill(text = "Upload photo", icon = AppIcons.Gallery, onClick = onUpload, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(18.dp))
            // Shutter — photo capture feeds the OCR fallback for damaged barcodes
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(3.dp, Tokens.ink(0.85f), CircleShape)
                    .padding(6.dp)
                    .background(Tokens.Accent, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCapture,
                    ),
            )
            Spacer(modifier = Modifier.width(18.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Switch(
                        checked = batchMode,
                        onCheckedChange = onToggleBatch,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Tokens.Accent,
                            checkedThumbColor = Tokens.OnAccent,
                            uncheckedTrackColor = Tokens.ink(0.18f),
                            uncheckedThumbColor = Tokens.TextPrimary,
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                    Text(
                        text = "Batch",
                        color = Tokens.ink(0.7f),
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(26.dp))
    }
}

@Composable
private fun ModeSegment(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Tokens.Accent else Color.Transparent,
                RoundedCornerShape(100.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Tokens.OnAccent else Tokens.ink(0.7f),
            fontFamily = PlexSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Wordmark() {
    Text(
        text = "DCR",
        color = Tokens.ink(0.85f),
        fontFamily = PlexMono,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.12.em,
    )
}

@Composable
internal fun PermissionFallback(onRequest: () -> Unit, onUpload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Scan a shipping label",
            color = Tokens.TextPrimary,
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "DCR reads the barcodes on GS1 shipping labels with your camera — nothing ever leaves the device.",
            textAlign = TextAlign.Center,
            color = Tokens.ink(0.5f),
            fontFamily = PlexSans,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.widthIn(max = 260.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(modifier = Modifier.widthIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(text = "Allow camera access", icon = AppIcons.Camera, onClick = onRequest)
            SecondaryPill(text = "Upload photo", icon = AppIcons.Gallery, onClick = onUpload, modifier = Modifier.fillMaxWidth())
        }
    }
}
/**
 * Shown when batch mode is switched on: name a fresh batch document (a
 * truckload usually deserves its own doc) or keep filing to the current one.
 */
@Composable
internal fun BatchStartPrompt(
    currentDocumentName: String,
    onCreate: (String) -> Unit,
    onUseCurrent: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Start a batch",
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = "Scans will save instantly, back to back, and the document gets a per-batch summary with discrepancy checks.",
                color = Tokens.ink(0.55f),
                fontFamily = PlexSans,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
                    .border(1.dp, Tokens.ink(0.16f), RoundedCornerShape(9.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp),
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = "e.g. Truck 12 — Tuesday",
                        color = Tokens.ink(0.35f),
                        fontFamily = PlexSans,
                        fontSize = 13.sp,
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Tokens.TextBright, fontFamily = PlexSans, fontSize = 13.sp),
                    cursorBrush = SolidColor(Tokens.Accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val canCreate = name.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.Accent.copy(alpha = if (canCreate) 1f else 0.45f), RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canCreate,
                    ) { onCreate(name.trim()) }
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Create batch document",
                    color = Tokens.OnAccent,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.ink(0.08f), RoundedCornerShape(12.dp))
                    .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onUseCurrent,
                    )
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Keep filing to $currentDocumentName",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
