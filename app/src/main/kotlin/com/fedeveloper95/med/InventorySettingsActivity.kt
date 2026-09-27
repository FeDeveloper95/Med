@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
package com.fedeveloper95.med

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color.parseColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fedeveloper95.med.services.MedViewModel
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import java.time.LocalDate

class InventorySettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { getSharedPreferences("med_settings", MODE_PRIVATE) }
            val currentTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)

            MedTheme(themeOverride = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InventorySettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventorySettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activity = context as ComponentActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
    val viewModel: MedViewModel = viewModel()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    var inventoryEnabled by remember { mutableStateOf(prefs.getBoolean("pref_inventory_enabled", true)) }
    var notificationsEnabled by remember { mutableStateOf(hasNotificationPermission && prefs.getBoolean("pref_inventory_notifs_enabled", true)) }
    var warningDays by remember { mutableIntStateOf(prefs.getInt("pref_inventory_warning_days", 5)) }

    var hiddenMeds by remember {
        mutableStateOf(prefs.getStringSet("pref_inventory_hidden_meds", emptySet())?.toSet() ?: emptySet())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
                hasNotificationPermission = hasPerm

                if (!hasPerm && notificationsEnabled) {
                    notificationsEnabled = false
                    prefs.edit().putBoolean("pref_inventory_notifs_enabled", false).apply()
                } else if (hasPerm && prefs.getBoolean("pref_inventory_notifs_enabled", false)) {
                    notificationsEnabled = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        notificationsEnabled = isGranted
        prefs.edit().putBoolean("pref_inventory_notifs_enabled", isGranted).apply()
    }

    val handleNotificationToggle: (Boolean) -> Unit = { checked ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (checked) {
            val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }

            if (hasPerm) {
                notificationsEnabled = true
                prefs.edit().putBoolean("pref_inventory_notifs_enabled", true).apply()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasRequested = prefs.getBoolean("has_requested_notification_permission", false)
                    val rationale = activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

                    if (!hasRequested || rationale) {
                        prefs.edit().putBoolean("has_requested_notification_permission", true).apply()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        val intent = Intent().apply {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                } else {
                    val intent = Intent().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            action = "android.settings.APP_NOTIFICATION_SETTINGS"
                            putExtra("app_package", context.packageName)
                            putExtra("app_uid", context.applicationInfo.uid)
                        }
                    }
                    context.startActivity(intent)
                }
            }
        } else {
            notificationsEnabled = false
            prefs.edit().putBoolean("pref_inventory_notifs_enabled", false).apply()
        }
    }

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)
    val availableIcons: Map<String, ImageVector> = remember(icSick, icMind, icMixture) {
        val tempMap = AVAILABLE_ICONS.toMutableMap()
        tempMap["MixtureMed"] = icSick
        tempMap["Bed"] = icMind
        tempMap["Mood"] = icMixture
        tempMap
    }

    val today = remember { LocalDate.now() }
    val medsUnique = remember(viewModel.items) {
        viewModel.items
            .filter { it.type == ItemType.Medicine }
            .filter { item ->
                val isAfterStart = !today.isBefore(item.creationDate)
                val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                isAfterStart && isBeforeEnd
            }
            .distinctBy { it.title.trim().lowercase() }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appBarTypography = MaterialTheme.typography.copy(
        headlineMedium = MaterialTheme.typography.displaySmall.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal
        ),
        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Scaffold(
            topBar = {
                MaterialTheme(typography = appBarTypography) {
                    LargeTopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.settings_inventory_title),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                                ExpressiveIconButton(
                                    onClick = onBack,
                                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.discard),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            },
            containerColor = Color.Transparent,
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { padding ->
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    val headerIconRes = when {
                        !inventoryEnabled -> R.drawable.header_inventory_off
                        inventoryEnabled && !notificationsEnabled -> R.drawable.header_notifs_none
                        else -> R.drawable.header_inventory_notifs_notifications
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = headerIconRes,
                            animationSpec = tween(durationMillis = 300),
                            label = "header_transition"
                        ) { iconRes ->
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                }

                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val shape = RoundedCornerShape(64.dp)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                inventoryEnabled = !inventoryEnabled
                                prefs.edit().putBoolean("pref_inventory_enabled", inventoryEnabled).apply()
                            },
                        shape = shape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        ListItem(
                            modifier = Modifier.padding(vertical = 4.dp),
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.inventory_master_toggle_title),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = inventoryEnabled,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        inventoryEnabled = it
                                        prefs.edit().putBoolean("pref_inventory_enabled", it).apply()
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (inventoryEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        InventorySegmentedSwitchItem(
                            icon = Icons.Rounded.Notifications,
                            title = stringResource(R.string.inventory_notifs_title),
                            subtitle = stringResource(R.string.inventory_notifs_desc),
                            containerColor = Color(0xFF80DEEA),
                            iconColor = Color(0xFF006064),
                            index = 0,
                            count = 2,
                            checked = notificationsEnabled,
                            enabled = inventoryEnabled,
                            onCheckedChange = handleNotificationToggle
                        )

                        InventorySegmentedSliderItem(
                            icon = Icons.Rounded.Warning,
                            title = stringResource(R.string.inventory_warning_days_title),
                            subtitle = stringResource(R.string.inventory_warning_days_desc, warningDays),
                            containerColor = Color(0xFFffb683),
                            iconColor = Color(0xFF753403),
                            index = 1,
                            count = 2,
                            value = warningDays,
                            enabled = inventoryEnabled && notificationsEnabled,
                            onValueChange = { newValue ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                warningDays = newValue
                                prefs.edit().putInt("pref_inventory_warning_days", newValue).apply()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    if (medsUnique.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.inventory_manage_stock_header),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier.animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            medsUnique.forEachIndexed { presetIndex, med ->
                                val isSelected = !hiddenMeds.contains(med.title.trim().lowercase())

                                val activeBgColor = if (med.colorCode == null || med.colorCode == "dynamic") {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    try {
                                        Color(parseColor(med.colorCode))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                }

                                val activeIconColor = if (med.colorCode == null || med.colorCode == "dynamic") {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    Color.Black.copy(alpha = 0.7f)
                                }

                                val iconToUse = availableIcons[med.iconName] ?: Icons.Rounded.MedicalServices

                                InventorySegmentedSwitchItem(
                                    icon = iconToUse,
                                    title = med.title,
                                    subtitle = "",
                                    containerColor = if (isSelected) activeBgColor else MaterialTheme.colorScheme.surfaceVariant,
                                    iconColor = if (isSelected) activeIconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    index = presetIndex,
                                    count = medsUnique.size,
                                    checked = isSelected,
                                    enabled = inventoryEnabled,
                                    isCompact = true,
                                    onCheckedChange = { checked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val medKey = med.title.trim().lowercase()
                                        val newSet = hiddenMeds.toMutableSet()
                                        if (checked) {
                                            newSet.remove(medKey)
                                        } else {
                                            newSet.add(medKey)
                                        }
                                        hiddenMeds = newSet
                                        prefs.edit().putStringSet("pref_inventory_hidden_meds", newSet).apply()
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InventorySegmentedSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    index: Int,
    count: Int,
    checked: Boolean,
    enabled: Boolean = true,
    isCompact: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    SegmentedListItem(
        selected = false,
        onClick = { if (enabled) onCheckedChange(!checked) },
        modifier = if (count == 1) Modifier.clip(RoundedCornerShape(20.dp)) else Modifier,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.5f)
                    .padding(vertical = if (isCompact) 0.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 40.dp else 48.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal,
                        style = if (isCompact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange,
                    thumbContent = {
                        Icon(
                            imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                )
            }
        }
    )
}

@Composable
fun InventorySegmentedSliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    index: Int,
    count: Int,
    value: Int,
    enabled: Boolean = true,
    onValueChange: (Int) -> Unit
) {
    SegmentedListItem(
        selected = false,
        onClick = { },
        modifier = if (count == 1) Modifier.clip(RoundedCornerShape(20.dp)) else Modifier,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.5f)
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValueChange(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    enabled = enabled,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    )
}