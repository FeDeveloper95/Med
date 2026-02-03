@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.MainActivity

import android.annotation.SuppressLint
import android.graphics.Color.parseColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fedeveloper95.med.*
import com.fedeveloper95.med.R
import com.fedeveloper95.med.elements.TimePickerSwitchable
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

@SuppressLint("NewApi")
@Composable
fun MedicinePopup(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, List<LocalTime>, List<DayOfWeek>?) -> Unit,
    initialText: String = ""
) {
    var text by remember { mutableStateOf(initialText) }
    var frequencyCountStr by remember { mutableStateOf("1") }
    var frequencyUnit by remember { mutableStateOf("Day") }
    var expandedUnit by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf(LocalDate.now().dayOfWeek)) }
    var selectedTimes by remember { mutableStateOf(listOf(LocalTime.now())) }

    var selectedIconName by remember { mutableStateOf("MedicalServices") }
    var selectedColor by remember { mutableStateOf("dynamic") }
    var showIconPicker by remember { mutableStateOf(false) }
    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initialText.isEmpty()) focusRequester.requestFocus() }

    LaunchedEffect(frequencyCountStr, frequencyUnit) {
        if (frequencyUnit == "Day") {
            val count = frequencyCountStr.toIntOrNull() ?: 1
            if (count > 0 && count != selectedTimes.size) {
                val newTimes = selectedTimes.toMutableList()
                if (count > newTimes.size) {
                    repeat(count - newTimes.size) { newTimes.add(LocalTime.now()) }
                } else {
                    while (newTimes.size > count) newTimes.removeLast()
                }
                selectedTimes = newTimes
            }
        } else {
            if (selectedTimes.isEmpty()) selectedTimes = listOf(LocalTime.now())
            if (selectedTimes.size > 1) selectedTimes = listOf(selectedTimes.first())

            val limit = frequencyCountStr.toIntOrNull()
            if (limit != null && selectedDays.size > limit) {
                selectedDays = selectedDays.toList().take(limit).toSet()
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(currentIcon = selectedIconName, onDismiss = { showIconPicker = false }, onIconSelected = { selectedIconName = it; showIconPicker = false })
    }

    if (showTimePickerForIndex != null) {
        val index = showTimePickerForIndex!!
        val initialTime = selectedTimes.getOrElse(index) { LocalTime.now() }

        TimePickerSwitchable(
            onDismiss = { showTimePickerForIndex = null },
            onConfirm = { newTime ->
                val newTimes = selectedTimes.toMutableList()
                if (index < newTimes.size) newTimes[index] = newTime else newTimes.add(newTime)
                selectedTimes = newTimes
                showTimePickerForIndex = null
            },
            initialTime = initialTime
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btnMorph"
    )

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        title = { Text(text = stringResource(R.string.new_medicine_title), fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val iconVector = when (selectedIconName) {
                        "MixtureMed" -> icSick
                        "Bed" -> icMind
                        "Mood" -> icMixture
                        else -> AVAILABLE_ICONS[selectedIconName] ?: Icons.Rounded.Event
                    }
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
                Text(text = stringResource(R.string.frequency_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = frequencyCountStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) frequencyCountStr = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedUnit,
                        onExpandedChange = { expandedUnit = !expandedUnit },
                        modifier = Modifier.weight(2f)
                    ) {
                        val dayLabel = stringResource(R.string.frequency_unit_day)
                        val weekLabel = stringResource(R.string.frequency_unit_week)
                        OutlinedTextField(
                            value = if (frequencyUnit == "Day") dayLabel else weekLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnit) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
                        )
                        ExposedDropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }) {
                            DropdownMenuItem(text = { Text(dayLabel, fontFamily = GoogleSansFlex) }, onClick = { frequencyUnit = "Day"; expandedUnit = false })
                            DropdownMenuItem(text = { Text(weekLabel, fontFamily = GoogleSansFlex) }, onClick = { frequencyUnit = "Week"; expandedUnit = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (frequencyUnit == "Week") {
                    Text(text = stringResource(R.string.days_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                        days.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                            val content = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .clickable {
                                        val limit = frequencyCountStr.toIntOrNull() ?: 7
                                        selectedDays = if (isSelected) {
                                            if (selectedDays.size > 1) selectedDays - day else selectedDays
                                        } else {
                                            if (selectedDays.size < limit) selectedDays + day else selectedDays
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = content,
                                    fontFamily = GoogleSansFlex
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TimeSelectorItem(label = stringResource(R.string.time_label), time = selectedTimes.firstOrNull() ?: LocalTime.now()) { showTimePickerForIndex = 0 }
                } else {
                    Text(text = stringResource(R.string.times_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedTimes.forEachIndexed { index, time ->
                        TimeSelectorItem(label = stringResource(R.string.schedule_label_format, index + 1), time = time) { showTimePickerForIndex = index }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = stringResource(R.string.select_color), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        val days = if (frequencyUnit == "Week") selectedDays.toList() else null
                        onConfirm(text, selectedIconName, selectedColor, selectedTimes, days)
                    }
                },
                shape = RoundedCornerShape(cornerPercent),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.save_action),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = { ExpressiveTextButton(onClick = onDismiss, text = stringResource(R.string.cancel_action)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(32.dp), tonalElevation = 6.dp
    )
}