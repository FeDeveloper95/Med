@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)

package com.fedeveloper95.med

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
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
import com.fedeveloper95.med.elements.MainActivity.CommunityBottomSheet
import com.fedeveloper95.med.elements.MainActivity.EventBottomSheet
import com.fedeveloper95.med.elements.MainActivity.EventPopup
import com.fedeveloper95.med.elements.MainActivity.MainFAB
import com.fedeveloper95.med.elements.MainActivity.MedSnackbarHost
import com.fedeveloper95.med.elements.MainActivity.MedicineBottomSheet
import com.fedeveloper95.med.elements.MainActivity.MedicinePopup
import com.fedeveloper95.med.elements.MainActivity.NotesBottomSheet
import com.fedeveloper95.med.services.AppLockManager
import com.fedeveloper95.med.services.DataRepository
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.services.NotificationReceiver
import com.fedeveloper95.med.services.Updater
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

val AVAILABLE_ICONS: Map<String, ImageVector> = mapOf(
    "MedicalServices" to Icons.Rounded.MedicalServices,
    "Event" to Icons.Rounded.Event,
    "FitnessCenter" to Icons.Rounded.FitnessCenter,
    "Restaurant" to Icons.Rounded.Restaurant,
    "Thermometer" to Icons.Rounded.DeviceThermostat,
    "Mindfulness" to Icons.Rounded.SelfImprovement,
    "LocalHospital" to Icons.Rounded.LocalHospital,
    "Favorite" to Icons.Rounded.Favorite,
    "Star" to Icons.Rounded.Star,
    "Bolt" to Icons.Rounded.Bolt,
    "WaterDrop" to Icons.Rounded.WaterDrop,
    "DirectionsRun" to Icons.Rounded.DirectionsRun,
    "Healing" to Icons.Rounded.Healing
)

val AVAILABLE_COLORS = listOf(
    "dynamic",
    "#ffb3b6",
    "#ffb869",
    "#e8c349",
    "#a0d57b",
    "#97cbff",
    "#b6c6ed",
    "#cabeff",
    "#f7adfd"
)

const val PREF_THEME = "pref_theme"
const val THEME_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2

const val PREF_WEEK_START = "pref_week_start"
const val PREF_SORT_ORDER = "pref_sort_order"
const val PREF_PRESETS = "pref_presets"
const val PREF_PRESETS_ORDERED = "pref_presets_ordered"

val DeleteRed = Color(0xFFEF5350)

enum class ItemType { Event, Medicine }

