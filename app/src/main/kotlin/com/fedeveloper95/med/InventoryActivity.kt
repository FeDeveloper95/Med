@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
package com.fedeveloper95.med

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fedeveloper95.med.elements.InventoryActivity.WelcomeBottomSheet
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.services.MedViewModel
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import java.time.LocalDate

class InventoryActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = getSharedPreferences("med_settings", MODE_PRIVATE)
            val currentTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)
            val lifecycleOwner = LocalLifecycleOwner.current

            var showWelcomeSheet by remember {
                mutableStateOf(prefs.getBoolean("inventory_first_time", true))
            }

            var inventoryEnabled by remember {
                mutableStateOf(prefs.getBoolean("pref_inventory_enabled", true))
            }
            var hiddenMeds by remember {
                mutableStateOf(prefs.getStringSet("pref_inventory_hidden_meds", emptySet())?.toSet() ?: emptySet())
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        inventoryEnabled = prefs.getBoolean("pref_inventory_enabled", true)
                        hiddenMeds = prefs.getStringSet("pref_inventory_hidden_meds", emptySet())?.toSet() ?: emptySet()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val viewModel: MedViewModel = viewModel()
            val today = LocalDate.now()

            val allMedicines = viewModel.items
                .filter { it.type == ItemType.Medicine }
                .filter { item ->
                    val isAfterStart = !today.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !today.isAfter(item.endDate)
                    isAfterStart && isBeforeEnd
                }

            val groupedMedicines = allMedicines.groupBy { it.title.trim().lowercase() }

            val displayMedicines = remember(allMedicines, inventoryEnabled, hiddenMeds) {
                if (!inventoryEnabled) {
                    emptyList()
                } else {
                    groupedMedicines.map { (_, list) -> list.first() }
                        .filter { !hiddenMeds.contains(it.title.trim().lowercase()) }
                }
            }

            fun calculateRemainingTotal(med: MedData): Double? {
                val initialTotal = med.totalQuantity ?: return null
                if (med.dose == null) return null

                var currentTotal: Double = initialTotal
                val specificGroup = allMedicines.filter { it.title.trim().lowercase() == med.title.trim().lowercase() }

                for (item in specificGroup) {
                    item.takenHistory.forEach { (date, _) ->
                        if (!date.isBefore(item.creationDate) && (item.endDate == null || !date.isAfter(item.endDate))) {
                            currentTotal -= (item.dose ?: 0.0)
                        }
                    }
                }
                return currentTotal
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

            MedTheme(themeOverride = currentTheme) {
                if (showWelcomeSheet) {
                    WelcomeBottomSheet(onDismiss = {
                        prefs.edit().putBoolean("inventory_first_time", false).apply()
                        showWelcomeSheet = false
                    })
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        MaterialTheme(typography = appBarTypography) {
                            LargeTopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(R.string.inventory_title),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                navigationIcon = {
                                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                                        ExpressiveIconButton(
                                            onClick = { finish() },
                                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = stringResource(R.string.back),
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                actions = {
                                    ExpressiveIconButton(
                                        onClick = { startActivity(Intent(this@InventoryActivity, InventorySettingsActivity::class.java)) },
                                        icon = Icons.Rounded.Settings,
                                        contentDescription = stringResource(R.string.settings_desc),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                },
                                colors = TopAppBarDefaults.largeTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                scrollBehavior = scrollBehavior
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    if (displayMedicines.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_warning),
                                    contentDescription = null,
                                    modifier = Modifier.size(128.dp),
                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.empty_inventory),
                                    fontFamily = GoogleSansFlex,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            itemsIndexed(displayMedicines, key = { _, item -> item.id }) { index, item ->
                                val isFirstInGroup = index == 0
                                val isLastInGroup = index == displayMedicines.lastIndex
                                val topRadius = if (isFirstInGroup) 20.dp else 4.dp
                                val bottomRadius = if (isLastInGroup) 20.dp else 4.dp
                                val shape = RoundedCornerShape(
                                    topStart = topRadius,
                                    topEnd = topRadius,
                                    bottomStart = bottomRadius,
                                    bottomEnd = bottomRadius
                                )
                                val remaining = calculateRemainingTotal(item)
                                InventoryCard(item = item, shape = shape, remaining = remaining)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryCard(item: MedData, shape: androidx.compose.ui.graphics.Shape, remaining: Double?) {
    val context = LocalContext.current

    val formatDouble: (Double?) -> String = { d ->
        if (d == null) ""
        else if (d == d.toLong().toDouble()) d.toLong().toString()
        else String.format("%.1f", d)
    }

    val isLowStock = remaining != null && remaining <= 5
    val progress = if (item.totalQuantity != null && item.totalQuantity > 0) (remaining?.toFloat() ?: 0f) / item.totalQuantity.toFloat() else 0f

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val icon = when (item.iconName) {
        "MixtureMed" -> icSick
        "Bed" -> icMind
        "Mood" -> icMixture
        else -> if (item.iconName != null && AVAILABLE_ICONS.containsKey(item.iconName)) AVAILABLE_ICONS[item.iconName]!!
        else Icons.Rounded.MedicalServices
    }

    val customColor = remember(item.colorCode) {
        if (item.colorCode != null && item.colorCode != "dynamic") try {
            Color(android.graphics.Color.parseColor(item.colorCode))
        } catch (e: Exception) {
            null
        } else null
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface
    val iconBoxColor = customColor ?: MaterialTheme.colorScheme.tertiaryContainer
    val iconBoxTintColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(iconBoxColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconBoxTintColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = item.title,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (remaining != null) {
                            val unitStr = if (item.unit != null) " ${item.unit}" else ""
                            Text(
                                text = stringResource(R.string.inventory_remaining_format, formatDouble(remaining), unitStr),
                                fontFamily = GoogleSansFlex,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.inventory_not_set),
                                fontFamily = GoogleSansFlex,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                if (isLowStock) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (remaining != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = contentColor.copy(alpha = 0.1f)
                )
            }
        }
    }
}