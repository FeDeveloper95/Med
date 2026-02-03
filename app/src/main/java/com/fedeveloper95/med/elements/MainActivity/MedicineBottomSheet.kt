@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.fedeveloper95.med.elements.MainActivity

import android.annotation.SuppressLint
import android.graphics.Color.parseColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
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
import com.fedeveloper95.med.*
import com.fedeveloper95.med.R
import com.fedeveloper95.med.elements.TimePickerSwitchable
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.darken
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

@SuppressLint("NewApi")
@Composable
fun MedicineBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, List<LocalTime>, List<DayOfWeek>?) -> Unit,
    initialText: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

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
        IconPickerDialog(
            currentIcon = selectedIconName,
            onDismiss = { showIconPicker = false },
            onIconSelected = { selectedIconName = it; showIconPicker = false }
        )
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

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val saveInteractionSource = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
    val isSavePressed by saveInteractionSource.collectIsPressedAsState()

    val cancelWeight by animateFloatAsState(
        targetValue = if (isCancelPressed) 1.2f else if (isSavePressed) 0.8f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cancelWeight"
    )
    val saveWeight by animateFloatAsState(
        targetValue = if (isSavePressed) 1.2f else if (isCancelPressed) 0.8f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "saveWeight"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Box(
                modifier = Modifier
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {}
                    .padding(vertical = 10.dp)
            ) {
                BottomSheetDefaults.DragHandle()
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.new_medicine_title),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val iconVector = when (selectedIconName) {
                    "MixtureMed" -> icSick
                    "Bed" -> icMind
                    "Mood" -> icMixture
                    else -> AVAILABLE_ICONS[selectedIconName] ?: Icons.Rounded.Event
                }
                val headerBg = if (selectedColor == "dynamic") MaterialTheme.colorScheme.surfaceVariant else try { Color(parseColor(selectedColor)) } catch(e:Exception) { MaterialTheme.colorScheme.surfaceVariant }
                val headerTint = if (selectedColor == "dynamic") MaterialTheme.colorScheme.primary else Color.Black.copy(0.7f)

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(headerBg)
                        .clickable { showIconPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = headerTint
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset((-4).dp, (-4).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.name_hint), fontFamily = GoogleSansFlex) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.frequency_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = frequencyCountStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) frequencyCountStr = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
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
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedUnit,
                            onDismissRequest = { expandedUnit = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            DropdownMenuItem(text = { Text(dayLabel, fontFamily = GoogleSansFlex) }, onClick = { frequencyUnit = "Day"; expandedUnit = false })
                            DropdownMenuItem(text = { Text(weekLabel, fontFamily = GoogleSansFlex) }, onClick = { frequencyUnit = "Week"; expandedUnit = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (frequencyUnit == "Week") {
                Text(
                    text = stringResource(R.string.days_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    overflowIndicator = { state ->
                        IconButton(onClick = { state.show() }) {
                            Icon(Icons.Rounded.MoreHoriz, contentDescription = "More")
                        }
                    }
                ) {
                    val days = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

                    days.forEach { day ->
                        val isSelected = selectedDays.contains(day)

                        toggleableItem(
                            checked = isSelected,
                            label = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            onCheckedChange = { _ ->
                                val limit = frequencyCountStr.toIntOrNull() ?: 7
                                selectedDays = if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays - day else selectedDays
                                } else {
                                    if (selectedDays.size < limit) selectedDays + day else selectedDays
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                TimeSelectorItem(label = stringResource(R.string.time_label), time = selectedTimes.firstOrNull() ?: LocalTime.now()) { showTimePickerForIndex = 0 }
            } else {
                Text(
                    text = stringResource(R.string.times_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                selectedTimes.forEachIndexed { index, time ->
                    TimeSelectorItem(label = stringResource(R.string.schedule_label_format, index + 1), time = time) { showTimePickerForIndex = index }
                    if (index < selectedTimes.lastIndex) Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.select_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                                Color(parseColor(colorCode))
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
                                .then(if (isDynamic && isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
                                .border(borderWidth, borderColor, RoundedCornerShape(percent = cornerPercent))
                                .clickable(interactionSource = interactionSource, indication = null) { selectedColor = colorCode },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDynamic) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.weight(cancelWeight).height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    interactionSource = cancelInteractionSource
                ) {
                    Text(stringResource(R.string.cancel_action), fontFamily = GoogleSansFlex, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }

                Button(
                    onClick = {
                        if (text.isNotBlank()) {
                            val days = if (frequencyUnit == "Week") selectedDays.toList() else null
                            onConfirm(text, selectedIconName, selectedColor, selectedTimes, days)
                        }
                    },
                    modifier = Modifier.weight(saveWeight).height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    interactionSource = saveInteractionSource
                ) {
                    Text(stringResource(R.string.save_action), fontFamily = GoogleSansFlex, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}