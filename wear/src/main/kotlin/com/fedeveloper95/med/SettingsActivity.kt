package com.fedeveloper95.med

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.curvedText
import com.fedeveloper95.med.services.WearDataManager
import com.fedeveloper95.med.ui.theme.MedTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearDataManager.initialize(this)

        setContent {
            MedTheme {
                val listState = rememberScalingLazyListState()
                var showOpenOnPhone by remember { mutableStateOf(false) }

                AppScaffold(
                    timeText = { TimeText() }
                ) {
                    ScreenScaffold(scrollState = listState) {
                        ScalingLazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            item {
                                ListHeader {
                                    Text(
                                        text = stringResource(R.string.settings),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            item {
                                SwitchButton(
                                    checked = WearDataManager.alarmsEnabled,
                                    onCheckedChange = {
                                        WearDataManager.alarmsEnabled = it
                                        WearDataManager.syncSettings()
                                    },
                                    label = { Text(stringResource(R.string.alarms_title)) },
                                    secondaryLabel = { Text(stringResource(R.string.alarms_desc)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                ListHeader {
                                    Text(
                                        text = stringResource(R.string.quick_actions_title),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            if (WearDataManager.availableEvents.isNotEmpty()) {
                                items(WearDataManager.availableEvents) { action ->
                                    val isSelected = WearDataManager.enabledEvents.contains(action)

                                    SwitchButton(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) WearDataManager.enabledEvents.add(action)
                                            else WearDataManager.enabledEvents.remove(action)
                                            WearDataManager.syncSettings()
                                        },
                                        label = { Text(action) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            item {
                                ListHeader {
                                    Text(
                                        text = "Social & UI",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }

                            item {
                                Card(
                                    onClick = {
                                        startActivity(Intent(this@SettingsActivity, ThemeSettingsActivity::class.java))
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "Tema",
                                            modifier = Modifier.padding(start = 40.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Card(
                                    onClick = {
                                        WearDataManager.openUrlOnPhone("https://github.com/FeDeveloper95/Med")
                                        showOpenOnPhone = true
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        Icon(
                                            imageVector = Icons.Rounded.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "GitHub",
                                            modifier = Modifier.padding(start = 32.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OpenOnPhoneDialog(
                            visible = showOpenOnPhone,
                            onDismissRequest = { showOpenOnPhone = false },
                            curvedText = {
                                curvedText(text = "Controlla il telefono")
                            }
                        )
                    }
                }
            }
        }
    }
}