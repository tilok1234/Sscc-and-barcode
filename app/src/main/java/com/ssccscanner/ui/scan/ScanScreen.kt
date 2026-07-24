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

    // Batch mode saves without leaving the camera — confirm each one via toast.
    LaunchedEffect(state.batchSavedMessage) {
        state.batchSavedMessage?.let {
            toast.show(it)
            viewModel.consumeBatchSavedMessage()
        }
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
    var batchPromptOpen by remember { mutableStateOf(false) }

    // Zoom for budget cameras that can't focus up close: stand back and zoom in.
    var zoomRatio by remember { mutableStateOf(1f) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    LaunchedEffect(camera, zoomRatio) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    Box(modifier = Modifier.fillMaxSize().background(Tokens.CameraSurface)) {
        when (val flow = state.flow) {
            is ScanFlow.Ready -> {
                if (hasCameraPermission) {
                    CameraPreviewLayer(
                        liveScanning = state.scanMode == ScanMode.BARCODE,
                        onBarcodes = viewModel::onBarcodesDetected,
                        imageCapture = imageCapture,
                        onCamera = { camera = it },
                    )
                    ReadyOverlay(
                        scanMode = state.scanMode,
                        batchMode = state.batchMode,
                        batchCount = state.batchCount,
                        documentName = state.activeDocumentName,
                        zoomRatio = zoomRatio,
                        onCycleZoom = {
                            zoomRatio = when {
                                zoomRatio < 1.5f -> 2f
                                zoomRatio < 2.5f -> 3f
                                else -> 1f
                            }
                        },
                        onSetMode = viewModel::setScanMode,
                        // Turning batch ON first asks for a batch document name.
                        onToggleBatch = { on ->
                            if (on) batchPromptOpen = true else viewModel.setBatchMode(false)
                        },
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

        if (batchPromptOpen) {
            BatchStartPrompt(
                currentDocumentName = state.activeDocumentName,
                onCreate = { name ->
                    documentsViewModel.createDocument(name, isBatch = true)
                    viewModel.setBatchMode(true)
                    toast.show("Batch started — filing to $name")
                    batchPromptOpen = false
                },
                onUseCurrent = {
                    viewModel.setBatchMode(true)
                    toast.show("Batch on — filing to ${state.activeDocumentName}")
                    batchPromptOpen = false
                },
                onDismiss = { batchPromptOpen = false },
            )
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
    onBarcodes: (List<DetectedBarcode>, android.graphics.Bitmap?) -> Unit,
    imageCapture: ImageCapture,
    onCamera: (androidx.camera.core.Camera?) -> Unit = {},
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
                    // ~1080p frames: better decode range AND a usable stored
                    // label photo (the decoding frame is kept as the scan image).
                    val resolution = androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            androidx.camera.core.resolutionselector.ResolutionStrategy(
                                android.util.Size(1920, 1080),
                                androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                            ),
                        )
                        .build()
                    add(
                        ImageAnalysis.Builder()
                            .setResolutionSelector(resolution)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(analysisExecutor, BarcodeAnalyzer(onBarcodes)) },
                    )
                }
            }
            try {
                p.unbindAll()
                val cam = p.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases.toTypedArray(),
                )
                onCamera(cam)
            } catch (_: Exception) {
                // Camera unavailable (emulator without camera etc.) — leave preview black.
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            onCamera(null)
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
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
