@file:OptIn(
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.fedeveloper95.med

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color.parseColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.services.WearSyncManager
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme

class WearSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("med_settings", MODE_PRIVATE) }
            val savedTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)
            MedTheme(themeOverride = savedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WearSettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun WearSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
    var useWearOS by remember { mutableStateOf(prefs.getBoolean("pref_use_wearos", false)) }
    val showNotifications = remember { prefs.getBoolean(PREF_SHOW_NOTIFICATIONS, true) }
    val fullScreenAlarm = remember { prefs.getBoolean(PREF_FULL_SCREEN_ALARM, true) }
    val areAlarmsEnabled = showNotifications && fullScreenAlarm
    var ringOnWatch by remember { mutableStateOf(prefs.getBoolean("pref_wear_ring_alarms", true)) }

    val presetsStrings = remember { loadPresets(prefs) }
    val allNames = remember(presetsStrings) {
        presetsStrings.mapNotNull {
            it.split("|").getOrNull(1)?.takeIf { name -> name.isNotBlank() }
        }.toSet()
    }
    var wearEnabledEvents by remember {
        val saved = prefs.getStringSet("pref_wear_enabled_events", null)
        val initial = saved?.filter { allNames.contains(it) }?.toSet() ?: allNames
        if (saved == null || saved != initial) {
            prefs.edit().putStringSet("pref_wear_enabled_events", initial).apply()
        }
        mutableStateOf(initial)
    }

    LaunchedEffect(Unit) {
        WearSyncManager.initialize(context)
        WearSyncManager.syncSettings(ringOnWatch, wearEnabledEvents, allNames)
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
                                text = stringResource(R.string.wearos_settings_title),
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
                item { Spacer(modifier = Modifier.height(20.dp)) }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.header_watch),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(200.dp)
                        )
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
                                useWearOS = !useWearOS
                                prefs.edit().putBoolean("pref_use_wearos", useWearOS).apply()
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
                                    text = stringResource(R.string.wearos_master_toggle_title),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = useWearOS,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        useWearOS = it
                                        prefs.edit().putBoolean("pref_use_wearos", it).apply()
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (useWearOS) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        WearSegmentedSwitchItem(
                            icon = Icons.Rounded.Watch,
                            title = stringResource(R.string.wearos_ring_alarms_title),
                            subtitle = if (areAlarmsEnabled) stringResource(R.string.wearos_ring_alarms_desc) else stringResource(R.string.wearos_alarms_disabled_desc),
                            containerColor = if (areAlarmsEnabled && useWearOS) Color(0xFF80DEEA) else MaterialTheme.colorScheme.surfaceVariant,
                            iconColor = if (areAlarmsEnabled && useWearOS) Color(0xFF006064) else MaterialTheme.colorScheme.onSurfaceVariant,
                            index = 0,
                            count = 1,
                            checked = ringOnWatch,
                            enabled = areAlarmsEnabled && useWearOS,
                            onCheckedChange = { isChecked ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                ringOnWatch = isChecked
                                prefs.edit().putBoolean("pref_wear_ring_alarms", isChecked).apply()
                                WearSyncManager.syncSettings(isChecked, wearEnabledEvents, allNames)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    val validPresets = presetsStrings.filter {
                        it.split("|").getOrNull(1)?.isNotBlank() == true
                    }

                    if (validPresets.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.wearos_quick_actions_header),
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
                            validPresets.forEachIndexed { presetIndex, preset ->
                                val parts = preset.split("|")
                                val event = parts.getOrNull(1) ?: ""
                                val iconName = parts.getOrNull(2) ?: "Event"
                                val colorCode = parts.getOrNull(3) ?: "dynamic"

                                val isSelected = wearEnabledEvents.contains(event)
                                val activeBgColor = if (colorCode == "dynamic") {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    try {
                                        Color(parseColor(colorCode))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    }
                                }

                                val activeIconColor = if (colorCode == "dynamic") {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    Color.Black.copy(alpha = 0.7f)
                                }

                                WearSegmentedSwitchItem(
                                    icon = availableIcons[iconName] ?: Icons.Rounded.Check,
                                    title = event,
                                    subtitle = "",
                                    containerColor = if (isSelected) activeBgColor else MaterialTheme.colorScheme.surfaceVariant,
                                    iconColor = if (isSelected) activeIconColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    index = presetIndex,
                                    count = validPresets.size,
                                    checked = isSelected,
                                    enabled = useWearOS,
                                    isCompact = true,
                                    onCheckedChange = { checked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val newSet = wearEnabledEvents.toMutableSet()
                                        if (checked) newSet.add(event) else newSet.remove(event)
                                        wearEnabledEvents = newSet
                                        prefs.edit().putStringSet("pref_wear_enabled_events", newSet).apply()
                                        WearSyncManager.syncSettings(ringOnWatch, newSet, allNames)
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
fun WearSegmentedSwitchItem(
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
fun WearSegmentedItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    index: Int,
    count: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    SegmentedListItem(
        selected = false,
        onClick = { if (enabled) onClick() },
        modifier = if (count == 1) Modifier.clip(RoundedCornerShape(20.dp)) else Modifier,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.5f)
                    .padding(vertical = 4.dp),
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
        }
    )
}