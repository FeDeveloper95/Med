@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)

package com.fedeveloper95.med

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color.parseColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fedeveloper95.med.elements.MainActivity.EventPopup
import com.fedeveloper95.med.elements.MainActivity.MainFAB
import com.fedeveloper95.med.elements.MainActivity.MedSnackbarHost
import com.fedeveloper95.med.elements.MainActivity.MedicinePopup
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.HashMap
import java.util.Locale

const val PREF_THEME = "pref_theme"
const val THEME_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

const val PREF_WEEK_START = "pref_week_start"
const val PREF_PRESETS = "pref_presets"
const val PREF_PRESETS_ORDERED = "pref_presets_ordered"

val DeleteRed = Color(0xFFEF5350)

data class MedItem(
    val id: Long = System.currentTimeMillis(),
    val groupId: Long? = null,
    val type: ItemType,
    val title: String,
    val iconName: String? = null,
    val colorCode: String? = null,
    val frequencyLabel: String? = null,
    val creationDate: LocalDate,
    val creationTime: LocalTime = LocalTime.now(),
    val takenHistory: HashMap<LocalDate, LocalTime> = HashMap(),
    val recurrenceDays: List<DayOfWeek>? = null,
    val endDate: LocalDate? = null
) : Serializable

enum class ItemType { Event, Medicine }

class MedViewModel(application: Application) : AndroidViewModel(application) {
    private val _items = mutableStateListOf<MedItem>()
    val items: List<MedItem> get() = _items

    private val fileName = "med_data.dat"

    var selectedDate by mutableStateOf(LocalDate.now())

    init {
        loadData()
    }

    fun addItem(
        type: ItemType,
        title: String,
        iconName: String? = null,
        colorCode: String? = null,
        times: List<LocalTime>,
        days: List<DayOfWeek>?
    ) {
        val groupId = System.currentTimeMillis()
        val baseDate = selectedDate

        times.forEach { time ->
            val newItem = MedItem(
                id = System.nanoTime(),
                groupId = groupId,
                type = type,
                title = title,
                iconName = iconName,
                colorCode = colorCode,
                frequencyLabel = if (days != null) "Specific Days" else if (times.size > 1) "${times.size}x Daily" else "Daily",
                creationDate = baseDate,
                creationTime = time,
                recurrenceDays = days,
                endDate = null
            )
            _items.add(newItem)
            if (type == ItemType.Medicine) NotificationReceiver.scheduleNotification(getApplication(), newItem)
        }
        saveData()
    }

    fun deleteItem(item: MedItem, deleteDate: LocalDate) {
        val index = _items.indexOfFirst { it.id == item.id }
        if (index == -1) return

        if (!deleteDate.isAfter(item.creationDate)) {
            _items.removeAt(index)
        } else {
            val updatedItem = item.copy(endDate = deleteDate.minusDays(1))
            _items[index] = updatedItem
        }
        saveData()
    }

    fun restoreItem(item: MedItem) {
        val index = _items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            _items[index] = item
        } else {
            _items.add(item)
        }
        saveData()
    }

    fun toggleMedicine(item: MedItem, date: LocalDate) {
        if (item.type != ItemType.Medicine) return
        if (date != LocalDate.now()) return

        val newHistory = HashMap(item.takenHistory)
        if (newHistory.containsKey(date)) newHistory.remove(date) else newHistory[date] = LocalTime.now()

        val index = _items.indexOfFirst { it.id == item.id }
        if (index != -1) _items[index] = item.copy(takenHistory = newHistory)
        saveData()
    }

    fun reloadData() { loadData() }

    private fun saveData() {
        try {
            val context = getApplication<Application>().applicationContext
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { ObjectOutputStream(it).writeObject(ArrayList(_items)) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadData() {
        try {
            val context = getApplication<Application>().applicationContext
            context.openFileInput(fileName).use {
                val list = ObjectInputStream(it).readObject() as ArrayList<MedItem>
                _items.clear()
                _items.addAll(list)
            }
        } catch (e: Exception) { _items.clear() }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
            val viewModel: MedViewModel = viewModel()
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.reloadData() }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) { viewModel.reloadData() }
                }
                val filter = IntentFilter("com.fedeveloper95.med.REFRESH_DATA")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                else context.registerReceiver(receiver, filter)
                onDispose { context.unregisterReceiver(receiver) }
            }

            var currentTheme by remember { mutableIntStateOf(prefs.getInt(PREF_THEME, THEME_SYSTEM)) }
            var currentWeekStart by remember { mutableStateOf(prefs.getString(PREF_WEEK_START, "monday") ?: "monday") }
            var currentPresets by remember { mutableStateOf(loadPresets(prefs)) }

            val currentVersionName = remember { try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0" } catch (e: Exception) { "1.0" } }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                val update = Updater.checkForUpdates(currentVersionName)
                if (update != null) Updater.showUpdateNotification(context, update)
            }

            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    when (key) {
                        PREF_THEME -> currentTheme = sharedPreferences.getInt(PREF_THEME, THEME_SYSTEM)
                        PREF_WEEK_START -> currentWeekStart = sharedPreferences.getString(PREF_WEEK_START, "monday") ?: "monday"
                        PREF_PRESETS, PREF_PRESETS_ORDERED -> currentPresets = loadPresets(sharedPreferences)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            MedTheme(themeOverride = currentTheme) {
                MedApp(viewModel = viewModel, weekStart = currentWeekStart, presets = currentPresets)
            }
        }
    }
}

