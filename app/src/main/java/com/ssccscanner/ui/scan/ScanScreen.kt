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

@Composable
fun ScanScreen(
    documentsViewModel: DocumentsViewModel,
    onReportDamage: (String) -> Unit = {},
    viewModel: ScanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toast = LocalToast.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let(viewModel::processStillImage)
    }
    val pickImage = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val capturePhoto = {
        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.processStillImage(Uri.fromFile(file))
                }

                override fun onError(exception: ImageCaptureException) {
                    toast.show("Couldn't capture photo")
                }
            },
        )
    }

    var pickerOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Tokens.CameraSurface)) {
        when (val flow = state.flow) {
            is ScanFlow.Ready -> {
                if (hasCameraPermission) {
                    CameraPreviewLayer(
                        liveScanning = state.scanMode == ScanMode.BARCODE,
                        onBarcodes = viewModel::onBarcodesDetected,
                        imageCapture = imageCapture,
                    )
                    ReadyOverlay(
                        scanMode = state.scanMode,
                        batchMode = state.batchMode,
                        batchCount = state.batchCount,
                        documentName = state.activeDocumentName,
                        onSetMode = viewModel::setScanMode,
                        onToggleBatch = viewModel::setBatchMode,
                        onUpload = pickImage,
                        onCapture = capturePhoto,
                        onOpenPicker = { pickerOpen = true },
                    )
                } else {
                    PermissionFallback(
                        onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onUpload = pickImage,
                    )
                }
            }

            is ScanFlow.Processing -> ProcessingOverlay()

            is ScanFlow.Result -> ResultView(
                scan = flow.scan,
                batchMode = state.batchMode,
                documentName = state.activeDocumentName,
                onScanAgain = viewModel::scanAgain,
                onSaveEdits = viewModel::saveEdits,
                onReportDamage = onReportDamage,
            )

            is ScanFlow.Error -> ErrorView(message = flow.message, onRetry = viewModel::dismissError)
        }

        if (pickerOpen) {
            val documents by documentsViewModel.documents.collectAsState()
            val activeId by documentsViewModel.activeDocumentId.collectAsState()
            DocumentPickerSheet(
                documents = documents,
                activeDocumentId = activeId,
                onSelect = {
                    documentsViewModel.setActiveDocument(it.id)
                    toast.show("Filing to ${it.name}")
                    pickerOpen = false
                },
                onCreate = {
                    documentsViewModel.createDocument(it)
                    toast.show("Filing to $it")
                    pickerOpen = false
                },
                onDismiss = { pickerOpen = false },
            )
        }
    }
}

// --- Camera ---

@Composable
private fun CameraPreviewLayer(
    liveScanning: Boolean,
    onBarcodes: (List<DetectedBarcode>) -> Unit,
    imageCapture: ImageCapture,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    // Rebinds when liveScanning flips: label mode runs preview + capture only,
    // so a stray barcode can't trigger a scan while the user frames the label.
    DisposableEffect(lifecycleOwner, liveScanning) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            val p = providerFuture.get()
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val useCases = buildList {
                add(preview)
                add(imageCapture)
                if (liveScanning) {
                    add(
                        ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(analysisExecutor, BarcodeAnalyzer(onBarcodes)) },
                    )
                }
            }
            try {
                p.unbindAll()
                p.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases.toTypedArray(),
                )
            } catch (_: Exception) {
                // Camera unavailable (emulator without camera etc.) — leave preview black.
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

// --- Ready-state overlay ---

@Composable
private fun ReadyOverlay(
    scanMode: ScanMode,
    batchMode: Boolean,
    batchCount: Int,
    documentName: String,
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

        // Scanning reticle — barcode-shaped, or taller to frame a whole label
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(
                        width = if (scanMode == ScanMode.BARCODE) 264.dp else 280.dp,
                        height = if (scanMode == ScanMode.BARCODE) 160.dp else 330.dp,
                    )
                    .border(1.5.dp, Tokens.ink(0.4f), RoundedCornerShape(14.dp)),
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (scanMode == ScanMode.BARCODE) {
                "Point the camera at the label's barcodes.\nThey're read automatically."
            } else {
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
        text = "SSCC",
        color = Tokens.ink(0.85f),
        fontFamily = PlexMono,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.12.em,
    )
}

@Composable
private fun PermissionFallback(onRequest: () -> Unit, onUpload: () -> Unit) {
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
            text = "SSCC Scanner reads the barcodes on GS1 shipping labels with your camera — nothing ever leaves the device.",
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

// --- Processing ---

@Composable
private fun ProcessingOverlay() {
    Box(modifier = Modifier.fillMaxSize().background(Tokens.CameraSurface), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularProgressIndicator(color = Tokens.Accent, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
            Text(
                text = "Reading label…",
                color = Tokens.TextBright,
                fontFamily = PlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

// --- Result ---

@Composable
private fun ResultView(
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
            val thumb = remember(scan.timestamp) { ScanViewModel.decodeThumbnail(scan.thumbnail) }
            if (thumb != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = "Label photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(110.dp).background(Tokens.Panel, RoundedCornerShape(10.dp)),
                )
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
                    Spacer(modifier = Modifier.weight(1f))
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

@Composable
private fun EditField(
    value: String,
    onChange: (String) -> Unit,
    mono: Boolean = false,
    numeric: Boolean = false,
    isError: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.ink(0.06f), RoundedCornerShape(9.dp))
            .border(
                1.dp,
                if (isError) Tokens.Danger.copy(alpha = 0.5f) else Tokens.ink(0.16f),
                RoundedCornerShape(9.dp),
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            textStyle = TextStyle(
                color = Tokens.TextBright,
                fontFamily = if (mono) PlexMono else PlexSans,
                fontSize = fontSize,
            ),
            cursorBrush = SolidColor(Tokens.Accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DataField(label: String, value: String?, big: Boolean = false, onCopy: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label)
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
private fun SmallField(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldLabel(label, small = true)
        Text(
            text = value ?: "—",
            color = if (value != null) Tokens.TextBright else Tokens.ink(0.4f),
            fontFamily = PlexMono,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FieldLabel(text: String, small: Boolean = false) {
    Text(
        text = text.uppercase(),
        color = Tokens.ink(0.45f),
        fontFamily = PlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = if (small) 10.sp else 11.sp,
        letterSpacing = 0.08.em,
    )
}

// --- Error ---

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Tokens.Panel).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).background(Tokens.Danger, CircleShape))
            Text(
                text = "Couldn't read label",
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = Tokens.ink(0.6f),
                fontFamily = PlexSans,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
        Box(modifier = Modifier.fillMaxWidth().background(Tokens.Panel).padding(14.dp)) {
            PrimaryButton(text = "Try again", icon = null, onClick = onRetry)
        }
    }
}

// --- Shared buttons ---

@Composable
fun PrimaryButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Tokens.Accent, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Tokens.OnAccent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Tokens.OnAccent,
            fontFamily = PlexSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp,
        )
    }
}

@Composable
fun SecondaryPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Tokens.ink(0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, Tokens.ink(0.18f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Tokens.TextPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = Tokens.TextPrimary,
            fontFamily = PlexSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
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
