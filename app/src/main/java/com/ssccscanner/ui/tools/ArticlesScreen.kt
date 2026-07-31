package com.ssccscanner.ui.tools

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ssccscanner.data.ArticlePhotoEntity
import com.ssccscanner.data.ArticleWithAttachments
import com.ssccscanner.scan.ImageUtils
import com.ssccscanner.ui.AppIcons
import com.ssccscanner.ui.FullscreenPhotoOverlay
import com.ssccscanner.ui.LocalToast
import com.ssccscanner.ui.NoteItem
import com.ssccscanner.ui.NotesSection
import com.ssccscanner.ui.PhotoItem
import com.ssccscanner.ui.PhotosSection
import com.ssccscanner.ui.components.DataField
import com.ssccscanner.ui.components.EditField
import com.ssccscanner.ui.components.FieldLabel
import com.ssccscanner.ui.components.PrimaryButton
import com.ssccscanner.ui.components.SecondaryPill
import com.ssccscanner.ui.components.SmallField
import com.ssccscanner.ui.relativeTime
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens
import java.io.File

/**
 * Article registry tool: save the articles you handle — article number, name,
 * GTIN, label photos, notes — so a number scribbled on a pallet can be looked
 * up on the spot.
 */
@Composable
fun ArticlesScreen(viewModel: ArticlesViewModel, onBack: () -> Unit) {
    val articles by viewModel.articles.collectAsState()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }

    val open = articles.firstOrNull { it.article.id == openId }
    when {
        creating -> ArticleEditor(
            title = "New article",
            initialArticleNo = "",
            initialName = "",
            initialGtin = "",
            onCancel = { creating = false },
            onSave = { no, name, gtin ->
                viewModel.add(no, name, gtin) { newId -> openId = newId }
                creating = false
            },
        )
        open != null -> ArticleDetail(
            viewModel = viewModel,
            entry = open,
            onBack = { openId = null },
        )
        else -> ArticleList(
            articles = articles,
            onBack = onBack,
            onOpen = { openId = it.article.id },
            onNew = { creating = true },
        )
    }
}

// --- List ---