enum class TooltipPosition { Above, Start }

@Composable
fun rememberCustomTooltipPositionProvider(
    position: TooltipPosition,
    spacing: Int = 8
): PopupPositionProvider {
    val density = LocalDensity.current
    val spacingPx = with(density) { spacing.dp.roundToPx() }

    return remember(position, density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                return when (position) {
                    TooltipPosition.Above -> {
                        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                        val y = anchorBounds.top - popupContentSize.height - spacingPx
                        IntOffset(x, y)
                    }
                    TooltipPosition.Start -> {
                        val x = anchorBounds.left - popupContentSize.width - spacingPx
                        val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                        IntOffset(x, y)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    containerColor: Color,
    contentColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 20 else 50,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(cornerPercent),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}

@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner"
    )

    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(cornerPercent),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        interactionSource = interactionSource
    ) {
        Text(text, fontFamily = GoogleSansFlex, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ExpressiveTextButton(
    onClick: () -> Unit,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner"
    )

    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(cornerPercent),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        interactionSource = interactionSource
    ) {
        Text(text, fontFamily = GoogleSansFlex, style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedApp(viewModel: MedViewModel = viewModel(), weekStart: String, presets: List<String>) {
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var showMedicineDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var preFilledText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var activeSwipingItemId by remember { mutableStateOf<Long?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val menuItems = remember(presets) {
        val defaultItems = listOf(
            Triple(ItemType.Medicine, Icons.Default.MedicalServices, Triple(context.getString(R.string.medicine_label), null as String?, null as String?)),
            Triple(ItemType.Event, Icons.Rounded.Event, Triple(context.getString(R.string.event_label), null, null))
        )
        val presetItems = presets.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 2) {
                val type = if (parts[0] == ItemType.Medicine.name) ItemType.Medicine else ItemType.Event
                val name = parts[1]
                val iconName = parts.getOrNull(2)
                val colorCode = parts.getOrNull(3)
                val icon = if (iconName != null && AVAILABLE_ICONS.containsKey(iconName)) AVAILABLE_ICONS[iconName]!! else if (type == ItemType.Medicine) Icons.Default.MedicalServices else Icons.Rounded.Event
                Triple(type, icon, Triple(name, iconName, colorCode))
            } else null
        }
        defaultItems + presetItems
    }

    BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                MainFAB(
                    fabMenuExpanded = fabMenuExpanded,
                    onExpandedChange = { fabMenuExpanded = it },
                    menuItems = menuItems,
                    onMenuItemClick = { type, name, iconName, colorCode ->
                        if (iconName == null) {
                            preFilledText = ""
                            if (type == ItemType.Medicine) showMedicineDialog = true else showEventDialog = true
                        } else {
                            viewModel.addItem(type, name, iconName, colorCode, listOf(LocalTime.now()), null)
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExpressiveIconButton(onClick = { showDatePicker = true }, icon = Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.choose_date_desc), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            ExpressiveIconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }, icon = Icons.Default.Settings, contentDescription = stringResource(R.string.settings_desc), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val localeForCalendar = if (weekStart == "sunday") Locale.US else Locale.ITALY
                    WeeklyCalendarPager(selectedDate = viewModel.selectedDate, onDateSelected = { viewModel.selectedDate = it }, locale = localeForCalendar)
                    Spacer(modifier = Modifier.height(24.dp))
                    val initialPage = 10000
                    val dayPagerState = rememberPagerState(initialPage = initialPage, pageCount = { 20000 })
                    val today = remember { LocalDate.now() }
                    LaunchedEffect(viewModel.selectedDate) {
                        val daysDiff = ChronoUnit.DAYS.between(today, viewModel.selectedDate).toInt()
                        val targetPage = initialPage + daysDiff
                        if (dayPagerState.currentPage != targetPage) dayPagerState.animateScrollToPage(targetPage)
                    }
                    LaunchedEffect(dayPagerState.currentPage) {
                        val daysDiff = dayPagerState.currentPage - initialPage
                        val newDate = today.plusDays(daysDiff.toLong())
                        if (viewModel.selectedDate != newDate) viewModel.selectedDate = newDate
                    }
                    HorizontalPager(state = dayPagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                        val daysDiff = page - initialPage
                        val pageDate = today.plusDays(daysDiff.toLong())

                        val pageItems = viewModel.items.filter { item ->
                            when (item.type) {
                                ItemType.Event -> item.creationDate == pageDate
                                ItemType.Medicine -> {
                                    val isAfterStart = !pageDate.isBefore(item.creationDate)
                                    val isBeforeEnd = item.endDate == null || !pageDate.isAfter(item.endDate)
                                    val isCorrectDay = item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(pageDate.dayOfWeek)
                                    isAfterStart && isBeforeEnd && isCorrectDay
                                }
                            }
                        }

                        var isRefreshing by remember { mutableStateOf(false) }
                        val pullRefreshState = rememberPullToRefreshState()
                        val onRefresh: () -> Unit = { isRefreshing = true; scope.launch { delay(1000); viewModel.reloadData(); isRefreshing = false } }
                        PullToRefreshBox(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize(), indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullRefreshState, isRefreshing = isRefreshing, modifier = Modifier.align(Alignment.TopCenter)) }) {
                            if (pageItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(stringResource(R.string.no_events_label), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    itemsIndexed(items = pageItems, key = { _, item -> item.id }) { index, item ->
                                        val topRadius = if (index == 0) 28.dp else 4.dp
                                        val bottomRadius = if (index == pageItems.lastIndex) 28.dp else 4.dp
                                        val shape = RoundedCornerShape(topStart = topRadius, topEnd = topRadius, bottomStart = bottomRadius, bottomEnd = bottomRadius)

                                        Box(modifier = Modifier.animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold), fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow), fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow))) {
                                            SwipeableSquishItem(
                                                item = item,
                                                shape = shape,
                                                onDeleteThresholdReached = { resetAnimation ->
                                                    val itemToRestore = item
                                                    viewModel.deleteItem(item, pageDate)

                                                    val deletedMsg = context.getString(R.string.item_deleted, item.title)
                                                    val undoMsg = context.getString(R.string.undo_action)

                                                    scope.launch {
                                                        snackbarHostState.currentSnackbarData?.dismiss()

                                                        val dismissJob = launch {
                                                            delay(2000)
                                                            snackbarHostState.currentSnackbarData?.dismiss()
                                                        }

                                                        val result = snackbarHostState.showSnackbar(
                                                            message = deletedMsg,
                                                            actionLabel = undoMsg,
                                                            withDismissAction = true,
                                                            duration = SnackbarDuration.Indefinite
                                                        )

                                                        dismissJob.cancel()

                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            viewModel.restoreItem(itemToRestore)
                                                            resetAnimation()
                                                        }
                                                    }
                                                },
                                                onSwipeStart = { activeSwipingItemId = item.id },
                                                onSwipeCancel = { activeSwipingItemId = null }
                                            ) {
                                                MedItemCard(item = item, currentViewDate = pageDate, shape = shape, onToggle = { viewModel.toggleMedicine(item, pageDate) })
                                            }
                                        }
                                    }
                                    item { Spacer(modifier = Modifier.height(100.dp)) }
                                }
                            }
                        }
                    }
                }
                if (fabMenuExpanded) Box(modifier = Modifier.fillMaxSize().background(Color.Transparent).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { fabMenuExpanded = false })
            }
        }

        MedSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.selectedDate.toEpochDay() * 24 * 60 * 60 * 1000)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { ExpressiveTextButton(onClick = { datePickerState.selectedDateMillis?.let { millis -> viewModel.selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000)) }; showDatePicker = false }, text = stringResource(R.string.ok_action)) },
            dismissButton = { ExpressiveTextButton(onClick = { showDatePicker = false }, text = stringResource(R.string.cancel_action)) },
            colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(32.dp), tonalElevation = 6.dp
        ) { DatePicker(state = datePickerState) }
    }
    if (showMedicineDialog) {
        MedicinePopup(
            onDismiss = { showMedicineDialog = false },
            onConfirm = { title, iconName, colorCode, times, days ->
                viewModel.addItem(ItemType.Medicine, title, iconName, colorCode, times, days)
                showMedicineDialog = false
            },
            initialText = preFilledText
        )
    }
    if (showEventDialog) {
        EventPopup(
            onDismiss = { showEventDialog = false },
            onConfirm = { title, iconName, colorCode, times, days ->
                viewModel.addItem(ItemType.Event, title, iconName, colorCode, times, days)
                showEventDialog = false
            },
            initialText = preFilledText
        )
    }
}