class MedViewModel(application: Application) : AndroidViewModel(application) {
    private val _items = mutableStateListOf<MedData>()
    val items: List<MedData> get() = _items

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
        days: List<DayOfWeek>?,
        notes: String? = null,
        category: String? = null,
        intervalGap: Int? = null
    ) {
        val groupId = System.currentTimeMillis()
        val baseDate = selectedDate
        val context = getApplication<Application>()

        var currentOrder = (_items.maxOfOrNull { it.displayOrder } ?: 0) + 1

        val itemsOnDate = _items.filter { item ->
            when (item.type) {
                ItemType.Event -> item.creationDate == selectedDate
                ItemType.Medicine -> {
                    val isAfterStart = !selectedDate.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !selectedDate.isAfter(item.endDate)
                    val isCorrectDay =
                        item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(
                            selectedDate.dayOfWeek
                        )
                    val isCorrectGap = item.intervalGap == null || ChronoUnit.DAYS.between(
                        item.creationDate,
                        selectedDate
                    ) % item.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedBy { it.displayOrder }

        var inGroup = false
        for (item in itemsOnDate) {
            if (item.iconName == "DIVIDER") {
                inGroup = item.title.isNotBlank()
            }
        }
        if (inGroup) {
            val emptyDivider = MedData(
                id = System.nanoTime(),
                groupId = System.currentTimeMillis(),
                type = ItemType.Event,
                title = "",
                iconName = "DIVIDER",
                colorCode = "dynamic",
                frequencyLabel = "",
                creationDate = selectedDate,
                creationTime = LocalTime.MIN,
                takenHistory = hashMapOf(),
                recurrenceDays = null,
                endDate = null,
                notes = null,
                displayOrder = currentOrder++,
                category = null,
                intervalGap = null
            )
            _items.add(emptyDivider)
        }

        times.forEach { time ->
            val newItem = MedData(
                id = System.nanoTime(),
                groupId = groupId,
                type = type,
                title = title,
                iconName = iconName,
                colorCode = colorCode,
                frequencyLabel = if (intervalGap == 14) context.getString(R.string.frequency_unit_biweek)
                else if (days != null) context.getString(R.string.frequency_specific_days)
                else if (times.size > 1) context.getString(
                    R.string.frequency_daily_multiple,
                    times.size
                )
                else context.getString(R.string.frequency_daily),
                creationDate = baseDate,
                creationTime = time,
                recurrenceDays = days,
                endDate = null,
                notes = notes,
                displayOrder = currentOrder++,
                category = category,
                intervalGap = intervalGap
            )
            _items.add(newItem)
            if (type == ItemType.Medicine) {
                NotificationReceiver.scheduleNotification(getApplication(), newItem)
            }
        }
        saveData()
    }

    fun updateItem(
        originalItem: MedData,
        title: String,
        iconName: String?,
        colorCode: String?,
        times: List<LocalTime>,
        days: List<DayOfWeek>?,
        notes: String?,
        intervalGap: Int?
    ) {
        val index = _items.indexOfFirst { it.id == originalItem.id }
        if (index != -1) {
            val context = getApplication<Application>()
            _items[index] = originalItem.copy(
                title = title,
                iconName = iconName,
                colorCode = colorCode,
                creationTime = times.firstOrNull() ?: originalItem.creationTime,
                recurrenceDays = days,
                notes = notes,
                intervalGap = intervalGap,
                frequencyLabel = if (intervalGap == 14) context.getString(R.string.frequency_unit_biweek)
                else if (days != null) context.getString(R.string.frequency_specific_days)
                else if (times.size > 1) context.getString(
                    R.string.frequency_daily_multiple,
                    times.size
                )
                else context.getString(R.string.frequency_daily)
            )

            if (times.size > 1) {
                for (i in 1 until times.size) {
                    val newItem = _items[index].copy(
                        id = System.nanoTime() + i,
                        creationTime = times[i],
                        takenHistory = HashMap()
                    )
                    _items.add(newItem)
                }
            }
            saveData()
            if (originalItem.type == ItemType.Medicine) {
                NotificationReceiver.scheduleNotification(getApplication(), _items[index])
            }
        }
    }

    fun deleteItem(item: MedData, deleteDate: LocalDate) {
        val index = _items.indexOfFirst { it.id == item.id }
        if (index == -1) return

        if (!deleteDate.isAfter(item.creationDate)) {
            _items.removeAt(index)
        } else {
            val updatedItem = item.copy(endDate = deleteDate.minusDays(1))
            _items[index] = updatedItem
        }

        val itemsOnDate = _items.filter {
            when (it.type) {
                ItemType.Event -> it.creationDate == deleteDate
                ItemType.Medicine -> {
                    val isAfterStart = !deleteDate.isBefore(it.creationDate)
                    val isBeforeEnd = it.endDate == null || !deleteDate.isAfter(it.endDate)
                    val isCorrectDay =
                        it.recurrenceDays.isNullOrEmpty() || it.recurrenceDays.contains(deleteDate.dayOfWeek)
                    val isCorrectGap = it.intervalGap == null || ChronoUnit.DAYS.between(
                        it.creationDate,
                        deleteDate
                    ) % it.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedWith(compareBy({ it.displayOrder }, { it.creationTime }))

        var changed = true
        var currentList = itemsOnDate
        val dividersToRemove = mutableSetOf<Long>()
        while (changed) {
            changed = false
            val toRemove = currentList.filterIndexed { i, current ->
                if (current.iconName == "DIVIDER") {
                    val next = currentList.getOrNull(i + 1)
                    next == null || next.iconName == "DIVIDER"
                } else false
            }
            if (toRemove.isNotEmpty()) {
                dividersToRemove.addAll(toRemove.map { it.id })
                currentList = currentList.filterNot { it in toRemove }
                changed = true
            }
        }

        if (dividersToRemove.isNotEmpty()) {
            _items.removeAll { it.id in dividersToRemove }
        }

        saveData()
    }

    fun restoreItem(item: MedData) {
        val index = _items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            _items[index] = item
        } else {
            _items.add(item)
        }
        saveData()
    }

    fun toggleMedicine(item: MedData, date: LocalDate) {
        if (item.type != ItemType.Medicine) return
        if (date.isAfter(LocalDate.now())) return

        val newHistory = HashMap(item.takenHistory)
        if (newHistory.containsKey(date)) newHistory.remove(date) else newHistory[date] =
            LocalTime.now()

        val index = _items.indexOfFirst { it.id == item.id }
        if (index != -1) _items[index] = item.copy(takenHistory = newHistory)
        saveData()
    }

    fun reloadData() {
        loadData()
    }

    private fun saveData() {
        DataRepository.saveData(getApplication(), _items)
    }

    private fun loadData() {
        val list = DataRepository.loadData(getApplication())
        _items.clear()
        _items.addAll(list)
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppLockManager.init(application)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("med_settings", MODE_PRIVATE) }
            val viewModel: MedViewModel = viewModel()
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer =
                    LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.reloadData() }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        viewModel.reloadData()
                    }
                }
                val filter = IntentFilter("com.fedeveloper95.med.REFRESH_DATA")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(
                    receiver,
                    filter,
                    RECEIVER_NOT_EXPORTED
                )
                else context.registerReceiver(receiver, filter)
                onDispose { context.unregisterReceiver(receiver) }
            }

            var currentTheme by remember {
                mutableIntStateOf(
                    prefs.getInt(
                        PREF_THEME,
                        THEME_SYSTEM
                    )
                )
            }
            var currentWeekStart by remember {
                mutableStateOf(
                    prefs.getString(
                        PREF_WEEK_START,
                        "monday"
                    ) ?: "monday"
                )
            }
            var currentSortOrder by remember {
                mutableStateOf(
                    prefs.getString(
                        PREF_SORT_ORDER,
                        "time"
                    ) ?: "time"
                )
            }
            var currentPresets by remember { mutableStateOf(loadPresets(prefs)) }
            var useBottomSheet by remember {
                mutableStateOf(
                    prefs.getBoolean(
                        "pref_experimental_bottom_sheet",
                        true
                    )
                )
            }

            val currentVersionName = remember {
                try {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
                } catch (e: Exception) {
                    "1.0"
                }
            }
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                val update = Updater.checkForUpdates(currentVersionName)
                if (update != null) Updater.showUpdateNotification(context, update)
            }

            DisposableEffect(prefs) {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                        when (key) {
                            PREF_THEME -> currentTheme =
                                sharedPreferences.getInt(PREF_THEME, THEME_SYSTEM)

                            PREF_WEEK_START -> currentWeekStart =
                                sharedPreferences.getString(PREF_WEEK_START, "monday") ?: "monday"

                            PREF_SORT_ORDER -> currentSortOrder =
                                sharedPreferences.getString(PREF_SORT_ORDER, "time") ?: "time"

                            PREF_PRESETS, PREF_PRESETS_ORDERED -> currentPresets =
                                loadPresets(sharedPreferences)

                            "pref_experimental_bottom_sheet" -> useBottomSheet =
                                sharedPreferences.getBoolean(
                                    "pref_experimental_bottom_sheet",
                                    false
                                )
                        }
                    }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            MedTheme(themeOverride = currentTheme) {
                MedApp(
                    viewModel = viewModel,
                    weekStart = currentWeekStart,
                    sortOrder = currentSortOrder,
                    presets = currentPresets,
                    useBottomSheet = useBottomSheet,
                    isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                )
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
                        val x =
                            anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                        val y = anchorBounds.top - popupContentSize.height - spacingPx
                        IntOffset(x, y)
                    }

                    TooltipPosition.Start -> {
                        val x = anchorBounds.left - popupContentSize.width - spacingPx
                        val y =
                            anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
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
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
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
fun MedApp(
    viewModel: MedViewModel = viewModel(),
    weekStart: String,
    sortOrder: String,
    presets: List<String>,
    useBottomSheet: Boolean,
    isExpandedScreen: Boolean
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }

    var showCommunitySheet by remember {
        mutableStateOf(
            !prefs.getBoolean(
                "pref_community_shown",
                false
            )
        )
    }

    var fabMenuExpanded by remember { mutableStateOf(false) }
    var showMedicineDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MedData?>(null) }
    var preFilledText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var activeSwipingItemId by remember { mutableStateOf<Long?>(null) }
    var noteToShow by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val menuItems = remember(presets, icSick, icMind, icMixture) {
        val defaultItems: List<Triple<ItemType, ImageVector, Triple<String, String?, String?>>> =
            listOf(
                Triple(
                    ItemType.Medicine,
                    Icons.Rounded.MedicalServices,
                    Triple(
                        context.getString(R.string.medicine_label),
                        null as String?,
                        null as String?
                    )
                ),
                Triple(
                    ItemType.Event,
                    Icons.Rounded.Event,
                    Triple(context.getString(R.string.event_label), null, null)
                )
            )
        val presetItems: List<Triple<ItemType, ImageVector, Triple<String, String?, String?>>> =
            presets.mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    val type =
                        if (parts[0] == ItemType.Medicine.name) ItemType.Medicine else ItemType.Event
                    val name = parts[1]
                    val iconName = parts.getOrNull(2)
                    val colorCode = parts.getOrNull(3)

                    val icon: ImageVector = when (iconName) {
                        "MixtureMed" -> icSick
                        "Bed" -> icMind
                        "Mood" -> icMixture
                        else -> if (iconName != null && AVAILABLE_ICONS.containsKey(iconName)) AVAILABLE_ICONS[iconName]!!
                        else if (type == ItemType.Medicine) Icons.Rounded.MedicalServices
                        else Icons.Rounded.Event
                    }

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
                            if (type == ItemType.Medicine) showMedicineDialog =
                                true else showEventDialog = true
                        } else {
                            viewModel.addItem(
                                type,
                                name,
                                iconName,
                                colorCode,
                                listOf(LocalTime.now()),
                                null
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isExpandedScreen) Modifier.padding(horizontal = 64.dp) else Modifier)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExpressiveIconButton(
                                onClick = {
                                    val intent =
                                        Intent(context, EditModeActivity::class.java).apply {
                                            putExtra(
                                                "SELECTED_DATE",
                                                viewModel.selectedDate.toEpochDay()
                                            )
                                        }
                                    context.startActivity(intent)
                                },
                                icon = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.edit_mode_desc),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ExpressiveIconButton(
                                onClick = { showDatePicker = true },
                                icon = Icons.Rounded.CalendarMonth,
                                contentDescription = stringResource(R.string.choose_date_desc),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ExpressiveIconButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            SettingsActivity::class.java
                                        )
                                    )
                                },
                                icon = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings_desc),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val localeForCalendar = if (weekStart == "sunday") Locale.US else Locale.ITALY
                    WeeklyCalendarPager(
                        selectedDate = viewModel.selectedDate,
                        onDateSelected = { viewModel.selectedDate = it },
                        locale = localeForCalendar
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val initialPage = 10000
                    val dayPagerState =
                        rememberPagerState(initialPage = initialPage, pageCount = { 20000 })
                    val today = remember { LocalDate.now() }

                    var isProgrammaticScroll by remember { mutableStateOf(false) }

                    LaunchedEffect(viewModel.selectedDate) {
                        val daysDiff =
                            ChronoUnit.DAYS.between(today, viewModel.selectedDate).toInt()
                        val targetPage = initialPage + daysDiff
                        if (dayPagerState.currentPage != targetPage) {
                            isProgrammaticScroll = true
                            try {
                                dayPagerState.animateScrollToPage(targetPage)
                            } finally {
                                isProgrammaticScroll = false
                            }
                        }
                    }

                    LaunchedEffect(dayPagerState) {
                        snapshotFlow { dayPagerState.currentPage }.collect { page ->
                            if (!isProgrammaticScroll) {
                                val daysDiff = page - initialPage
                                val newDate = today.plusDays(daysDiff.toLong())
                                if (viewModel.selectedDate != newDate) {
                                    viewModel.selectedDate = newDate
                                }
                            }
                        }
                    }

                    HorizontalPager(
                        state = dayPagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        val daysDiff = page - initialPage
                        val pageDate = today.plusDays(daysDiff.toLong())

                        val pageItems = viewModel.items.filter { item ->
                            when (item.type) {
                                ItemType.Event -> item.creationDate == pageDate
                                ItemType.Medicine -> {
                                    val isAfterStart = !pageDate.isBefore(item.creationDate)
                                    val isBeforeEnd =
                                        item.endDate == null || !pageDate.isAfter(item.endDate)
                                    val isCorrectDay =
                                        item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(
                                            pageDate.dayOfWeek
                                        )
                                    val isCorrectGap =
                                        item.intervalGap == null || ChronoUnit.DAYS.between(
                                            item.creationDate,
                                            pageDate
                                        ) % item.intervalGap == 0L
                                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                                }
                            }
                        }.let { list ->
                            if (sortOrder == "time") {
                                list.filter { it.iconName != "DIVIDER" }
                                    .sortedWith(compareBy { it.creationTime })
                            } else {
                                var currentList = list.sortedWith(
                                    compareBy(
                                        { it.displayOrder },
                                        { it.creationTime })
                                )
                                var changed = true
                                while (changed) {
                                    changed = false
                                    val toRemove = currentList.filterIndexed { i, current ->
                                        if (current.iconName == "DIVIDER") {
                                            val next = currentList.getOrNull(i + 1)
                                            next == null || next.iconName == "DIVIDER"
                                        } else false
                                    }
                                    if (toRemove.isNotEmpty()) {
                                        currentList = currentList.filterNot { it in toRemove }
                                        changed = true
                                    }
                                }
                                currentList
                            }
                        }

                        var isRefreshing by remember { mutableStateOf(false) }
                        val pullRefreshState = rememberPullToRefreshState()
                        val onRefresh: () -> Unit = {
                            isRefreshing = true; scope.launch {
                            delay(1000); viewModel.reloadData(); isRefreshing = false
                        }
                        }

                        PullToRefreshBox(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            modifier = Modifier.fillMaxSize(),
                            indicator = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    PullToRefreshDefaults.LoadingIndicator(
                                        state = pullRefreshState,
                                        isRefreshing = isRefreshing
                                    )
                                }
                            }
                        ) {
                            if (pageItems.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Rounded.Event,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            stringResource(R.string.no_events_label),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                if (isExpandedScreen) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 340.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            count = pageItems.size,
                                            key = { pageItems[it].id },
                                            span = { idx ->
                                                if (pageItems[idx].iconName == "DIVIDER") GridItemSpan(
                                                    maxLineSpan
                                                ) else GridItemSpan(1)
                                            }
                                        ) { idx ->
                                            val item = pageItems[idx]
                                            val isDivider = item.iconName == "DIVIDER"

                                            Box(
                                                modifier = Modifier.animateItem(
                                                    placementSpec = spring(
                                                        stiffness = Spring.StiffnessMediumLow,
                                                        visibilityThreshold = IntOffset.VisibilityThreshold
                                                    ),
                                                    fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                            ) {
                                                if (isDivider) {
                                                    if (item.title.isNotBlank()) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    start = 8.dp,
                                                                    top = if (idx == 0) 0.dp else 16.dp,
                                                                    bottom = 16.dp,
                                                                    end = 8.dp
                                                                ),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = item.title,
                                                                style = MaterialTheme.typography.titleMedium.copy(
                                                                    fontFamily = GoogleSansFlex,
                                                                    fontWeight = FontWeight.Normal
                                                                ),
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.height(if (idx == 0) 0.dp else 24.dp))
                                                    }
                                                } else {
                                                    val shape = RoundedCornerShape(24.dp)
                                                    SwipeableSquishItem(
                                                        item = item,
                                                        shape = shape,
                                                        onDeleteThresholdReached = { resetAnimation ->
                                                            val itemToRestore = item
                                                            viewModel.deleteItem(item, pageDate)
                                                            val deletedMsg = context.getString(
                                                                R.string.item_deleted,
                                                                item.title
                                                            )
                                                            val undoMsg =
                                                                context.getString(R.string.undo_action)
                                                            scope.launch {
                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                val dismissJob =
                                                                    launch { delay(2000); snackbarHostState.currentSnackbarData?.dismiss() }
                                                                val result =
                                                                    snackbarHostState.showSnackbar(
                                                                        message = deletedMsg,
                                                                        actionLabel = undoMsg,
                                                                        withDismissAction = true,
                                                                        duration = SnackbarDuration.Indefinite
                                                                    )
                                                                dismissJob.cancel()
                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                    viewModel.restoreItem(
                                                                        itemToRestore
                                                                    )
                                                                    resetAnimation()
                                                                }
                                                            }
                                                        },
                                                        onSwipeStart = {
                                                            activeSwipingItemId = item.id
                                                        },
                                                        onSwipeCancel = {
                                                            activeSwipingItemId = null
                                                        }
                                                    ) {
                                                        MedDataCard(
                                                            item = item,
                                                            currentViewDate = pageDate,
                                                            shape = shape,
                                                            onToggle = {
                                                                viewModel.toggleMedicine(
                                                                    item,
                                                                    pageDate
                                                                )
                                                            },
                                                            onClick = {
                                                                if (!item.notes.isNullOrBlank()) noteToShow =
                                                                    item.notes
                                                            },
                                                            onLongClick = { editingItem = item }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Spacer(
                                                modifier = Modifier.height(
                                                    100.dp
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        itemsIndexed(
                                            items = pageItems,
                                            key = { _, item -> item.id }) { index, item ->

                                            val isDivider = item.iconName == "DIVIDER"
                                            val isFirstInGroup =
                                                index == 0 || pageItems.getOrNull(index - 1)?.iconName == "DIVIDER"
                                            val isLastInGroup =
                                                index == pageItems.lastIndex || pageItems.getOrNull(
                                                    index + 1
                                                )?.iconName == "DIVIDER"

                                            val topRadius = if (isFirstInGroup) 20.dp else 4.dp
                                            val bottomRadius = if (isLastInGroup) 20.dp else 4.dp
                                            val shape = RoundedCornerShape(
                                                topStart = topRadius,
                                                topEnd = topRadius,
                                                bottomStart = bottomRadius,
                                                bottomEnd = bottomRadius
                                            )

                                            Box(
                                                modifier = Modifier.animateItem(
                                                    placementSpec = spring(
                                                        stiffness = Spring.StiffnessMediumLow,
                                                        visibilityThreshold = IntOffset.VisibilityThreshold
                                                    ),
                                                    fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                    fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                            ) {
                                                if (isDivider) {
                                                    if (item.title.isNotBlank()) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(
                                                                    start = 16.dp,
                                                                    top = if (index == 0) 0.dp else 16.dp,
                                                                    bottom = 16.dp,
                                                                    end = 16.dp
                                                                ),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = item.title,
                                                                style = MaterialTheme.typography.titleMedium.copy(
                                                                    fontFamily = GoogleSansFlex,
                                                                    fontWeight = FontWeight.Normal
                                                                ),
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.height(if (index == 0) 0.dp else 24.dp))
                                                    }
                                                } else {
                                                    SwipeableSquishItem(
                                                        item = item,
                                                        shape = shape,
                                                        onDeleteThresholdReached = { resetAnimation ->
                                                            val itemToRestore = item
                                                            viewModel.deleteItem(item, pageDate)

                                                            val deletedMsg = context.getString(
                                                                R.string.item_deleted,
                                                                item.title
                                                            )
                                                            val undoMsg =
                                                                context.getString(R.string.undo_action)

                                                            scope.launch {
                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                val dismissJob =
                                                                    launch { delay(2000); snackbarHostState.currentSnackbarData?.dismiss() }
                                                                val result =
                                                                    snackbarHostState.showSnackbar(
                                                                        message = deletedMsg,
                                                                        actionLabel = undoMsg,
                                                                        withDismissAction = true,
                                                                        duration = SnackbarDuration.Indefinite
                                                                    )
                                                                dismissJob.cancel()
                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                    viewModel.restoreItem(
                                                                        itemToRestore
                                                                    )
                                                                    resetAnimation()
                                                                }
                                                            }
                                                        },
                                                        onSwipeStart = {
                                                            activeSwipingItemId = item.id
                                                        },
                                                        onSwipeCancel = {
                                                            activeSwipingItemId = null
                                                        }
                                                    ) {
                                                        MedDataCard(
                                                            item = item,
                                                            currentViewDate = pageDate,
                                                            shape = shape,
                                                            onToggle = {
                                                                viewModel.toggleMedicine(
                                                                    item,
                                                                    pageDate
                                                                )
                                                            },
                                                            onClick = {
                                                                if (!item.notes.isNullOrBlank()) noteToShow =
                                                                    item.notes
                                                            },
                                                            onLongClick = { editingItem = item }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        item { Spacer(modifier = Modifier.height(100.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (fabMenuExpanded) Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { fabMenuExpanded = false })
        }

        MedSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        )
    }

    if (showCommunitySheet) {
        CommunityBottomSheet(
            onDismiss = {
                showCommunitySheet = false
                prefs.edit().putBoolean("pref_community_shown", true).apply()
            }
        )
    }

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = viewModel.selectedDate.toEpochDay() * 24 * 60 * 60 * 1000)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                ExpressiveTextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.selectedDate =
                            LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    }; showDatePicker = false
                }, text = stringResource(R.string.ok_action))
            },
            dismissButton = {
                ExpressiveTextButton(
                    onClick = { showDatePicker = false },
                    text = stringResource(R.string.cancel_action)
                )
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 6.dp
        ) { DatePicker(state = datePickerState) }
    }

    if (editingItem != null) {
        val itemToEdit = editingItem!!
        val isMed = itemToEdit.type == ItemType.Medicine

        if (useBottomSheet) {
            if (isMed) {
                MedicineBottomSheet(
                    onDismiss = { editingItem = null },
                    onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                        viewModel.updateItem(
                            itemToEdit,
                            title,
                            iconName,
                            colorCode,
                            times,
                            days,
                            notes,
                            intervalGap
                        )
                        editingItem = null
                    },
                    initialItem = itemToEdit
                )
            } else {
                EventBottomSheet(
                    onDismiss = { editingItem = null },
                    onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                        viewModel.updateItem(
                            itemToEdit,
                            title,
                            iconName,
                            colorCode,
                            times,
                            days,
                            notes,
                            intervalGap
                        )
                        editingItem = null
                    },
                    initialItem = itemToEdit
                )
            }
        } else {
            if (isMed) {
                MedicinePopup(
                    onDismiss = { editingItem = null },
                    onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                        viewModel.updateItem(
                            itemToEdit,
                            title,
                            iconName,
                            colorCode,
                            times,
                            days,
                            notes,
                            intervalGap
                        )
                        editingItem = null
                    },
                    initialItem = itemToEdit
                )
            } else {
                EventPopup(
                    onDismiss = { editingItem = null },
                    onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                        viewModel.updateItem(
                            itemToEdit,
                            title,
                            iconName,
                            colorCode,
                            times,
                            days,
                            notes,
                            intervalGap
                        )
                        editingItem = null
                    },
                    initialItem = itemToEdit
                )
            }
        }
    }

    if (showMedicineDialog) {
        if (useBottomSheet) {
            MedicineBottomSheet(
                onDismiss = { showMedicineDialog = false },
                onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                    viewModel.addItem(
                        ItemType.Medicine,
                        title,
                        iconName,
                        colorCode,
                        times,
                        days,
                        notes = notes,
                        intervalGap = intervalGap
                    )
                    showMedicineDialog = false
                },
                initialText = preFilledText
            )
        } else {
            MedicinePopup(
                onDismiss = { showMedicineDialog = false },
                onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                    viewModel.addItem(
                        ItemType.Medicine,
                        title,
                        iconName,
                        colorCode,
                        times,
                        days,
                        notes = notes,
                        intervalGap = intervalGap
                    )
                    showMedicineDialog = false
                },
                initialText = preFilledText
            )
        }
    }

    if (showEventDialog) {
        if (useBottomSheet) {
            EventBottomSheet(
                onDismiss = { showEventDialog = false },
                onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                    viewModel.addItem(
                        ItemType.Event,
                        title,
                        iconName,
                        colorCode,
                        times,
                        days,
                        notes = notes,
                        intervalGap = intervalGap
                    )
                    showEventDialog = false
                },
                initialText = preFilledText
            )
        } else {
            EventPopup(
                onDismiss = { showEventDialog = false },
                onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                    viewModel.addItem(
                        ItemType.Event,
                        title,
                        iconName,
                        colorCode,
                        times,
                        days,
                        notes = notes,
                        intervalGap = intervalGap
                    )
                    showEventDialog = false
                },
                initialText = preFilledText
            )
        }
    }

    if (noteToShow != null) {
        NotesBottomSheet(notes = noteToShow!!, onDismiss = { noteToShow = null })
    }
}

