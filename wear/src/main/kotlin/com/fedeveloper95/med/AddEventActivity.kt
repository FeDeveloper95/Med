package com.fedeveloper95.med

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.fedeveloper95.med.services.WearDataManager
import com.fedeveloper95.med.ui.theme.MedTheme

class AddEventActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedTheme {
                AppScaffold(
                    timeText = { TimeText() }
                ) {
                    AddEventsScreen(
                        enabledEvents = WearDataManager.enabledEvents,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun AddEventsScreen(enabledEvents: List<String>, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = listState) {
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
                        text = stringResource(R.string.add_event),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (enabledEvents.isEmpty()) {
                item {
                    Card(
                        onClick = onBack,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.no_events_enabled),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            } else {
                items(enabledEvents) { event ->
                    var foundIcon = "Event"
                    var foundColor = "dynamic"

                    val detail = WearDataManager.presetDetails.find { it.startsWith("$event|") }
                    if (detail != null) {
                        val parts = detail.split("|")
                        foundIcon = parts.getOrNull(1) ?: "Event"
                        foundColor = parts.getOrNull(2) ?: "dynamic"
                    } else {
                        val matchingEvent = WearDataManager.eventsList.find { it.title.equals(event, ignoreCase = true) }
                        val matchingMed = WearDataManager.medicines.find { it.title.equals(event, ignoreCase = true) }

                        if (matchingEvent != null) {
                            foundIcon = matchingEvent.iconName
                            foundColor = matchingEvent.colorCode
                        } else if (matchingMed != null) {
                            foundIcon = matchingMed.iconName
                            foundColor = matchingMed.colorCode
                        }
                    }

                    val itemIcon = getIconForName(foundIcon)
                    val customColor = remember(foundColor) { parseCustomColor(foundColor) }
                    val circleColor = customColor ?: MaterialTheme.colorScheme.primaryContainer
                    val iconTintColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimaryContainer

                    Card(
                        onClick = {
                            WearDataManager.sendEventToPhone(event)
                            onBack()
                        },
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
                                } else if (event.isNotEmpty()) {
                                    Text(
                                        text = event.take(1).uppercase(),
                                        color = iconTintColor,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.size(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}