@Composable
fun TimeSelectorItem(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = GoogleSansFlex, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time.format(DateTimeFormatter.ofPattern("HH:mm")), fontFamily = GoogleSansFlex, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.Edit, contentDescription = "Edit time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun MedItemCard(item: MedItem, currentViewDate: LocalDate, shape: Shape, onToggle: () -> Unit) {
    val isMedicine = item.type == ItemType.Medicine
    val isTakenToday = if (isMedicine) item.takenHistory.containsKey(currentViewDate) else false
    val timestamp = if (isMedicine) item.takenHistory[currentViewDate] else item.creationTime
    val isToday = LocalDate.now() == currentViewDate
    val toggleEnabled = isMedicine && isToday

    val icon = if (item.iconName != null && AVAILABLE_ICONS.containsKey(item.iconName)) AVAILABLE_ICONS[item.iconName]!! else if (isMedicine) Icons.Rounded.MedicalServices else Icons.Rounded.Event
    val customColor = remember(item.colorCode) { if (item.colorCode != null && item.colorCode != "dynamic") try { Color(parseColor(item.colorCode)) } catch (e: Exception) { null } else null }

    val cardContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardContentColor = MaterialTheme.colorScheme.onSurface
    val iconBoxColor = customColor ?: if (isMedicine) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val iconBoxTintColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else if (isMedicine) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(modifier = Modifier.fillMaxWidth(), shape = shape, colors = CardDefaults.cardColors(containerColor = cardContainerColor, contentColor = cardContentColor), elevation = CardDefaults.cardElevation(0.dp)) {
        ListItem(
            headlineContent = {
                Text(item.title, fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
            },
            supportingContent = {
                Column {
                    if (isMedicine && !item.frequencyLabel.isNullOrBlank()) {
                        Text(item.frequencyLabel, fontFamily = GoogleSansFlex, style = MaterialTheme.typography.bodySmall, color = cardContentColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMedicine) {
                            val scheduledTime = item.creationTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            if (isTakenToday && timestamp != null) {
                                val takenTime = timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))
                                Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(12.dp), tint = cardContentColor.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Taken at $takenTime", fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodyMedium, color = cardContentColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            } else {
                                Text("Scheduled for $scheduledTime", fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodyMedium, color = cardContentColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        } else if (timestamp != null) {
                            Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(12.dp), tint = cardContentColor.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(timestamp.format(DateTimeFormatter.ofPattern("HH:mm")), fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodyMedium, color = cardContentColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }
                }
            },
            leadingContent = {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(iconBoxColor), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconBoxTintColor, modifier = Modifier.size(24.dp))
                }
            },
            trailingContent = if (isMedicine) { {
                IconButton(onClick = onToggle, enabled = toggleEnabled) {
                    AnimatedContent(targetState = isTakenToday, label = "checkAnim") { taken ->
                        if (taken) Icon(Icons.Outlined.CheckCircle, stringResource(R.string.taken_desc), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        else Icon(Icons.Outlined.Circle, stringResource(R.string.to_take_desc), tint = if (toggleEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
                    }
                }
            } } else null,
            modifier = Modifier.padding(vertical = 4.dp), colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun SwipeableSquishItem(
    item: MedItem,
    shape: Shape,
    onDeleteThresholdReached: (() -> Unit) -> Unit,
    onSwipeStart: () -> Unit,
    onSwipeCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    var itemWidth by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    val isDark = isSystemInDarkTheme()
    val deleteBackgroundColor = if (isDark) Color(0xFFF2B8B5) else Color(0xFFB3261E)
    val deleteIconColor = if (isDark) Color(0xFF601410) else Color(0xFFFFFFFF)

    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { itemWidth = it.width.toFloat() }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch {
                        val newVal = (offsetX.value + delta).coerceIn(0f, itemWidth)
                        offsetX.snapTo(newVal)
                    }
                },
                onDragStarted = { onSwipeStart() },
                onDragStopped = {
                    val threshold = itemWidth * 0.5f
                    if (offsetX.value > threshold) {
                        scope.launch {
                            offsetX.animateTo(itemWidth, spring(stiffness = Spring.StiffnessMedium))
                            val resetAnim = { scope.launch { offsetX.snapTo(0f) } }
                            onDeleteThresholdReached { resetAnim.invoke(); Unit }
                        }
                    } else {
                        scope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            onSwipeCancel()
                        }
                    }
                }
            ),
        content = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(deleteBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = deleteIconColor
                )
            }
            Box { content() }
        }
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val currentOffsetX = offsetX.value

        val contentPlaceable = measurables[1].measure(constraints)
        val height = contentPlaceable.height

        val redBoxWidth = currentOffsetX.toInt().coerceAtLeast(0)

        val redPlaceable = measurables[0].measure(
            Constraints(
                minWidth = redBoxWidth,
                maxWidth = redBoxWidth,
                minHeight = height,
                maxHeight = height
            )
        )

        layout(totalWidth, height) {
            redPlaceable.place(0, 0)
            contentPlaceable.place(redBoxWidth, 0)
        }
    }
}

@Composable
fun WeeklyCalendarPager(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    locale: Locale
) {
    val pagerState = rememberPagerState(initialPage = 1000, pageCount = { 2000 })
    val today = remember { LocalDate.now() }
    val currentWeekStart = remember(locale, today) { today.with(WeekFields.of(locale).dayOfWeek(), 1L) }

    LaunchedEffect(selectedDate, locale) {
        val weeksDiff = ChronoUnit.WEEKS.between(currentWeekStart, selectedDate.with(WeekFields.of(locale).dayOfWeek(), 1L))
        val targetPage = 1000 + weeksDiff.toInt()
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp), pageSpacing = 16.dp) { page ->
        val weekStart = currentWeekStart.plusWeeks((page - 1000).toLong())
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = weekStart.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (dayOffset in 0..6) {
                    val date = weekStart.plusDays(dayOffset.toLong())
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CalendarDayItem(date = date, isSelected = date == selectedDate, onClick = { onDateSelected(date) })
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayItem(date: LocalDate, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, label = "bgColor")
    val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, label = "contentColor")
    val cornerRadius by animateIntAsState(targetValue = if (isPressed) 12 else 32, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "corner")

    Column(
        modifier = Modifier
            .width(44.dp).height(68.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = date.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, Locale.ENGLISH), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = contentColor)
    }
}

@Composable
fun IconPickerDialog(currentIcon: String, onDismiss: () -> Unit, onIconSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Icon", fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val icons = AVAILABLE_ICONS.toList(); val rows = icons.chunked(4)
                rows.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((name, icon) in rowItems) {
                            val isSelected = currentIcon == name
                            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(containerColor).clickable { onIconSelected(name) }, contentAlignment = Alignment.Center) {
                                Icon(imageVector = icon, contentDescription = name, tint = iconTint, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {}, dismissButton = { ExpressiveTextButton(onClick = onDismiss, text = stringResource(R.string.cancel_action)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(32.dp)
    )
}

@Composable
fun TimePickerDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, dismissButton: @Composable () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp, modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min).background(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { content(); Row(modifier = Modifier.height(40.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) { dismissButton(); confirmButton() } }
        }
    }
}