@Composable
fun TimeSelectorItem(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontFamily = GoogleSansFlex,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                time.format(DateTimeFormatter.ofPattern("HH:mm")),
                fontFamily = GoogleSansFlex,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.edit_time_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MedDataCard(
    item: MedData,
    currentViewDate: LocalDate,
    shape: Shape,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "anim_shape"
    )

    val animatedShape = remember(shape, pressProgress) {
        if (shape is RoundedCornerShape) {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val targetPx = with(density) { 20.dp.toPx() }
                    fun lerp(start: Float, stop: Float, fraction: Float) =
                        (1 - fraction) * start + fraction * stop

                    val ts = lerp(shape.topStart.toPx(size, density), targetPx, pressProgress)
                    val te = lerp(shape.topEnd.toPx(size, density), targetPx, pressProgress)
                    val bs = lerp(shape.bottomStart.toPx(size, density), targetPx, pressProgress)
                    val be = lerp(shape.bottomEnd.toPx(size, density), targetPx, pressProgress)

                    return Outline.Rounded(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                0f,
                                0f,
                                size.width,
                                size.height
                            ),
                            topLeft = androidx.compose.ui.geometry.CornerRadius(ts),
                            topRight = androidx.compose.ui.geometry.CornerRadius(te),
                            bottomRight = androidx.compose.ui.geometry.CornerRadius(be),
                            bottomLeft = androidx.compose.ui.geometry.CornerRadius(bs)
                        )
                    )
                }
            }
        } else shape
    }

    val isMedicine = item.type == ItemType.Medicine
    val isTakenToday = if (isMedicine) item.takenHistory.containsKey(currentViewDate) else false
    val timestamp = if (isMedicine) item.takenHistory[currentViewDate] else item.creationTime
    val isToday = LocalDate.now() == currentViewDate

    val toggleEnabled = isMedicine && !currentViewDate.isAfter(LocalDate.now())

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val icon = when (item.iconName) {
        "MixtureMed" -> icSick
        "Bed" -> icMind
        "Mood" -> icMixture
        else -> if (item.iconName != null && AVAILABLE_ICONS.containsKey(item.iconName)) AVAILABLE_ICONS[item.iconName]!!
        else if (isMedicine) Icons.Rounded.MedicalServices
        else Icons.Rounded.Event
    }

    val customColor = remember(item.colorCode) {
        if (item.colorCode != null && item.colorCode != "dynamic") try {
            Color(parseColor(item.colorCode))
        } catch (e: Exception) {
            null
        } else null
    }

    val cardContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardContentColor = MaterialTheme.colorScheme.onSurface
    val iconBoxColor = customColor
        ?: if (isMedicine) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val iconBoxTintColor =
        if (customColor != null) Color.Black.copy(alpha = 0.7f) else if (isMedicine) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(animatedShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = animatedShape,
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    item.title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            },
            supportingContent = {
                Column {
                    if (isMedicine && !item.frequencyLabel.isNullOrBlank()) {
                        Text(
                            item.frequencyLabel,
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.bodySmall,
                            color = cardContentColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMedicine) {
                            val scheduledTime =
                                item.creationTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            if (isTakenToday && timestamp != null) {
                                val takenTime =
                                    timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))
                                val datePart = if (isToday) "" else "${
                                    currentViewDate.format(
                                        DateTimeFormatter.ofPattern("dd/MM")
                                    )
                                } "

                                Icon(
                                    Icons.Rounded.Schedule,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = cardContentColor.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(
                                        R.string.status_taken_format,
                                        datePart,
                                        takenTime
                                    ),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cardContentColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            } else {
                                Text(
                                    stringResource(
                                        R.string.status_scheduled_format,
                                        scheduledTime
                                    ),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cardContentColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        } else if (timestamp != null) {
                            Icon(
                                Icons.Rounded.Schedule,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = cardContentColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cardContentColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBoxColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconBoxTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            trailingContent = if (isMedicine) {
                {
                    RadioButton(
                        selected = isTakenToday,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggle()
                        },
                        enabled = toggleEnabled
                    )
                }
            } else null,
            modifier = Modifier.padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun SwipeableSquishItem(
    item: MedData,
    shape: Shape,
    onDeleteThresholdReached: (() -> Unit) -> Unit,
    onSwipeStart: () -> Unit,
    onSwipeCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    Icons.Rounded.Delete,
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
    val currentWeekStart =
        remember(locale, today) { today.with(WeekFields.of(locale).dayOfWeek(), 1L) }

    LaunchedEffect(selectedDate, locale) {
        val weeksDiff = ChronoUnit.WEEKS.between(
            currentWeekStart,
            selectedDate.with(WeekFields.of(locale).dayOfWeek(), 1L)
        )
        val targetPage = 1000 + weeksDiff.toInt()
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 16.dp
    ) { page ->
        val weekStart = currentWeekStart.plusWeeks((page - 1000).toLong())
        val monthName = when (weekStart.month) {
            java.time.Month.JANUARY -> R.string.month_january
            java.time.Month.FEBRUARY -> R.string.month_february
            java.time.Month.MARCH -> R.string.month_march
            java.time.Month.APRIL -> R.string.month_april
            java.time.Month.MAY -> R.string.month_may
            java.time.Month.JUNE -> R.string.month_june
            java.time.Month.JULY -> R.string.month_july
            java.time.Month.AUGUST -> R.string.month_august
            java.time.Month.SEPTEMBER -> R.string.month_september
            java.time.Month.OCTOBER -> R.string.month_october
            java.time.Month.NOVEMBER -> R.string.month_november
            java.time.Month.DECEMBER -> R.string.month_december
            else -> R.string.unknown
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(monthName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (dayOffset in 0..6) {
                    val date = weekStart.plusDays(dayOffset.toLong())
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CalendarDayItem(
                            date = date,
                            isSelected = date == selectedDate,
                            onClick = { onDateSelected(date) })
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
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )
    val cornerRadius by animateIntAsState(
        targetValue = if (isPressed) 12 else 32,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "corner"
    )

    val isToday = date == LocalDate.now()
    val dayInitial = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> R.string.day_short_mon
        DayOfWeek.TUESDAY -> R.string.day_short_tue
        DayOfWeek.WEDNESDAY -> R.string.day_short_wed
        DayOfWeek.THURSDAY -> R.string.day_short_thu
        DayOfWeek.FRIDAY -> R.string.day_short_fri
        DayOfWeek.SATURDAY -> R.string.day_short_sat
        DayOfWeek.SUNDAY -> R.string.day_short_sun
        else -> R.string.unknown
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .then(
                if (isToday) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(cornerRadius.dp)
                )
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(dayInitial),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content(); Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) { dismissButton(); confirmButton() }
            }
        }
    }
}