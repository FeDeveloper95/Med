package com.fedeveloper95.med

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.fedeveloper95.med.elements.MainActivity.DeleteElement
import com.fedeveloper95.med.services.WearDataManager
import com.fedeveloper95.med.services.WearItem
import com.fedeveloper95.med.ui.theme.MedTheme

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

data class ItemToDelete(
    val rawName: String,
    val displayName: String,
    val type: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearDataManager.initialize(this)

        WearDataManager.requestSyncFromPhone()

        setContent {
            MedTheme {
                AppScaffold(
                    timeText = { TimeText() }
                ) {
                    MainScreen(
                        medicines = WearDataManager.medicines,
                        events = WearDataManager.eventsList,
                        onOpenAddEvents = {
                            startActivity(Intent(this@MainActivity, AddEventActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

fun parseCustomColor(colorStr: String?): Color? {
    if (colorStr.isNullOrBlank() || colorStr.equals("dynamic", ignoreCase = true)) return null
    return try {
        Color(android.graphics.Color.parseColor(colorStr))
    } catch (e: Exception) {
        null
    }
}

fun getIconForName(iconName: String?): ImageVector? {
    if (iconName.isNullOrBlank()) return null
    return AVAILABLE_ICONS[iconName]
}

@Composable
fun MainScreen(
    medicines: List<WearItem>,
    events: List<WearItem>,
    onOpenAddEvents: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var itemToDelete by remember { mutableStateOf<ItemToDelete?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = onOpenAddEvents,
                    buttonSize = EdgeButtonSize.Large
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_event),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        ) {
            ScalingLazyColumn(
                state = listState,
                autoCentering = AutoCenteringParams(itemIndex = 1),
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item {
                    ListHeader {
                        Text(
                            text = stringResource(R.string.header_medicines),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (medicines.isEmpty()) {
                    item {
                        EmptyStateCard(text = stringResource(R.string.no_medicines))
                    }
                } else {
                    items(medicines, key = { "med_${it.title}_${it.time}" }) { med ->
                        val name = med.title
                        val time = med.time
                        val isTaken = WearDataManager.itemStates[name] == true
                        val itemIcon = getIconForName(med.iconName)

                        val customColor = remember(med.colorCode) { parseCustomColor(med.colorCode) }
                        val circleColor = customColor ?: MaterialTheme.colorScheme.primaryContainer
                        val iconTintColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimaryContainer

                        Card(
                            onClick = { WearDataManager.setItemTaken(name, !isTaken, "medicine") },
                            onLongClick = { itemToDelete = ItemToDelete(name, name, "medicine") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(circleColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (itemIcon != null) {
                                        Icon(
                                            imageVector = itemIcon,
                                            contentDescription = null,
                                            tint = iconTintColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (name.isNotEmpty()) {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            color = iconTintColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.size(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (time.isNotEmpty()) {
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (isTaken) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(24.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    ListHeader {
                        Text(
                            text = stringResource(R.string.header_events),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (events.isEmpty()) {
                    item {
                        EmptyStateCard(text = stringResource(R.string.no_events))
                    }
                } else {
                    items(events, key = { "evt_${it.title}_${it.time}_${System.identityHashCode(it)}" }) { ev ->                        val name = ev.title
                        val time = ev.time
                        val itemIcon = getIconForName(ev.iconName)

                        val customColor = remember(ev.colorCode) { parseCustomColor(ev.colorCode) }
                        val circleColor = customColor ?: MaterialTheme.colorScheme.primaryContainer
                        val iconTintColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimaryContainer

                        Card(
                            onClick = { },
                            onLongClick = { itemToDelete = ItemToDelete(name, name, "event") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(circleColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (itemIcon != null) {
                                        Icon(
                                            imageVector = itemIcon,
                                            contentDescription = null,
                                            tint = iconTintColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (name.isNotEmpty()) {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            color = iconTintColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.size(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (time.isNotEmpty()) {
                                        Text(
                                            text = time,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        if (itemToDelete != null) {
            DeleteElement(
                itemName = itemToDelete!!.displayName,
                onConfirm = {
                    WearDataManager.deleteItem(itemToDelete!!.rawName, itemToDelete!!.type)
                    itemToDelete = null
                },
                onDismiss = {
                    itemToDelete = null
                }
            )
        }
    }
}

@Composable
fun EmptyStateCard(text: String) {
    Card(
        onClick = { },
        enabled = false,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
    }
}