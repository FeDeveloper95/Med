package com.fedeveloper95.med

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.fedeveloper95.med.services.WearDataManager
import com.fedeveloper95.med.ui.theme.MedTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearDataManager.initialize(this)

        setContent {
            MedTheme {
                val listState = rememberScalingLazyListState()
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
                        }
                    }
                }
            }
        }
    }
}