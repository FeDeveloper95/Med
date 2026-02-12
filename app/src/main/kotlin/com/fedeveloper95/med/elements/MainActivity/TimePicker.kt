@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med.elements

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fedeveloper95.med.R
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import java.time.LocalTime

@Composable
fun TimePickerSwitchable(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    initialTime: LocalTime = LocalTime.now()
) {
    val context = LocalContext.current
    val isSystem24Hour = remember { DateFormat.is24HourFormat(context) }

    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = isSystem24Hour
    )

    var showPicker by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current

    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(state.hour, state.minute))
                }
            ) {
                Text(stringResource(R.string.ok_action), fontFamily = GoogleSansFlex)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_action), fontFamily = GoogleSansFlex)
            }
        },
        toggle = {
            if (configuration.screenHeightDp > 400) {
                IconButton(onClick = { showPicker = !showPicker }) {
                    val icon = if (showPicker) Icons.Rounded.Keyboard else Icons.Rounded.Schedule
                    Icon(
                        imageVector = icon,
                        contentDescription = if (showPicker) stringResource(R.string.switch_to_text_input) else stringResource(R.string.switch_to_touch_input)
                    )
                }
            }
        }
    ) {
        if (showPicker && configuration.screenHeightDp > 400) {
            TimePicker(
                state = state,
                layoutType = TimePickerLayoutType.Vertical
            )
        } else {
            TimeInput(
                state = state,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
                .background(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    toggle()
                    Row {
                        dismissButton()
                        confirmButton()
                    }
                }
            }
        }
    }
}