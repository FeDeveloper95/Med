@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fedeveloper95.med.elements.SettingsActivity.StartWeekPopup
import com.fedeveloper95.med.elements.SettingsActivity.ThemePopup
import com.fedeveloper95.med.elements.SettingsActivity.UpdateDialog
import com.fedeveloper95.med.services.UpdateStatus
import com.fedeveloper95.med.services.Updater
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
                        onThemeChanged = { newTheme -> currentThemeOverride = newTheme }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    currentTheme: Int,
    onThemeChanged: (Int) -> Unit
) {
    val context = LocalContext.current

    fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }

    var weekStart by remember { mutableStateOf(prefs.getString(PREF_WEEK_START, "monday") ?: "monday") }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showWeekStartDialog by remember { mutableStateOf(false) }

    val openUpdateDialog = remember { activity?.intent?.getBooleanExtra("EXTRA_OPEN_UPDATE_DIALOG", false) == true }
    var showUpdateDialog by remember { mutableStateOf(openUpdateDialog) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }

    val appInfo = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = pInfo.versionName ?: "1.0"
            val build = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong()
            context.getString(R.string.version_format, version, build)
        } catch (e: Exception) {
            context.getString(R.string.unknown)
        }
    }

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    LaunchedEffect(openUpdateDialog) {
        if (openUpdateDialog) {
            updateStatus = UpdateStatus.Checking
            val update = Updater.checkForUpdates(currentVersionName)
            updateStatus = if (update != null) UpdateStatus.Available(update) else UpdateStatus.NoUpdate
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appBarTypography = MaterialTheme.typography.copy(
        headlineMedium = MaterialTheme.typography.displaySmall.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold
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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
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
                        topStart = 28.dp,
                        topEnd = 28.dp,
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
                    subtitle = if (weekStart == "sunday") stringResource(R.string.sunday) else stringResource(R.string.monday),
                    containerColor = Color(0xFFffb683),
                    iconColor = Color(0xFF753403),
                    shape = RoundedCornerShape(4.dp),
                    onClick = { showWeekStartDialog = true }
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
                    shape = RoundedCornerShape(4.dp),
                    onClick = {
                        val intent = Intent(context, QuickActionsSettingsActivity::class.java)
                        context.startActivity(intent)
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
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    ),
                    onClick = {
                        val intent = Intent(context, AdvancedSettingsActivity::class.java)
                        context.startActivity(intent)
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
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
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    onClick = {
                        try {
                            val intent = Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
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
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    ),
                    onClick = {
                        showUpdateDialog = true
                        updateStatus = UpdateStatus.Checking
                        scope.launch {
                            val update = Updater.checkForUpdates(currentVersionName)
                            updateStatus =
                                if (update != null) UpdateStatus.Available(update) else UpdateStatus.NoUpdate
                        }
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

    if (showUpdateDialog) {
        UpdateDialog(
            status = updateStatus,
            onDismiss = {
                showUpdateDialog = false
                activity?.intent?.removeExtra("EXTRA_OPEN_UPDATE_DIALOG")
            },
            onUpdate = { url ->
                Updater.startDownload(
                    context,
                    url,
                    (updateStatus as UpdateStatus.Available).info.version
                )
                showUpdateDialog = false
            },
            onCheckAgain = {
                updateStatus = UpdateStatus.Checking
                scope.launch {
                    val update = Updater.checkForUpdates(currentVersionName)
                    updateStatus =
                        if (update != null) UpdateStatus.Available(update) else UpdateStatus.NoUpdate
                }
            }
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
        modifier = Modifier.fillMaxWidth(),
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
        modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}