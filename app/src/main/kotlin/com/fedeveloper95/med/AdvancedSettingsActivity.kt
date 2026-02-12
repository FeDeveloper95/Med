@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.fedeveloper95.med

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.elements.AdvancedSettingsActivity.RestorePopup
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.Calendar

class AdvancedSettingsActivity : ComponentActivity() {
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
                    AdvancedSettingsScreen(onBack = { finish() })
                }
            }
        }
    }
}

const val PREF_AUTO_UPDATES = "pref_auto_updates"
const val PREF_EXPERIMENTAL_BOTTOM_SHEET = "pref_experimental_bottom_sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
    var autoUpdates by remember { mutableStateOf(prefs.getBoolean(PREF_AUTO_UPDATES, true)) }
    var experimentalBottomSheet by remember { mutableStateOf(prefs.getBoolean(PREF_EXPERIMENTAL_BOTTOM_SHEET, false)) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                exportSettings(context, it)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                if (importSettings(context, it)) {
                    showRestartDialog = true
                }
            }
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
                            text = stringResource(R.string.settings_advanced_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                            ExpressiveIconButton(
                                onClick = onBack,
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back),
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
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                SettingsSwitchCard(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(R.string.settings_auto_updates_title),
                    subtitle = stringResource(R.string.settings_auto_updates_desc),
                    containerColor = Color(0xFFfcbd00),
                    iconColor = Color(0xFF6d3a01),
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    checked = autoUpdates,
                    onCheckedChange = {
                        autoUpdates = it
                        prefs.edit().putBoolean(PREF_AUTO_UPDATES, it).apply()
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.Flag,
                    title = stringResource(R.string.settings_setup_title),
                    subtitle = stringResource(R.string.settings_setup_desc),
                    containerColor = Color(0xFFffaee4),
                    iconColor = Color(0xFF8d0053),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    ),
                    onClick = {
                        val intent = Intent(context, WelcomeActivity::class.java).apply {
                            putExtra("FORCE_SHOW", true)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                Text(
                    text = stringResource(R.string.settings_experimental_header),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Rounded.ViewStream,
                    title = stringResource(R.string.settings_bottom_sheet_title),
                    subtitle = stringResource(R.string.settings_bottom_sheet_desc),
                    containerColor = Color(0xFFB39DDB),
                    iconColor = Color(0xFF4527A0),
                    shape = RoundedCornerShape(28.dp),
                    checked = experimentalBottomSheet,
                    onCheckedChange = {
                        experimentalBottomSheet = it
                        prefs.edit().putBoolean(PREF_EXPERIMENTAL_BOTTOM_SHEET, it).apply()
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                Text(
                    text = stringResource(R.string.settings_backup_header),
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
                    icon = Icons.Rounded.CloudUpload,
                    title = stringResource(R.string.settings_export_title),
                    subtitle = stringResource(R.string.settings_export_desc),
                    containerColor = Color(0xFF80da88),
                    iconColor = Color(0xFF00522c),
                    shape = RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    onClick = {
                        val timestamp = Calendar.getInstance().timeInMillis
                        exportLauncher.launch("med_backup_$timestamp.json")
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }

            item {
                SettingsItemCard(
                    icon = Icons.Rounded.CloudDownload,
                    title = stringResource(R.string.settings_import_title),
                    subtitle = stringResource(R.string.settings_import_desc),
                    containerColor = Color(0xFF67d4ff),
                    iconColor = Color(0xFF004e5d),
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 28.dp,
                        bottomEnd = 28.dp
                    ),
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }

    if (showRestartDialog) {
        RestorePopup(
            onDismiss = { showRestartDialog = false },
            onRestart = {
                val packageManager = context.packageManager
                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                val componentName = intent?.component
                val mainIntent = Intent.makeRestartActivityTask(componentName)
                context.startActivity(mainIntent)
                Runtime.getRuntime().exit(0)
            }
        )
    }
}

private suspend fun exportSettings(context: Context, uri: Uri) {
    withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            val prefs = context.getSharedPreferences("med_prefs", Context.MODE_PRIVATE)
            val settings = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)

            val prefsJson = JSONObject()
            prefs.all.forEach { (k, v) ->
                when (v) {
                    is Set<*> -> prefsJson.put(k, JSONArray(v))
                    else -> prefsJson.put(k, v)
                }
            }
            root.put("med_prefs", prefsJson)

            val settingsJson = JSONObject()
            settings.all.forEach { (k, v) ->
                when (v) {
                    is Set<*> -> settingsJson.put(k, JSONArray(v))
                    else -> settingsJson.put(k, v)
                }
            }
            root.put("med_settings", settingsJson)

            try {
                val fileInputStream = context.openFileInput("med_data.dat")
                val bytes = fileInputStream.readBytes()
                fileInputStream.close()
                val base64Data = Base64.encodeToString(bytes, Base64.DEFAULT)
                root.put("med_data_file", base64Data)
            } catch (e: FileNotFoundException) {
            }

            context.contentResolver.openOutputStream(uri)?.use {
                it.write(root.toString().toByteArray())
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.export_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun importSettings(context: Context, uri: Uri): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line)
                        line = reader.readLine()
                    }
                }
            }
            val root = JSONObject(sb.toString())

            if (root.has("med_prefs")) {
                val prefs = context.getSharedPreferences("med_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit().clear()
                val json = root.getJSONObject("med_prefs")
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    when (val value = json.get(key)) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                        is String -> editor.putString(key, value)
                        is JSONArray -> {
                            val set = mutableSetOf<String>()
                            for (i in 0 until value.length()) set.add(value.getString(i))
                            editor.putStringSet(key, set)
                        }
                    }
                }
                editor.apply()
            }

            if (root.has("med_settings")) {
                val settings = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
                val editor = settings.edit().clear()
                val json = root.getJSONObject("med_settings")
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    when (val value = json.get(key)) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                        is String -> editor.putString(key, value)
                        is JSONArray -> {
                            val set = mutableSetOf<String>()
                            for (i in 0 until value.length()) set.add(value.getString(i))
                            editor.putStringSet(key, set)
                        }
                    }
                }
                editor.apply()
            }

            if (root.has("med_data_file")) {
                try {
                    val base64Data = root.getString("med_data_file")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    context.openFileOutput("med_data.dat", Context.MODE_PRIVATE).use {
                        it.write(bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.import_error), Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}