@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.fedeveloper95.med

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fedeveloper95.med.elements.MainActivity.CommunityBottomSheet
import com.fedeveloper95.med.elements.MainActivity.EventBottomSheet
import com.fedeveloper95.med.elements.MainActivity.EventPopup
import com.fedeveloper95.med.elements.MainActivity.MainFAB
import com.fedeveloper95.med.elements.MainActivity.MedDataCard
import com.fedeveloper95.med.elements.MainActivity.MedSnackbarHost
import com.fedeveloper95.med.elements.MainActivity.MedicineBottomSheet
import com.fedeveloper95.med.elements.MainActivity.MedicinePopup
import com.fedeveloper95.med.elements.MainActivity.NotesBottomSheet
import com.fedeveloper95.med.elements.MainActivity.WeeklyCalendarPager
import com.fedeveloper95.med.services.AppLockManager
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.services.MedViewModel
import com.fedeveloper95.med.services.Updater
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class ItemType { Event, Medicine }

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
            var useExperimentalNavBar by remember {
                mutableStateOf(
                    prefs.getBoolean("ExperimentalNavBar", false) || prefs.getBoolean("pref_experimental_navbar", false)
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

            var showCommunitySheet by remember { mutableStateOf(false) }

            LaunchedEffect(currentVersionName) {
                val lastVersion = prefs.getString("last_version", null)
                if (lastVersion != currentVersionName) {
                    showCommunitySheet = true
                }
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

                            "ExperimentalNavBar", "pref_experimental_navbar" -> useExperimentalNavBar =
                                sharedPreferences.getBoolean("ExperimentalNavBar", false) || sharedPreferences.getBoolean("pref_experimental_navbar", false)
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
                    useExperimentalNavBar = useExperimentalNavBar,
                    isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                    showCommunitySheet = showCommunitySheet,
                    onCommunitySheetDismiss = {
                        showCommunitySheet = false
                        prefs.edit()
                            .putString("last_version", currentVersionName)
                            .apply()
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            notificationManager.canUseFullScreenIntent()
                        } else {
                            true
                        }
                        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } else {
                            NotificationManagerCompat.from(context).areNotificationsEnabled()
                        }
                        if (hasNotificationPermission && !canUseFullScreen) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        }
                    }
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
        animationSpec = tween(durationMillis = 200),
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
        animationSpec = tween(durationMillis = 200),
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
        animationSpec = tween(durationMillis = 200),
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
    viewModel: MedViewModel,
    weekStart: String,
    sortOrder: String,
    presets: List<String>,
    useBottomSheet: Boolean,
    useExperimentalNavBar: Boolean,
    isExpandedScreen: Boolean,
    showCommunitySheet: Boolean,
    onCommunitySheetDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
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
                if (!useExperimentalNavBar || selectedTab == 0) {
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
            },
            bottomBar = {
                if (useExperimentalNavBar) {
                    ShortNavigationBar {
                        ShortNavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                            label = { Text("Home", fontFamily = GoogleSansFlex) }
                        )
                        ShortNavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
                            label = { Text("Stats", fontFamily = GoogleSansFlex) }
                        )
                        ShortNavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            label = { Text("You", fontFamily = GoogleSansFlex) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
            ) {
                when (selectedTab) {
                    0 -> {
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
                    1 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Stats",
                                style = MaterialTheme.typography.displayMedium,
                                fontFamily = GoogleSansFlex,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    2 -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "You",
                                style = MaterialTheme.typography.displayMedium,
                                fontFamily = GoogleSansFlex,
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
            onDismiss = onCommunitySheetDismiss
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

@Composable
fun SwipeableSquishItem(
    item: MedData,
    shape: androidx.compose.ui.graphics.Shape,
    onDeleteThresholdReached: (() -> Unit) -> Unit,
    onSwipeStart: () -> Unit,
    onSwipeCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
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