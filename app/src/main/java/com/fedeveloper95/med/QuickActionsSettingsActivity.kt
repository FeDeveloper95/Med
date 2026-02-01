@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalAnimationApi::class)

package com.fedeveloper95.med

import android.content.Context
import android.graphics.Color.parseColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import org.json.JSONArray

val AVAILABLE_ICONS = mapOf(
    "MedicalServices" to Icons.Rounded.MedicalServices,
    "Event" to Icons.Rounded.Event,
    "FitnessCenter" to Icons.Rounded.FitnessCenter,
    "Restaurant" to Icons.Rounded.Restaurant,
    "Work" to Icons.Rounded.Work,
    "School" to Icons.Rounded.School,
    "ShoppingCart" to Icons.Rounded.ShoppingCart,
    "LocalHospital" to Icons.Rounded.LocalHospital,
    "Favorite" to Icons.Rounded.Favorite,
    "Star" to Icons.Rounded.Star,
    "Bolt" to Icons.Rounded.Bolt,
    "WaterDrop" to Icons.Rounded.WaterDrop,
    "Bed" to Icons.Rounded.Bed,
    "DirectionsRun" to Icons.Rounded.DirectionsRun,
    "Mood" to Icons.Rounded.Mood,
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

class QuickActionsSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
            val currentTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)

            MedTheme(themeOverride = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuickActionsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun QuickActionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }

    var presetsList by remember {
        mutableStateOf(loadPresets(prefs))
    }

    var newName by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Event") }
    var selectedColor by remember { mutableStateOf("dynamic") }
    val selectedType = ItemType.Event

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun savePresets(list: List<String>) {
        presetsList = list
        val jsonArray = JSONArray(list)
        prefs.edit().putString(PREF_PRESETS_ORDERED, jsonArray.toString()).apply()
    }

    fun addPreset() {
        if (newName.isNotBlank()) {
            val newItem = "${selectedType.name}|$newName|$selectedIconName|$selectedColor"
            savePresets(presetsList + newItem)
            newName = ""
            selectedIconName = "Event"
            selectedColor = "dynamic"
        }
    }

    fun removePreset(index: Int) {
        val newList = presetsList.toMutableList()
        newList.removeAt(index)
        savePresets(newList)
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        if (toIndex in presetsList.indices) {
            val newList = presetsList.toMutableList()
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            savePresets(newList)
        }
    }

    // --- OVERRIDE TIPOGRAFICO ---
    val appBarTypography = MaterialTheme.typography.copy(
        headlineMedium = MaterialTheme.typography.displaySmall.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold // BOLD quando espanso
        ),
        titleLarge = MaterialTheme.typography.titleLarge.copy(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal // NORMAL quando collassato
        )
    )

    Scaffold(
        topBar = {
            MaterialTheme(typography = appBarTypography) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.quick_actions_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        // Padding applicato all'icona
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                            ExpressiveIconButton(
                                onClick = onBack,
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
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
        // Convertito in LazyColumn
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Preview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val iconVector = AVAILABLE_ICONS[selectedIconName] ?: Icons.Rounded.Event

                        val previewBackgroundColor = if (selectedColor == "dynamic") {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            try {
                                Color(android.graphics.Color.parseColor(selectedColor))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        }

                        val previewIconColor = if (selectedColor == "dynamic") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.7f)
                        }

                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(previewBackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(targetState = iconVector, label = "iconAnim") { targetIcon ->
                                Icon(
                                    imageVector = targetIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = previewIconColor
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Name Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.name_hint), fontFamily = GoogleSansFlex) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Icon Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 16.dp)
                    ) {
                        val icons = AVAILABLE_ICONS.toList()
                        val rows = icons.chunked(4)

                        rows.forEachIndexed { index, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for ((name, icon) in rowItems) {
                                    val isSelected = selectedIconName == name

                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isPressed by interactionSource.collectIsPressedAsState()

                                    val cornerPercent by animateIntAsState(
                                        targetValue = if (isPressed) 15 else 50,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "corner"
                                    )

                                    val baseColor = MaterialTheme.colorScheme.surfaceVariant
                                    val selectedColorBg = MaterialTheme.colorScheme.primaryContainer

                                    val containerColor by animateColorAsState(if (isSelected) selectedColorBg else baseColor)
                                    val iconTint by animateColorAsState(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(percent = cornerPercent))
                                            .background(containerColor)
                                            .clickable(interactionSource = interactionSource, indication = null) { selectedIconName = name },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = name,
                                            tint = iconTint,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            if (index < rows.lastIndex) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Color Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AVAILABLE_COLORS.forEach { colorCode ->
                                val isSelected = selectedColor == colorCode
                                val isDynamic = colorCode == "dynamic"

                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()

                                val targetCorner = when {
                                    isPressed -> 15
                                    isSelected -> 35
                                    else -> 50
                                }

                                val cornerPercent by animateIntAsState(
                                    targetValue = targetCorner,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "colorCorner"
                                )

                                val backgroundColor = if (isDynamic) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    try {
                                        Color(android.graphics.Color.parseColor(colorCode))
                                    } catch (e: Exception) {
                                        Color.Gray
                                    }
                                }

                                val borderWidth = if (isSelected) 3.dp else 0.dp
                                val borderColor = if (isDynamic) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    backgroundColor.darken(0.7f)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(percent = cornerPercent))
                                        .background(backgroundColor)
                                        .then(if(isDynamic && isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                                        .border(borderWidth, borderColor, RoundedCornerShape(percent = cornerPercent))
                                        .clickable(interactionSource = interactionSource, indication = null) { selectedColor = colorCode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDynamic) {
                                        Icon(
                                            imageVector = Icons.Rounded.Palette,
                                            contentDescription = "Dynamic Theme",
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Add Button Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ExpressiveButton(
                            onClick = { addPreset() },
                            text = stringResource(R.string.add_action),
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = if (newName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (newName.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // --- ACTIVE ACTIONS LIST ---
            if (presetsList.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.active_actions_header),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }

                itemsIndexed(presetsList) { index, itemString ->
                    val parts = itemString.split("|")
                    val name = parts.getOrNull(1) ?: "Unknown"
                    val iconName = parts.getOrNull(2) ?: "Event"
                    val colorCode = parts.getOrNull(3) ?: "dynamic"

                    val icon = AVAILABLE_ICONS[iconName] ?: Icons.Rounded.Event

                    val itemBgColor = if (colorCode == "dynamic") {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        try {
                            Color(android.graphics.Color.parseColor(colorCode))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    }

                    val itemIconTint = if (colorCode == "dynamic") {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        Color.Black.copy(alpha = 0.7f)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = when {
                            presetsList.size == 1 -> RoundedCornerShape(28.dp)
                            index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                            index == presetsList.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                            else -> RoundedCornerShape(4.dp)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = name,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(itemBgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = itemIconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            trailingContent = {
                                Row {
                                    if (index > 0) {
                                        IconButton(onClick = { moveItem(index, index - 1) }) {
                                            Icon(Icons.Default.KeyboardArrowUp, null)
                                        }
                                    }
                                    if (index < presetsList.lastIndex) {
                                        IconButton(onClick = { moveItem(index, index + 1) }) {
                                            Icon(Icons.Default.KeyboardArrowDown, null)
                                        }
                                    }
                                    IconButton(onClick = { removePreset(index) }) {
                                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.quick_actions_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = GoogleSansFlex
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

fun loadPresets(prefs: android.content.SharedPreferences): List<String> {
    val jsonString = prefs.getString(PREF_PRESETS_ORDERED, null)
    if (jsonString != null) {
        try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            return list
        } catch (e: Exception) { e.printStackTrace() }
    }

    val oldSet = prefs.getStringSet(PREF_PRESETS, null)
    return oldSet?.toList() ?: emptyList()
}

fun Color.darken(factor: Float = 0.7f): Color {
    return Color(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
        alpha = this.alpha
    )
}