@Composable
private fun ArticleList(
    articles: List<ArticleWithAttachments>,
    onBack: () -> Unit,
    onOpen: (ArticleWithAttachments) -> Unit,
    onNew: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = if (query.isBlank()) {
        articles
    } else {
        val q = query.trim()
        articles.filter { entry ->
            entry.article.articleNo.contains(q, ignoreCase = true) ||
                entry.article.name?.contains(q, ignoreCase = true) == true ||
                entry.article.gtin?.contains(q, ignoreCase = true) == true
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Articles",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Text(
                    text = "${articles.size} registered",
                    color = Tokens.ink(0.45f),
                    fontFamily = PlexMono,
                    fontSize = 12.sp,
                )
            }
            SecondaryPill(text = "+ New", icon = null, onClick = onNew)
        }

        // Search — placeholder drawn over the (empty) field so it stays visible.
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            EditField(value = query, onChange = { query = it })
            if (query.isEmpty()) {
                Text(
                    text = "Search article no, name or GTIN…",
                    color = Tokens.ink(0.35f),
                    fontFamily = PlexSans,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 13.dp, top = 12.dp),
                )
            }
        }

        if (articles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = AppIcons.Tag,
                        contentDescription = null,
                        tint = Tokens.ink(0.3f),
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = "No articles yet",
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "Register the articles you handle — number,\nname, label photos — and find them fast.",
                        color = Tokens.ink(0.5f),
                        fontFamily = PlexSans,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(top = 6.dp)) {
                items(filtered, key = { it.article.id }) { entry ->
                    ArticleRow(entry, onOpen)
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(entry: ArticleWithAttachments, onOpen: (ArticleWithAttachments) -> Unit) {
    val article = entry.article
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
        val firstPhoto = entry.photos.minByOrNull { it.createdAt }
        val bmp = remember(firstPhoto?.id) {
            firstPhoto?.let { ImageUtils.decodeFileScaled(it.filePath, maxWidth = 200) }
        }
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).background(Tokens.Panel, RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Tokens.ink(0.06f), RoundedCornerShape(8.dp))
                    .border(1.dp, Tokens.ink(0.14f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Tag,
                    contentDescription = null,
                    tint = Tokens.ink(0.35f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.articleNo,
                color = Tokens.TextBright,
                fontFamily = PlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val photoLabel = when (entry.photos.size) {
                0 -> null
                1 -> "1 photo"
                else -> "${entry.photos.size} photos"
            }
            val subtitle = listOfNotNull(article.name, photoLabel, relativeTime(article.createdAt))
                .joinToString(" · ")
            Text(
                text = subtitle,
                color = Tokens.ink(0.45f),
                fontFamily = PlexSans,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --- Editor (new + edit share this form) ---

@Composable
private fun ArticleEditor(
    title: String,
    initialArticleNo: String,
    initialName: String,
    initialGtin: String,
    onCancel: () -> Unit,
    onSave: (articleNo: String, name: String?, gtin: String?) -> Unit,
) {
    var articleNoDraft by rememberSaveable { mutableStateOf(initialArticleNo) }
    var nameDraft by rememberSaveable { mutableStateOf(initialName) }
    var gtinDraft by rememberSaveable { mutableStateOf(initialGtin) }
    val canSave = articleNoDraft.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(Tokens.Surface).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = AppIcons.Close,
                contentDescription = "Cancel",
                tint = Tokens.TextPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCancel,
                    ),
            )
            Text(
                text = title,
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                FieldLabel("Article no.")
                EditField(value = articleNoDraft, onChange = { articleNoDraft = it }, mono = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                FieldLabel("Name (optional)")
                EditField(value = nameDraft, onChange = { nameDraft = it })
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                FieldLabel("GTIN/EAN (optional)")
                EditField(value = gtinDraft, onChange = { gtinDraft = it }, mono = true, numeric = true)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Tokens.Panel).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SecondaryPill(text = "Cancel", icon = null, onClick = onCancel, modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.weight(1.4f)) {
                if (canSave) {
                    PrimaryButton(text = "Save", icon = AppIcons.Check) {
                        onSave(articleNoDraft.trim(), nameDraft, gtinDraft)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Tokens.Accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(vertical = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Save",
                            color = Tokens.OnAccent,
                            fontFamily = PlexSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                        )
                    }
                }
            }
        }
    }
}

// --- Detail ---

@Composable
private fun ArticleDetail(
    viewModel: ArticlesViewModel,
    entry: ArticleWithAttachments,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val clipboard = LocalClipboardManager.current
    val article = entry.article
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var viewingPhoto by remember { mutableStateOf<ArticlePhotoEntity?>(null) }

    // Camera capture into a FileProvider cache uri, then persisted by the VM.
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCaptureUri
        if (ok && uri != null) {
            viewModel.addPhoto(article.id, uri) { saved ->
                if (!saved) toast.show("Couldn't save photo")
            }
        }
        pendingCaptureUri = null
    }
    val capturePhoto = {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "article_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCaptureUri = uri
        takePicture.launch(uri)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            viewModel.addPhoto(article.id, it) { saved ->
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

    if (editing) {
        ArticleEditor(
            title = "Edit article",
            initialArticleNo = article.articleNo,
            initialName = article.name.orEmpty(),
            initialGtin = article.gtin.orEmpty(),
            onCancel = { editing = false },
            onSave = { no, name, gtin ->
                viewModel.update(article.id, no, name, gtin)
                editing = false
                toast.show("Article updated")
            },
        )
        return
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
                text = "Article",
                color = Tokens.TextPrimary,
                fontFamily = PlexSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = AppIcons.Pencil,
                contentDescription = "Edit",
                tint = Tokens.TextPrimary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { editing = true },
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DataField(label = "Article no.", value = article.articleNo, big = true) { value ->
                clipboard.setText(AnnotatedString(value))
                toast.show("Article no. copied")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallField("Name", article.name, Modifier.weight(1.4f))
                SmallField("GTIN/EAN", article.gtin, Modifier.weight(1f))
            }

            PhotosSection(
                photos = entry.photos.map { PhotoItem(it.id, it.filePath) },
                onAddCamera = capturePhoto,
                onAddGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onOpen = { item -> viewingPhoto = entry.photos.firstOrNull { it.id == item.id } },
                title = "Label photos",
            )

            NotesSection(
                notes = entry.notes.map { NoteItem(it.id, it.text, it.createdAt) },
                onAdd = { viewModel.addNote(article.id, it) },
                onDelete = { viewModel.deleteNote(it.id) },
                onEdit = { note, text -> viewModel.editNote(note.id, text) },
            )
        }

        // Footer: two-tap delete
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Tokens.Panel)
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.DangerBg, RoundedCornerShape(12.dp))
                    .border(1.dp, Tokens.DangerBorder, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (confirmDelete) {
                            viewModel.delete(article.id)
                            toast.show("Article deleted")
                            onBack()
                        } else {
                            confirmDelete = true
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (confirmDelete) "Sure?" else "Delete article",
                    color = Tokens.DangerText,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
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
