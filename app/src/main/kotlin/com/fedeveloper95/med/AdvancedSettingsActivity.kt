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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import java.io.InputStream
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.util.Calendar

class AdvancedSettingsActivity : ComponentActivity() {
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

            MedTheme(themeOverride = savedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdvancedSettingsScreen(
                        onBack = { finish() },
                        isExpandedScreen = isExpandedScreen
                    )
                }
            }
        }
    }
}

const val PREF_AUTO_UPDATES = "pref_auto_updates"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(onBack: () -> Unit, isExpandedScreen: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
    var autoUpdates by remember { mutableStateOf(prefs.getBoolean(PREF_AUTO_UPDATES, true)) }
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
                val success = importSettings(context, it)
                withContext(Dispatchers.Main) {
                    if (success) {
                        showRestartDialog = true
                    }
                }
            }
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
                val allItems = com.fedeveloper95.med.services.DataRepository.loadData(context)
                val dataArray = JSONArray()
                allItems.forEach { dataArray.put(it.toJson()) }
                root.put("med_data_v2", dataArray)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val fileInputStream = context.openFileInput("med_data.dat")
                val bytes = fileInputStream.readBytes()
                fileInputStream.close()
                val base64Data = Base64.encodeToString(bytes, Base64.DEFAULT)
                root.put("med_data_file", base64Data)
            } catch (e: FileNotFoundException) {
            } catch (e: Exception) {
                e.printStackTrace()
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
                try {
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (root.has("med_settings")) {
                try {
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val importedItems = mutableListOf<com.fedeveloper95.med.services.MedData>()

            if (root.has("med_data_v2")) {
                try {
                    val dataArray = root.getJSONArray("med_data_v2")
                    for (i in 0 until dataArray.length()) {
                        try {
                            importedItems.add(com.fedeveloper95.med.services.MedData.fromJson(dataArray.getJSONObject(i)))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (root.has("med_data_file")) {
                try {
                    val base64Data = root.getString("med_data_file")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    context.openFileOutput("med_data_temp.dat", Context.MODE_PRIVATE).use {
                        it.write(bytes)
                    }
                    val fis = context.openFileInput("med_data_temp.dat")
                    val ois = LegacyObjectInputStream(fis)
                    val oldList = ois.readObject() as? ArrayList<MedItem>
                    ois.close()
                    context.deleteFile("med_data_temp.dat")

                    oldList?.forEach { old ->
                        try {
                            importedItems.add(
                                com.fedeveloper95.med.services.MedData(
                                    id = old.id,
                                    groupId = old.groupId,
                                    type = old.type,
                                    title = old.title,
                                    iconName = old.iconName,
                                    colorCode = old.colorCode,
                                    frequencyLabel = old.frequencyLabel,
                                    creationDate = old.creationDate,
                                    creationTime = old.creationTime,
                                    takenHistory = old.takenHistory,
                                    recurrenceDays = old.recurrenceDays,
                                    endDate = old.endDate,
                                    notes = null,
                                    displayOrder = 0,
                                    intervalGap = null,
                                    category = null
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val currentItems = try {
                com.fedeveloper95.med.services.DataRepository.loadData(context)
            } catch (e: Exception) {
                emptyList()
            }

            val mergedItems = currentItems.toMutableList()
            val existingIds = currentItems.map { it.id }.toSet()

            importedItems.forEach { item ->
                if (!existingIds.contains(item.id)) {
                    mergedItems.add(item)
                } else {
                    val existingItemIndex = mergedItems.indexOfFirst { it.id == item.id }
                    if (existingItemIndex != -1) {
                        val existingItem = mergedItems[existingItemIndex]
                        val mergedHistory = java.util.HashMap(existingItem.takenHistory)
                        item.takenHistory.forEach { (date, time) ->
                            if (!mergedHistory.containsKey(date)) {
                                mergedHistory[date] = time
                            }
                        }
                        mergedItems[existingItemIndex] = existingItem.copy(takenHistory = mergedHistory)
                    }
                }
            }

            try {
                com.fedeveloper95.med.services.DataRepository.saveData(context, mergedItems)
                context.deleteFile("med_data.dat")
            } catch (e: Exception) {
                e.printStackTrace()
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

class LegacyObjectInputStream(inputStream: InputStream) : ObjectInputStream(inputStream) {
    override fun readClassDescriptor(): ObjectStreamClass {
        var desc = super.readClassDescriptor()
        if (desc.name == "com.fedeveloper95.med.OldDatabase" || desc.name == "com.fedeveloper95.med.MedItem" || desc.name == "com.fedeveloper95.med.MainActivity\$MedItem") {
            desc = ObjectStreamClass.lookup(MedItem::class.java)
        }
        return desc
    }
}