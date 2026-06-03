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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cake
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
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.res.painterResource
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
import com.fedeveloper95.med.elements.NotificationsSettingsActivity.FullscreenNotifsHandler
import com.fedeveloper95.med.services.AppLockManager
import com.fedeveloper95.med.services.HandoffHelper
import com.fedeveloper95.med.services.MedApp
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.services.MedViewModel
import com.fedeveloper95.med.services.Updater
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class ItemType { Event, Medicine, Illness }

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
        HandoffHelper.init(application)
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

            var currentTheme by remember { mutableIntStateOf(prefs.getInt(PREF_THEME, THEME_SYSTEM)) }
            var currentWeekStart by remember { mutableStateOf(prefs.getString(PREF_WEEK_START, "monday") ?: "monday") }
            var currentSortOrder by remember { mutableStateOf(prefs.getString(PREF_SORT_ORDER, "time") ?: "time") }
            var currentPresets by remember { mutableStateOf(loadPresets(prefs)) }
            var useBottomSheet by remember { mutableStateOf(prefs.getBoolean("pref_experimental_bottom_sheet", true)) }
            var currentDob by remember { mutableStateOf(prefs.getString("profile_dob", "") ?: "") }

            val currentVersionName = remember {
                try {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
                } catch (e: Exception) {
                    "1.0"
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
                            PREF_THEME -> currentTheme = sharedPreferences.getInt(PREF_THEME, THEME_SYSTEM)
                            PREF_WEEK_START -> currentWeekStart = sharedPreferences.getString(PREF_WEEK_START, "monday") ?: "monday"
                            PREF_SORT_ORDER -> currentSortOrder = sharedPreferences.getString(PREF_SORT_ORDER, "time") ?: "time"
                            PREF_PRESETS, PREF_PRESETS_ORDERED -> currentPresets = loadPresets(sharedPreferences)
                            "pref_experimental_bottom_sheet" -> useBottomSheet = sharedPreferences.getBoolean("pref_experimental_bottom_sheet", true)
                            "profile_dob" -> currentDob = sharedPreferences.getString("profile_dob", "") ?: ""
                        }
                    }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            MedTheme(themeOverride = currentTheme) {
                FullscreenNotifsHandler()
                MedApp(
                    viewModel = viewModel,
                    weekStart = currentWeekStart,
                    sortOrder = currentSortOrder,
                    presets = currentPresets,
                    useBottomSheet = useBottomSheet,
                    isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
                    showCommunitySheet = showCommunitySheet,
                    userDob = currentDob,
                    onCommunitySheetDismiss = {
                        showCommunitySheet = false
                        prefs.edit()
                            .putString("last_version", currentVersionName)
                            .apply()
                        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
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

@Composable
fun BirthdayCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_twelve_sided_cookie),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                )
                Icon(
                    imageVector = Icons.Rounded.Cake,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.happy_birthday_msg),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun IllnessCard(
    item: MedData,
    isConfirmed: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onLongClick: () -> Unit
) {
    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val icon = when (item.iconName) {
        "MixtureMed" -> icSick
        "Bed" -> icMind
        "Mood" -> icMixture
        else -> if (item.iconName != null && AVAILABLE_ICONS.containsKey(item.iconName)) AVAILABLE_ICONS[item.iconName]!!
        else icMind
    }

    val customColor = remember(item.colorCode) {
        if (item.colorCode != null && item.colorCode != "dynamic") try {
            Color(android.graphics.Color.parseColor(item.colorCode))
        } catch (e: Exception) {
            null
        } else null
    }

    val iconBoxColor = customColor ?: MaterialTheme.colorScheme.primaryContainer
    val iconBoxTintColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(iconBoxColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconBoxTintColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isConfirmed) item.title else stringResource(R.string.illness_question, item.title),
                    fontFamily = GoogleSansFlex,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            if (!isConfirmed) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { onNo() },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.no_action), fontFamily = GoogleSansFlex)
                    }

                    ToggleButton(
                        checked = false,
                        onCheckedChange = { onYes() },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.yes_action), fontFamily = GoogleSansFlex)
                    }
                }
            }
        }
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
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) { dismissButton(); confirmButton() }
            }
        }
    }
}