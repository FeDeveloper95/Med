@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.MainActivity

import android.graphics.Color.parseColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.*
import com.fedeveloper95.med.R
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun EventPopup(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, List<LocalTime>, List<DayOfWeek>?) -> Unit,
    initialText: String = ""
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedTimes by remember { mutableStateOf(listOf(LocalTime.now())) }

    var selectedIconName by remember { mutableStateOf("Event") }
    var selectedColor by remember { mutableStateOf("dynamic") }
    var showIconPicker by remember { mutableStateOf(false) }
    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initialText.isEmpty()) focusRequester.requestFocus() }

    if (showIconPicker) {
        IconPickerDialog(currentIcon = selectedIconName, onDismiss = { showIconPicker = false }, onIconSelected = { selectedIconName = it; showIconPicker = false })
    }

    if (showTimePickerForIndex != null) {
        val index = showTimePickerForIndex!!
        val initialTime = selectedTimes.getOrElse(index) { LocalTime.now() }
        val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = true)
        TimePickerDialog(
            onDismissRequest = { showTimePickerForIndex = null },
            confirmButton = { ExpressiveTextButton(onClick = {
                val newTime = LocalTime.of(timeState.hour, timeState.minute)
                val newTimes = selectedTimes.toMutableList()
                if (index < newTimes.size) newTimes[index] = newTime
                selectedTimes = newTimes
                showTimePickerForIndex = null
            }, text = "OK") },
            dismissButton = { ExpressiveTextButton(onClick = { showTimePickerForIndex = null }, text = "Cancel") }
        ) { TimePicker(state = timeState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.new_event_title), fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val iconVector = AVAILABLE_ICONS[selectedIconName] ?: Icons.Rounded.Event
                    val headerBg = if (selectedColor == "dynamic") MaterialTheme.colorScheme.surfaceVariant else try { Color(parseColor(selectedColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surfaceVariant }
                    val headerTint = if (selectedColor == "dynamic") MaterialTheme.colorScheme.primary else Color.Black.copy(0.7f)
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(headerBg).clickable { showIconPicker = true }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = iconVector, contentDescription = null, modifier = Modifier.size(40.dp), tint = headerTint)
                        Box(modifier = Modifier.align(Alignment.BottomEnd).offset((-4).dp, (-4).dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.name_hint), fontFamily = GoogleSansFlex) }, singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                )

                Spacer(modifier = Modifier.height(16.dp))
                TimeSelectorItem(label = "Time", time = selectedTimes.first()) { showTimePickerForIndex = 0 }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVAILABLE_COLORS.forEach { colorCode ->
                        val isSelected = selectedColor == colorCode
                        val isDynamic = colorCode == "dynamic"
                        val bg = if (isDynamic) MaterialTheme.colorScheme.surfaceVariant else try { Color(parseColor(colorCode)) } catch(e:Exception) { Color.Gray }
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(bg).border(if (isSelected) 3.dp else 0.dp, if(isDynamic) MaterialTheme.colorScheme.primary else Color(red = bg.red * 0.7f, green = bg.green * 0.7f, blue = bg.blue * 0.7f, alpha = bg.alpha), CircleShape).clickable { selectedColor = colorCode }, contentAlignment = Alignment.Center) {
                            if (isDynamic) Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            if (isSelected && !isDynamic) Icon(Icons.Outlined.CheckCircle, null, tint = Color.Black.copy(0.5f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            ExpressiveButton(onClick = {
                if (text.isNotBlank()) {
                    onConfirm(text, selectedIconName, selectedColor, selectedTimes, null)
                }
            }, text = stringResource(R.string.save_action))
        },
        dismissButton = { ExpressiveTextButton(onClick = onDismiss, text = stringResource(R.string.cancel_action)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(32.dp), tonalElevation = 6.dp
    )
}