package com.ssccscanner.ui.tools

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssccscanner.data.AppointmentWithNotes
import com.ssccscanner.ui.AppIcons
import com.ssccscanner.ui.LocalToast
import com.ssccscanner.ui.NoteItem
import com.ssccscanner.ui.NotesSection
import com.ssccscanner.ui.components.EditField
import com.ssccscanner.ui.components.FieldLabel
import com.ssccscanner.ui.components.PrimaryButton
import com.ssccscanner.ui.components.SecondaryPill
import com.ssccscanner.ui.theme.PlexMono
import com.ssccscanner.ui.theme.PlexSans
import com.ssccscanner.ui.theme.Tokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Appointment schedule tool: expected appointments in time order, each with
 * its own timestamped note log. Date/time are typed (validated live) rather
 * than picker-driven — matching the app's field idiom.
 */
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel, onBack: () -> Unit) {
    val appointments by viewModel.appointments.collectAsState()
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }

    val open = appointments.firstOrNull { it.appointment.id == openId }
    when {
        creating -> AppointmentEditor(
            title = "New appointment",
            initialTitle = "",
            initialAt = null,
            initialLocation = "",
            onCancel = { creating = false },
            onSave = { t, at, loc ->
                viewModel.add(t, at, loc)
                creating = false
            },
        )
        open != null -> AppointmentDetail(
            viewModel = viewModel,
            entry = open,
            onBack = { openId = null },
        )
        else -> ScheduleList(
            appointments = appointments,
            onBack = onBack,
            onOpen = { openId = it.appointment.id },
            onNew = { creating = true },
            onToggleDone = { viewModel.setDone(it.appointment.id, !it.appointment.done) },
        )
    }
}

// --- Formatting / parsing ---

private val dayFmt = SimpleDateFormat("EEE d MMM", Locale.US)
private val dayFullFmt = SimpleDateFormat("EEEE d MMMM yyyy", Locale.US)
private val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
private val dateInputFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

/** Strict "yyyy-MM-dd" + "HH:mm" → epoch millis, or null when malformed. */
private fun parseDateTime(date: String, time: String): Long? {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
    return runCatching { fmt.parse("${date.trim()} ${time.trim()}")?.time }.getOrNull()
}

// --- List ---

