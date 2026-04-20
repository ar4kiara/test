package com.arakiara.remindervoice

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arakiara.remindervoice.data.ReminderEntity
import com.arakiara.remindervoice.model.ReminderDays
import com.arakiara.remindervoice.model.TtsStyle
import com.arakiara.remindervoice.model.TtsVoiceOption
import com.arakiara.remindervoice.model.VoiceMode
import com.arakiara.remindervoice.settings.AppSettings
import com.arakiara.remindervoice.tts.TtsPreviewController
import com.arakiara.remindervoice.ui.ReminderViewModel
import com.arakiara.remindervoice.ui.ReminderVoiceTheme
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReminderVoiceTheme {
                ReminderApp()
            }
        }
    }
}

private fun Context.canScheduleExactAlarmsCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

private fun Context.isBatteryOptimizationIgnoredCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(packageName)
    } else {
        true
    }
}

private fun Context.resolveToneTitle(uriString: String?): String {
    if (uriString.isNullOrBlank()) return "Nada alarm default"
    return runCatching {
        val ringtone = RingtoneManager.getRingtone(this, Uri.parse(uriString))
        ringtone?.getTitle(this)
    }.getOrNull().orEmpty().ifBlank { "Nada terpilih" }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReminderApp(
    viewModel: ReminderViewModel = viewModel(),
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val previewController = remember(context) { TtsPreviewController(context) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingReminder by remember { mutableStateOf<ReminderEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var selectedDays by remember { mutableStateOf(ReminderDays.everyDay) }
    var voiceMode by remember { mutableStateOf(VoiceMode.TTS) }
    var ttsStyle by remember { mutableStateOf(TtsStyle.TEGAS) }
    var selectedTtsVoiceName by remember { mutableStateOf<String?>(null) }
    var previewText by remember { mutableStateOf("Bangun woy | ayo bangun sekarang [jeda 2] jangan tidur lagi") }
    var customSoundUri by remember { mutableStateOf<String?>(null) }
    var notificationSoundUri by remember { mutableStateOf<String?>(null) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }
    var ttsStyleDropdownExpanded by remember { mutableStateOf(false) }
    var ttsVoiceDropdownExpanded by remember { mutableStateOf(false) }
    var ttsVoiceSearch by remember { mutableStateOf("") }
    var forcedVolumePercent by remember { mutableIntStateOf(AppSettings.getForcedAlarmVolumePercent(context)) }
    var repeatDelaySeconds by remember { mutableIntStateOf(AppSettings.getRepeatDelaySeconds(context)) }
    var exactAlarmGranted by remember { mutableStateOf(context.canScheduleExactAlarmsCompat()) }
    var batteryOptimizationIgnored by remember { mutableStateOf(context.isBatteryOptimizationIgnoredCompat()) }

    val availableTtsVoices = previewController.availableVoices
    val filteredTtsVoices = remember(availableTtsVoices, ttsVoiceSearch) {
        val query = ttsVoiceSearch.trim().lowercase(Locale.ROOT)
        if (query.isBlank()) {
            availableTtsVoices
        } else {
            availableTtsVoices.filter { option ->
                option.label.lowercase(Locale.ROOT).contains(query) ||
                    option.name.lowercase(Locale.ROOT).contains(query) ||
                    option.localeTag.lowercase(Locale.ROOT).contains(query)
            }
        }
    }

    fun resetForm() {
        editingReminder = null
        title = ""
        message = ""
        hour = 7
        minute = 0
        selectedDays = ReminderDays.everyDay
        voiceMode = VoiceMode.TTS
        ttsStyle = TtsStyle.TEGAS
        selectedTtsVoiceName = null
        previewText = "Bangun woy | ayo bangun sekarang [jeda 2] jangan tidur lagi"
        customSoundUri = null
        notificationSoundUri = null
        ttsVoiceSearch = ""
    }

    fun fillForm(reminder: ReminderEntity) {
        editingReminder = reminder
        title = reminder.title
        message = reminder.message
        hour = reminder.hour
        minute = reminder.minute
        selectedDays = ReminderDays.decode(reminder.daysOfWeek)
        voiceMode = runCatching { VoiceMode.valueOf(reminder.voiceMode) }.getOrElse { VoiceMode.TTS }
        ttsStyle = runCatching { TtsStyle.valueOf(reminder.ttsStyle) }.getOrElse { TtsStyle.TEGAS }
        selectedTtsVoiceName = reminder.ttsVoiceName
        previewText = reminder.message.ifBlank { reminder.title }
        customSoundUri = reminder.customSoundUri
        notificationSoundUri = reminder.notificationSoundUri
        selectedTab = 0
    }

    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        customSoundUri = uri.toString()
    }

    val pickToneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        notificationSoundUri = uri?.toString()
    }

    DisposableEffect(previewController) {
        onDispose { previewController.shutdown() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmGranted = context.canScheduleExactAlarmsCompat()
                batteryOptimizationIgnored = context.isBatteryOptimizationIgnoredCompat()
                forcedVolumePercent = AppSettings.getForcedAlarmVolumePercent(context)
                repeatDelaySeconds = AppSettings.getRepeatDelaySeconds(context)
                previewController.refreshVoices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(viewModel.statusMessage) {
        viewModel.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
            exactAlarmGranted = context.canScheduleExactAlarmsCompat()
            batteryOptimizationIgnored = context.isBatteryOptimizationIgnoredCompat()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder Voice Pro") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                HeroHeader(reminders.size)
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (editingReminder == null) "Buat" else "Edit") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Tersimpan (${reminders.size})") },
                    )
                }
            }

            if (selectedTab == 0) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = if (editingReminder == null) "Buat pengingat" else "Edit pengingat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            if (!exactAlarmGranted) {
                                HelperCard(
                                    title = "Exact alarm belum aktif",
                                    body = "Android baru butuh izin Alarms & reminders supaya reminder tetap presisi saat aplikasi ditutup.",
                                    actionLabel = "Aktifkan exact alarm",
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            context.startActivity(
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                },
                                            )
                                        }
                                    },
                                )
                            }

                            if (!batteryOptimizationIgnored) {
                                HelperCard(
                                    title = "Optimasi baterai bisa mengganggu",
                                    body = "Di beberapa HP, sistem bisa terlalu agresif saat aplikasi ditutup. Supaya reminder lebih stabil, izinkan aplikasi ini dikecualikan dari optimasi baterai.",
                                    actionLabel = "Izinkan di baterai",
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            context.startActivity(
                                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                },
                                            )
                                        }
                                    },
                                )
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Judul tugas") },
                                placeholder = { Text("Contoh: Bangun pagi") },
                                singleLine = true,
                            )

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Pesan / instruksi") },
                                placeholder = { Text("Contoh: Bangun woy | salat dulu [jeda 2] jangan tidur lagi") },
                                supportingText = { Text("Jeda TTS: pakai enter, tanda |, atau [jeda 2] untuk jeda 2 detik.") },
                            )

                            TimeAndDaySection(
                                hour = hour,
                                minute = minute,
                                selectedDays = selectedDays,
                                onPickTime = {
                                    TimePickerDialog(
                                        context,
                                        { _, pickedHour, pickedMinute ->
                                            hour = pickedHour
                                            minute = pickedMinute
                                        },
                                        hour,
                                        minute,
                                        true,
                                    ).show()
                                },
                                onToggleDay = { dayValue ->
                                    selectedDays = if (dayValue in selectedDays) {
                                        selectedDays - dayValue
                                    } else {
                                        selectedDays + dayValue
                                    }
                                },
                            )

                            ExposedDropdownMenuBox(
                                expanded = voiceDropdownExpanded,
                                onExpandedChange = { voiceDropdownExpanded = !voiceDropdownExpanded },
                            ) {
                                OutlinedTextField(
                                    value = voiceMode.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    label = { Text("Mode suara") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceDropdownExpanded) },
                                )
                                DropdownMenu(
                                    expanded = voiceDropdownExpanded,
                                    onDismissRequest = { voiceDropdownExpanded = false },
                                ) {
                                    VoiceMode.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                voiceMode = option
                                                voiceDropdownExpanded = false
                                            },
                                        )
                                    }
                                }
                            }

                            when (voiceMode) {
                                VoiceMode.TTS -> {
                                    TtsSelectorSection(
                                        ttsStyle = ttsStyle,
                                        onTtsStyleChange = { ttsStyle = it },
                                        ttsStyleDropdownExpanded = ttsStyleDropdownExpanded,
                                        onTtsStyleDropdownChange = { ttsStyleDropdownExpanded = it },
                                        selectedTtsVoiceName = selectedTtsVoiceName,
                                        availableTtsVoices = availableTtsVoices,
                                        filteredTtsVoices = filteredTtsVoices,
                                        ttsVoiceSearch = ttsVoiceSearch,
                                        onTtsVoiceSearchChange = { ttsVoiceSearch = it },
                                        ttsVoiceDropdownExpanded = ttsVoiceDropdownExpanded,
                                        onTtsVoiceDropdownChange = { ttsVoiceDropdownExpanded = it },
                                        onVoiceSelected = { selectedTtsVoiceName = it },
                                    )

                                    TtsPreviewCard(
                                        previewText = previewText,
                                        onPreviewTextChange = { previewText = it },
                                        onTestClick = { previewController.speak(previewText, selectedTtsVoiceName, ttsStyle) },
                                        onStopClick = { previewController.stop() },
                                        onRefreshClick = { previewController.refreshVoices() },
                                        voiceCount = availableTtsVoices.size,
                                        lastError = previewController.lastError,
                                    )
                                }

                                VoiceMode.NOTIFICATION_ONLY -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                                    putExtra(
                                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                                        notificationSoundUri?.let(Uri::parse),
                                                    )
                                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih nada reminder")
                                                }
                                                pickToneLauncher.launch(intent)
                                            },
                                        ) {
                                            Text("Pilih suara bawaan HP")
                                        }
                                        Text(
                                            text = context.resolveToneTitle(notificationSoundUri),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                VoiceMode.CUSTOM_SOUND -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) }) {
                                            Text("Pilih file audio sendiri")
                                        }
                                        Text(
                                            text = customSoundUri ?: "Belum ada file dipilih.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            AlarmBehaviorCard(
                                forcedVolumePercent = forcedVolumePercent,
                                repeatDelaySeconds = repeatDelaySeconds,
                                onVolumeChange = { value ->
                                    forcedVolumePercent = value
                                    AppSettings.setForcedAlarmVolumePercent(context, value)
                                },
                                onRepeatDelayChange = { value ->
                                    repeatDelaySeconds = value
                                    AppSettings.setRepeatDelaySeconds(context, value)
                                },
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val target = editingReminder
                                        if (target == null) {
                                            viewModel.addReminder(
                                                title = title,
                                                message = message,
                                                hour = hour,
                                                minute = minute,
                                                daysOfWeek = selectedDays,
                                                voiceMode = voiceMode,
                                                ttsStyle = ttsStyle,
                                                ttsVoiceName = selectedTtsVoiceName,
                                                customSoundUri = customSoundUri,
                                                notificationSoundUri = notificationSoundUri,
                                            )
                                        } else {
                                            viewModel.updateReminder(
                                                reminder = target,
                                                title = title,
                                                message = message,
                                                hour = hour,
                                                minute = minute,
                                                daysOfWeek = selectedDays,
                                                voiceMode = voiceMode,
                                                ttsStyle = ttsStyle,
                                                ttsVoiceName = selectedTtsVoiceName,
                                                customSoundUri = customSoundUri,
                                                notificationSoundUri = notificationSoundUri,
                                            )
                                        }
                                        resetForm()
                                    },
                                ) {
                                    Text(if (editingReminder == null) "Simpan reminder" else "Update reminder")
                                }
                                if (editingReminder != null) {
                                    OutlinedButton(
                                        modifier = Modifier.weight(0.6f),
                                        onClick = { resetForm() },
                                    ) {
                                        Text("Batal")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Reminder tersimpan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (reminders.isEmpty()) {
                    item {
                        Card {
                            Box(Modifier.padding(16.dp)) {
                                Text("Belum ada reminder.")
                            }
                        }
                    }
                } else {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            availableTtsVoices = availableTtsVoices,
                            onEdit = { fillForm(reminder) },
                            onDelete = { viewModel.deleteReminder(reminder) },
                            onToggle = { viewModel.toggleReminder(reminder, it) },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeAndDaySection(
    hour: Int,
    minute: Int,
    selectedDays: Set<Int>,
    onPickTime: () -> Unit,
    onToggleDay: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Jam reminder",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onPickTime) {
                Text("Pilih jam • ${"%02d".format(hour)}:${"%02d".format(minute)}")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Hari aktif",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReminderDays.orderedDays.forEach { day ->
                    val isSelected = day.calendarValue in selectedDays
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleDay(day.calendarValue) },
                        label = { Text(day.shortLabel) },
                    )
                }
            }
            Text(
                text = ReminderDays.describe(selectedDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TtsSelectorSection(
    ttsStyle: TtsStyle,
    onTtsStyleChange: (TtsStyle) -> Unit,
    ttsStyleDropdownExpanded: Boolean,
    onTtsStyleDropdownChange: (Boolean) -> Unit,
    selectedTtsVoiceName: String?,
    availableTtsVoices: List<TtsVoiceOption>,
    filteredTtsVoices: List<TtsVoiceOption>,
    ttsVoiceSearch: String,
    onTtsVoiceSearchChange: (String) -> Unit,
    ttsVoiceDropdownExpanded: Boolean,
    onTtsVoiceDropdownChange: (Boolean) -> Unit,
    onVoiceSelected: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExposedDropdownMenuBox(
            expanded = ttsStyleDropdownExpanded,
            onExpandedChange = { onTtsStyleDropdownChange(!ttsStyleDropdownExpanded) },
        ) {
            OutlinedTextField(
                value = ttsStyle.label,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Karakter suara") },
                supportingText = { Text("Karakter ini memoles pitch dan kecepatan. Suara asli tetap tergantung mesin TTS di HP.") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ttsStyleDropdownExpanded) },
            )
            DropdownMenu(
                expanded = ttsStyleDropdownExpanded,
                onDismissRequest = { onTtsStyleDropdownChange(false) },
            ) {
                TtsStyle.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onTtsStyleChange(option)
                            onTtsStyleDropdownChange(false)
                        },
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = ttsVoiceDropdownExpanded,
            onExpandedChange = { onTtsVoiceDropdownChange(!ttsVoiceDropdownExpanded) },
        ) {
            OutlinedTextField(
                value = selectedVoiceLabel(availableTtsVoices, selectedTtsVoiceName),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Model suara TTS") },
                supportingText = {
                    Text(
                        if (availableTtsVoices.isEmpty()) {
                            "Belum ada daftar suara yang terbaca. Cek data TTS di HP lalu buka lagi aplikasi."
                        } else {
                            "Terdeteksi ${availableTtsVoices.size} suara. Pakai search/filter di daftar pilihan."
                        },
                    )
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ttsVoiceDropdownExpanded) },
            )
            DropdownMenu(
                expanded = ttsVoiceDropdownExpanded,
                onDismissRequest = { onTtsVoiceDropdownChange(false) },
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ttsVoiceSearch,
                        onValueChange = onTtsVoiceSearchChange,
                        modifier = Modifier.width(320.dp),
                        label = { Text("Cari suara / bahasa / online") },
                        singleLine = true,
                    )
                    DropdownMenuItem(
                        text = { Text("Default mesin TTS") },
                        onClick = {
                            onVoiceSelected(null)
                            onTtsVoiceDropdownChange(false)
                        },
                    )
                    if (filteredTtsVoices.isEmpty()) {
                        Text(
                            text = "Tidak ada suara yang cocok.",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        filteredTtsVoices.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onVoiceSelected(option.name)
                                    onTtsVoiceDropdownChange(false)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmBehaviorCard(
    forcedVolumePercent: Int,
    repeatDelaySeconds: Int,
    onVolumeChange: (Int) -> Unit,
    onRepeatDelayChange: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Perilaku alarm", fontWeight = FontWeight.SemiBold)
            Text(
                "Semua mode akan terus mengulang sampai kamu tekan Stop atau Tunda. Jeda dihitung setelah suara/TTS selesai dibacakan, jadi kalau kalimatnya panjang total terasa lebih lama.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Volume alarm paksa: $forcedVolumePercent%", fontWeight = FontWeight.Medium)
                Slider(
                    value = forcedVolumePercent.toFloat(),
                    onValueChange = { onVolumeChange(it.roundToInt().coerceIn(10, 100)) },
                    valueRange = 10f..100f,
                    steps = 8,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Jeda ulang setelah selesai: $repeatDelaySeconds detik", fontWeight = FontWeight.Medium)
                Slider(
                    value = repeatDelaySeconds.toFloat(),
                    onValueChange = { onRepeatDelayChange(it.roundToInt().coerceIn(1, 15)) },
                    valueRange = 1f..15f,
                    steps = 13,
                )
            }
        }
    }
}

private fun selectedVoiceLabel(
    options: List<TtsVoiceOption>,
    selectedName: String?,
): String {
    return selectedName
        ?.let { name -> options.firstOrNull { it.name == name }?.label }
        ?: "Default mesin TTS"
}

@Composable
private fun HeroHeader(totalReminders: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 8.dp,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF101A4F),
                            Color(0xFF2751E8),
                            Color(0xFF7B3FF5),
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Reminder Voice Pro",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "Alarm tugas harian dengan TTS, jeda bicara, nada HP, file audio, volume alarm, dan pilihan hari.",
                    color = Color(0xFFE7ECFF),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Surface(
                    color = Color.White.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$totalReminders reminder tersimpan",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TtsPreviewCard(
    previewText: String,
    onPreviewTextChange: (String) -> Unit,
    onTestClick: () -> Unit,
    onStopClick: () -> Unit,
    onRefreshClick: () -> Unit,
    voiceCount: Int,
    lastError: String?,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Tes model suara",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (voiceCount > 0) {
                    "$voiceCount suara berhasil dibaca. Ketik teks, pilih model suara, lalu tes."
                } else {
                    "Belum ada daftar suara yang tampil. Coba refresh atau cek data TTS bawaan HP."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Jeda TTS paling aman: pakai enter, tanda |, atau [jeda 2]. Contoh: Bangun woy | sekarang mandi [jeda 2] jangan tidur lagi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            if (!lastError.isNullOrBlank()) {
                Text(
                    text = lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = previewText,
                onValueChange = onPreviewTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Teks untuk tes suara") },
                placeholder = { Text("Contoh: Bangun woy | sekarang mandi [jeda 2] cepat") },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onTestClick,
                ) {
                    Text("Tes suara")
                }
                OutlinedButton(
                    modifier = Modifier.weight(0.8f),
                    onClick = onStopClick,
                ) {
                    Text("Stop tes")
                }
                TextButton(
                    modifier = Modifier.weight(0.8f),
                    onClick = onRefreshClick,
                ) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun HelperCard(
    title: String,
    body: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onClick) {
                Text(actionLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderItem(
    reminder: ReminderEntity,
    availableTtsVoices: List<TtsVoiceOption>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val voiceMode = runCatching { VoiceMode.valueOf(reminder.voiceMode) }.getOrElse { VoiceMode.NOTIFICATION_ONLY }
    val voiceLabel = when (voiceMode) {
        VoiceMode.TTS -> selectedVoiceLabel(availableTtsVoices, reminder.ttsVoiceName)
        else -> voiceMode.label
    }

    ElevatedCard {
        ListItem(
            headlineContent = {
                Text("${"%02d".format(reminder.hour)}:${"%02d".format(reminder.minute)} • ${reminder.title}")
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (reminder.message.isNotBlank()) {
                        Text(reminder.message)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(ReminderDays.describe(ReminderDays.decode(reminder.daysOfWeek))) },
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(voiceMode.label) },
                        )
                        if (voiceMode == VoiceMode.TTS) {
                            AssistChip(
                                onClick = {},
                                label = { Text(voiceLabel) },
                            )
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text(if (reminder.enabled) "Aktif" else "Nonaktif") },
                        )
                    }
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = onToggle,
                    )
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                        )
                    }
                }
            },
        )
    }
}
