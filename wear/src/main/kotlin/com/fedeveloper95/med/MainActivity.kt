package com.fedeveloper95.med

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.fedeveloper95.med.elements.MainActivity.AddEvents
import com.fedeveloper95.med.elements.MainActivity.DeleteElement
import com.fedeveloper95.med.services.WearDataManager
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.launch

data class ItemToDelete(val rawName: String, val displayName: String, val type: String, val resetAnim: () -> Unit)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearDataManager.initialize(this)
        setContent {
            MedTheme {
                val navController = rememberSwipeDismissableNavController()
                AppScaffold(
                    timeText = { TimeText() }
                ) {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                medicines = WearDataManager.medicines,
                                events = WearDataManager.eventsList,
                                onOpenAddEvents = { navController.navigate("add_events") }
                            )
                        }
                        composable("add_events") {
                            AddEvents(
                                enabledEvents = WearDataManager.enabledEvents,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(medicines: List<String>, events: List<String>, onOpenAddEvents: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    var itemToDelete by remember { mutableStateOf<ItemToDelete?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = onOpenAddEvents
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_event)
                    )
                }
            }
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item {
                    ListHeader {
                        Text(
                            text = stringResource(R.string.header_medicines),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (medicines.isEmpty()) {
                    item {
                        Card(
                            onClick = { },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.no_medicines), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(medicines, key = { "med_$it" }) { med ->
                        val cleanText = med.replace("✔", "").replace("✓", "").replace("✅", "").trim()
                        val timeRegex = Regex("^(\\d{1,2}:\\d{2})\\s*(?:-\\s*)?(.*)")
                        val match = timeRegex.find(cleanText)
                        val time = match?.groupValues?.get(1) ?: ""
                        val name = match?.groupValues?.get(2) ?: cleanText

                        val isTaken = WearDataManager.itemStates[cleanText] == true

                        SwipeableSquishItemWear(
                            onDeleteThresholdReached = { resetAnim ->
                                itemToDelete = ItemToDelete(cleanText, name, "medicine", resetAnim)
                            }
                        ) {
                            Card(
                                onClick = {
                                    WearDataManager.setItemTaken(cleanText, !isTaken, "medicine")
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = name, color = MaterialTheme.colorScheme.onSurface)
                                        if (time.isNotEmpty()) {
                                            Text(
                                                text = time,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isTaken) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isTaken) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    ListHeader {
                        Text(
                            text = stringResource(R.string.header_events),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (events.isEmpty()) {
                    item {
                        Card(
                            onClick = { },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.no_events), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(events, key = { "evt_$it" }) { ev ->
                        val cleanText = ev.replace("✔", "").replace("✓", "").replace("✅", "").trim()
                        val timeRegex = Regex("^(\\d{1,2}:\\d{2})\\s*(?:-\\s*)?(.*)")
                        val match = timeRegex.find(cleanText)
                        val time = match?.groupValues?.get(1) ?: ""
                        val name = match?.groupValues?.get(2) ?: cleanText

                        SwipeableSquishItemWear(
                            onDeleteThresholdReached = { resetAnim ->
                                itemToDelete = ItemToDelete(cleanText, name, "event", resetAnim)
                            }
                        ) {
                            Card(
                                onClick = { },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(text = name, color = MaterialTheme.colorScheme.onSurface)
                                    if (time.isNotEmpty()) {
                                        Text(
                                            text = time,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
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

                item { Spacer(modifier = Modifier.height(32.dp)) }
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
                    itemToDelete!!.resetAnim.invoke()
                    itemToDelete = null
                }
            )
        }
    }
}

@Composable
fun SwipeableSquishItemWear(
    onDeleteThresholdReached: (resetAnimation: () -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    var itemWidth by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Layout(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { itemWidth = it.width.toFloat() }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val threshold = -itemWidth * 0.5f
                        if (offsetX.value < threshold) {
                            scope.launch {
                                offsetX.animateTo(-itemWidth, spring(stiffness = Spring.StiffnessMedium))
                                val resetAnim = { scope.launch { offsetX.animateTo(0f) } }
                                onDeleteThresholdReached { resetAnim.invoke() }
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val newVal = (offsetX.value + dragAmount).coerceIn(-itemWidth, 0f)
                            offsetX.snapTo(newVal)
                        }
                    }
                )
            },
        content = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFF2B8B5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color(0xFF601410)
                )
            }
            Box { content() }
        }
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val contentPlaceable = measurables[1].measure(constraints)
        val height = contentPlaceable.height

        val redBoxWidth = (-offsetX.value).toInt().coerceAtLeast(0)
        val redPlaceable = measurables[0].measure(
            Constraints(
                minWidth = redBoxWidth,
                maxWidth = redBoxWidth,
                minHeight = height,
                maxHeight = height
            )
        )

        layout(totalWidth, height) {
            contentPlaceable.place(offsetX.value.toInt(), 0)
            redPlaceable.place(totalWidth - redBoxWidth, 0)
        }
    }
}