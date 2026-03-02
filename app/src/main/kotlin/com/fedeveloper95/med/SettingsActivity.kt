@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewStream
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.elements.NotificationsSettingsActivity.SortOrderPopup
import com.fedeveloper95.med.elements.SettingsActivity.StartWeekPopup
import com.fedeveloper95.med.elements.SettingsActivity.ThemePopup
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("med_settings", MODE_PRIVATE) }
            val savedTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)
            var currentThemeOverride by remember { mutableIntStateOf(savedTheme) }

            MedTheme(themeOverride = currentThemeOverride) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        onBack = { finish() },
                        currentTheme = currentThemeOverride,
                        onThemeChanged = { newTheme -> currentThemeOverride = newTheme },
                        isExpandedScreen = isExpandedScreen
                    )
                }
            }
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentTheme: Int,
    onThemeChanged: (Int) -> Unit,
    isExpandedScreen: Boolean
) {
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }

    var weekStart by remember {
        mutableStateOf(
            prefs.getString(PREF_WEEK_START, "monday") ?: "monday"
        )
    }
    var sortOrder by remember { mutableStateOf(prefs.getString(PREF_SORT_ORDER, "time") ?: "time") }
    var experimentalBottomSheet by remember {
        mutableStateOf(
            prefs.getBoolean(
                "pref_experimental_bottom_sheet",
                true
            )
        )
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showWeekStartDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    val appInfo = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = pInfo.versionName ?: "1.0"
            val build =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong()
            context.getString(R.string.version_format, version, build)
        } catch (e: Exception) {
            context.getString(R.string.unknown)
        }
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

    Scaffold(
        topBar = {
            MaterialTheme(typography = appBarTypography) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.settings_title),
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
            .fillMaxSize()
            .then(if (isExpandedScreen) Modifier.padding(horizontal = 64.dp) else Modifier)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.settings_header_customization),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_theme_title),
                    subtitle = when (currentTheme) {
                        THEME_LIGHT -> stringResource(R.string.settings_theme_light)
                        THEME_DARK -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    },
                    containerColor = Color(0xFFfcbd00),
                    iconColor = Color(0xFF6d3a01),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    onClick = { showThemeDialog = true }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.Event,
                    title = stringResource(R.string.settings_week_start_title),
                    subtitle = if (weekStart == "sunday") stringResource(R.string.sunday) else stringResource(
                        R.string.monday
                    ),
                    containerColor = Color(0xFFffb683),
                    iconColor = Color(0xFF753403),
                    shape = RoundedCornerShape(4.dp),
                    onClick = { showWeekStartDialog = true }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.Sort,
                    title = stringResource(R.string.settings_sort_order_title),
                    subtitle = if (sortOrder == "time") stringResource(R.string.settings_sort_order_time) else stringResource(
                        R.string.settings_sort_order_custom
                    ),
                    containerColor = Color(0xFF40C4FF),
                    iconColor = Color(0xFF003B5C),
                    shape = RoundedCornerShape(4.dp),
                    onClick = { showSortDialog = true }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsItemCard(
                    icon = ImageVector.vectorResource(R.drawable.ic_quick_actions),
                    title = stringResource(R.string.settings_presets_title),
                    subtitle = stringResource(R.string.settings_presets_desc),
                    containerColor = Color(0xFF80da88),
                    iconColor = Color(0xFF00522c),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    onClick = {
                        val intent = Intent(context, QuickActionsSettingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.settings_header_preferences),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.settings_notifications_title),
                    subtitle = stringResource(R.string.settings_notifications_desc),
                    containerColor = Color(0xFFffb4ab),
                    iconColor = Color(0xFF690005),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    onClick = {
                        val intent = Intent(context, NotificationsSettingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Rounded.ViewStream,
                    title = stringResource(R.string.settings_bottom_sheet_title),
                    subtitle = stringResource(R.string.settings_bottom_sheet_desc),
                    containerColor = Color(0xFFB39DDB),
                    iconColor = Color(0xFF4527A0),
                    shape = RoundedCornerShape(4.dp),
                    checked = experimentalBottomSheet,
                    onCheckedChange = {
                        experimentalBottomSheet = it
                        prefs.edit().putBoolean("pref_experimental_bottom_sheet", it).apply()
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.settings_advanced_title),
                    subtitle = stringResource(R.string.settings_advanced_desc),
                    containerColor = Color(0xFFC5C0FF),
                    iconColor = Color(0xFF2D237A),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    onClick = {
                        val intent = Intent(context, AdvancedSettingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Text(
                        text = stringResource(R.string.settings_header_language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingsItemCard(
                        icon = Icons.Rounded.Language,
                        title = stringResource(R.string.settings_language_title),
                        subtitle = stringResource(R.string.settings_language_desc),
                        containerColor = Color(0xFFffb3ae),
                        iconColor = Color(0xFF8a1a16),
                        shape = RoundedCornerShape(20.dp),
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_APP_LOCALE_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_header_info),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_version_title),
                    subtitle = appInfo,
                    containerColor = Color(0xFFa1c9ff),
                    iconColor = Color(0xFF0641a0),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    onClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.settings_developer_title),
                    subtitle = stringResource(R.string.settings_developer_name),
                    containerColor = Color(0xFFc7c7c7),
                    iconColor = Color(0xFF474747),
                    shape = RoundedCornerShape(4.dp),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/FeDeveloper95")
                        )
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.settings_report_title),
                    subtitle = stringResource(R.string.settings_report_desc),
                    containerColor = Color(0xFFffb3ae),
                    iconColor = Color(0xFF8a1a16),
                    shape = RoundedCornerShape(4.dp),
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/fedeveloper95"))
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.SystemUpdate,
                    title = stringResource(R.string.settings_check_updates_title),
                    subtitle = stringResource(R.string.settings_check_updates_desc),
                    containerColor = Color(0xFF67d4ff),
                    iconColor = Color(0xFF004e5d),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    onClick = {
                        context.startActivity(Intent(context, UpdaterActivity::class.java))
                    }
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showThemeDialog) {
        ThemePopup(
            selectedIndex = currentTheme,
            onOptionSelected = { index ->
                onThemeChanged(index)
                prefs.edit().putInt(PREF_THEME, index).apply()
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showWeekStartDialog) {
        StartWeekPopup(
            selectedIndex = if (weekStart == "monday") 0 else 1,
            onOptionSelected = { index ->
                weekStart = if (index == 0) "monday" else "sunday"
                prefs.edit().putString(PREF_WEEK_START, weekStart).apply()
                showWeekStartDialog = false
            },
            onDismiss = { showWeekStartDialog = false }
        )
    }

    if (showSortDialog) {
        SortOrderPopup(
            selectedIndex = if (sortOrder == "time") 0 else 1,
            onOptionSelected = { index ->
                sortOrder = if (index == 0) "time" else "custom"
                prefs.edit().putString(PREF_SORT_ORDER, sortOrder).apply()
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@Composable
fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    shape: Shape,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
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
            },
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    shape: Shape,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
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
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = {
                        if (checked) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            },
            modifier = Modifier
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}