@Composable
private fun ScheduleList(
    appointments: List<AppointmentWithNotes>,
    onBack: () -> Unit,
    onOpen: (AppointmentWithNotes) -> Unit,
    onNew: () -> Unit,
    onToggleDone: (AppointmentWithNotes) -> Unit,
) {
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
                    text = "Schedule",
                    color = Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Text(
                    text = "${appointments.size} appointments",
                    color = Tokens.ink(0.45f),
                    fontFamily = PlexMono,
                    fontSize = 12.sp,
                )
            }
            SecondaryPill(text = "+ New", icon = null, onClick = onNew)
        }

        if (appointments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = AppIcons.Calendar,
                        contentDescription = null,
                        tint = Tokens.ink(0.3f),
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = "Nothing scheduled",
                        color = Tokens.TextPrimary,
                        fontFamily = PlexSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "Add the appointments you expect,\nthen keep notes on each as they happen.",
                        color = Tokens.ink(0.5f),
                        fontFamily = PlexSans,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // Pending appointments stay on top — even overdue ones — so nothing
            // waiting gets buried; checked-off ones sink below.
            val planned = appointments.filter { !it.appointment.done }
            val done = appointments.filter { it.appointment.done }.sortedByDescending { it.appointment.at }

            LazyColumn {
                if (planned.isNotEmpty()) {
                    item { ListSectionLabel("Planned") }
                    items(planned, key = { it.appointment.id }) { entry ->
                        AppointmentRow(entry, dimmed = false, onOpen = onOpen, onToggleDone = onToggleDone)
                    }
                }
                if (done.isNotEmpty()) {
                    item { ListSectionLabel("Done") }
                    items(done, key = { it.appointment.id }) { entry ->
                        AppointmentRow(entry, dimmed = true, onOpen = onOpen, onToggleDone = onToggleDone)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListSectionLabel(text: String) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        FieldLabel(text, small = true)
    }
}

@Composable
private fun AppointmentRow(
    entry: AppointmentWithNotes,
    dimmed: Boolean,
    onOpen: (AppointmentWithNotes) -> Unit,
    onToggleDone: (AppointmentWithNotes) -> Unit,
) {
    val a = entry.appointment
    val alpha = if (dimmed) 0.55f else 1f
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
        Column(modifier = Modifier.width(74.dp)) {
            Text(
                text = dayFmt.format(Date(a.at)),
                color = Tokens.ink(0.7f * alpha),
                fontFamily = PlexMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
            Text(
                text = timeFmt.format(Date(a.at)),
                color = Tokens.Accent.copy(alpha = alpha),
                fontFamily = PlexMono,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = a.title,
                color = Tokens.TextBright.copy(alpha = alpha),
                fontFamily = PlexSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (a.done) TextDecoration.LineThrough else null,
            )
            val noteCount = entry.notes.size
            val subtitle = listOfNotNull(
                a.location,
                if (noteCount == 1) "1 note" else if (noteCount > 1) "$noteCount notes" else null,
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    color = Tokens.ink(0.45f * alpha),
                    fontFamily = PlexSans,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Done toggle — an empty circle until it's checked off.
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    if (a.done) Tokens.Success.copy(alpha = 0.15f) else Tokens.ink(0.05f),
                    CircleShape,
                )
                .border(
                    1.dp,
                    if (a.done) Tokens.Success.copy(alpha = 0.6f) else Tokens.ink(0.25f),
                    CircleShape,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onToggleDone(entry) },
            contentAlignment = Alignment.Center,
        ) {
            if (a.done) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = "Done",
                    tint = Tokens.Success,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

// --- Editor (new + edit share this form) ---

@Composable
private fun AppointmentEditor(
    title: String,
    initialTitle: String,
    initialAt: Long?,
    initialLocation: String,
    onCancel: () -> Unit,
    onSave: (title: String, at: Long, location: String?) -> Unit,
) {
    var titleDraft by rememberSaveable { mutableStateOf(initialTitle) }
    var dateDraft by rememberSaveable {
        mutableStateOf(dateInputFmt.format(Date(initialAt ?: System.currentTimeMillis())))
    }
    var timeDraft by rememberSaveable {
        mutableStateOf(initialAt?.let { timeFmt.format(Date(it)) } ?: "09:00")
    }
    var locationDraft by rememberSaveable { mutableStateOf(initialLocation) }

    val parsedAt = parseDateTime(dateDraft, timeDraft)
    val canSave = titleDraft.isNotBlank() && parsedAt != null

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
                FieldLabel("Title")
                EditField(value = titleDraft, onChange = { titleDraft = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    FieldLabel("Date (YYYY-MM-DD)")
                    EditField(
                        value = dateDraft,
                        onChange = { dateDraft = it },
                        mono = true,
                        isError = parsedAt == null,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    FieldLabel("Time (HH:MM)")
                    EditField(
                        value = timeDraft,
                        onChange = { timeDraft = it },
                        mono = true,
                        isError = parsedAt == null,
                    )
                }
            }
            if (parsedAt == null) {
                Text(
                    text = "Enter the date as YYYY-MM-DD and the time as HH:MM.",
                    color = Tokens.DangerText,
                    fontFamily = PlexSans,
                    fontSize = 11.sp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                FieldLabel("Location (optional)")
                EditField(value = locationDraft, onChange = { locationDraft = it })
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
                        onSave(titleDraft.trim(), parsedAt!!, locationDraft)
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
private fun AppointmentDetail(
    viewModel: ScheduleViewModel,
    entry: AppointmentWithNotes,
    onBack: () -> Unit,
) {
    val toast = LocalToast.current
    val a = entry.appointment
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            kotlinx.coroutines.delay(2500)
            confirmDelete = false
        }
    }

    if (editing) {
        AppointmentEditor(
            title = "Edit appointment",
            initialTitle = a.title,
            initialAt = a.at,
            initialLocation = a.location.orEmpty(),
            onCancel = { editing = false },
            onSave = { t, at, loc ->
                viewModel.update(a.id, t, at, loc)
                editing = false
                toast.show("Appointment updated")
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
                text = "Appointment",
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Tokens.Panel, RoundedCornerShape(10.dp))
                    .border(1.dp, Tokens.ink(0.12f), RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = a.title,
                    color = Tokens.TextBright,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textDecoration = if (a.done) TextDecoration.LineThrough else null,
                )
                Text(
                    text = "${dayFullFmt.format(Date(a.at))} · ${timeFmt.format(Date(a.at))}",
                    color = Tokens.Accent,
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                if (a.location != null) {
                    Text(
                        text = a.location,
                        color = Tokens.ink(0.6f),
                        fontFamily = PlexSans,
                        fontSize = 12.5.sp,
                    )
                }
            }

            // Done / not done
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (a.done) Tokens.Success.copy(alpha = 0.12f) else Tokens.ink(0.08f),
                        RoundedCornerShape(12.dp),
                    )
                    .border(
                        1.dp,
                        if (a.done) Tokens.Success.copy(alpha = 0.4f) else Tokens.ink(0.18f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        viewModel.setDone(a.id, !a.done)
                        toast.show(if (a.done) "Marked as not done" else "Marked as done")
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = if (a.done) Tokens.Success else Tokens.TextPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (a.done) "Done — tap to reopen" else "Mark as done",
                    color = if (a.done) Tokens.Success else Tokens.TextPrimary,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                )
            }

            NotesSection(
                notes = entry.notes.map { NoteItem(it.id, it.text, it.createdAt) },
                onAdd = { viewModel.addNote(a.id, it) },
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
                            viewModel.delete(a.id)
                            toast.show("Appointment deleted")
                            onBack()
                        } else {
                            confirmDelete = true
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (confirmDelete) "Sure?" else "Delete appointment",
                    color = Tokens.DangerText,
                    fontFamily = PlexSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
