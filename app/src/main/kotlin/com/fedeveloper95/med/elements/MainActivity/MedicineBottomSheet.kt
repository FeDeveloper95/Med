@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalTextApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)

package com.fedeveloper95.med.elements.MainActivity

import android.annotation.SuppressLint
import android.graphics.Color.parseColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.AVAILABLE_ICONS
import com.fedeveloper95.med.R
import com.fedeveloper95.med.SingleSelectConnectedButtonGroupWithFlowLayout
import com.fedeveloper95.med.TimeSelectorItem
import com.fedeveloper95.med.elements.TimePicker
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@SuppressLint("NewApi")
@Composable
fun MedicineBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, List<LocalTime>, List<DayOfWeek>?, String?, Int?, Int, Long?, Long?) -> Unit,
    initialItem: MedData? = null,
    initialText: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(initialItem?.title ?: initialText) }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    var frequencyType by remember {
        mutableIntStateOf(
            when {
                initialItem?.intervalGap != null && initialItem.intervalGap > 1 -> 2
                initialItem?.recurrenceDays != null -> 1
                else -> 0
            }
        )
    }

    var selectedTimes by remember {
        mutableStateOf(
            if (initialItem != null) listOf(initialItem.creationTime) else listOf(LocalTime.now())
        )
    }

    var timesPerDay by remember {
        mutableIntStateOf(if (frequencyType == 0 && initialItem != null) selectedTimes.size else 1)
    }

    val initGap = initialItem?.intervalGap
    var intervalUnit by remember {
        mutableIntStateOf(
            when {
                initGap != null && initGap % 30 == 0 -> 2
                initGap != null && initGap % 7 == 0 -> 1
                else -> 0
            }
        )
    }

    var intervalDays by remember {
        mutableStateOf(
            when {
                initGap != null && initGap % 30 == 0 -> (initGap / 30).toString()
                initGap != null && initGap % 7 == 0 -> (initGap / 7).toString()
                initGap != null -> initGap.toString()
                else -> "2"
            }
        )
    }

    var expandedInterval by remember { mutableStateOf(false) }

    var selectedDays by remember {
        mutableStateOf(
            initialItem?.recurrenceDays?.toSet() ?: setOf(LocalDate.now().dayOfWeek)
        )
    }

    var selectedIconName by remember { mutableStateOf(initialItem?.iconName ?: "MedicalServices") }
    var selectedColor by remember { mutableStateOf(initialItem?.colorCode ?: "dynamic") }
    var showIconPicker by remember { mutableStateOf(false) }
    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }
    var showSaveFrequencyPopup by remember { mutableStateOf(false) }

    var notificationType by remember { mutableIntStateOf(initialItem?.notificationType ?: 0) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(frequencyType, timesPerDay) {
        if (frequencyType == 0) {
            val count = timesPerDay
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
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = selectedIconName,
            currentColor = selectedColor,
            onDismiss = { showIconPicker = false },
            onConfirm = { icon, color ->
                selectedIconName = icon
                selectedColor = color
                showIconPicker = false
            }
        )
    }

    if (showTimePickerForIndex != null) {
        val index = showTimePickerForIndex!!
        val initialTime = selectedTimes.getOrElse(index) { LocalTime.now() }

        TimePicker(
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

    if (showSaveFrequencyPopup) {
        SaveFrequencyPopup(
            onDismiss = { showSaveFrequencyPopup = false },
            onApply = { start, end ->
                showSaveFrequencyPopup = false
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        val days = if (frequencyType == 1) selectedDays.toList() else null
                        val gap = if (frequencyType == 2) {
                            val base = intervalDays.toIntOrNull() ?: 2
                            when (intervalUnit) {
                                1 -> base * 7
                                2 -> base * 30
                                else -> base
                            }
                        } else null

                        onConfirm(
                            text,
                            selectedIconName,
                            selectedColor,
                            selectedTimes,
                            days,
                            notes.takeIf { it.isNotBlank() },
                            gap,
                            notificationType,
                            start,
                            end
                        )
                    }
                }
            }
        )
    }

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val saveInteractionSource = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
    val isSavePressed by saveInteractionSource.collectIsPressedAsState()

    val cancelCorner by animateIntAsState(
        targetValue = if (isCancelPressed) 15 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cancelCorner"
    )
    val saveCorner by animateIntAsState(
        targetValue = if (isSavePressed) 15 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "saveCorner"
    )

    val listState = rememberLazyListState()
    var wasAtTopWhenGestureStarted by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (!wasAtTopWhenGestureStarted) available else Velocity.Zero
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitFirstDown(requireUnconsumed = false)
                                wasAtTopWhenGestureStarted = !listState.canScrollBackward
                            }
                        }
                    }
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = stringResource(if (initialItem != null) R.string.edit_medicine_title else R.string.new_medicine_title),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val iconVector = when (selectedIconName) {
                            "MixtureMed" -> icSick
                            "Bed" -> icMind
                            "Mood" -> icMixture
                            else -> AVAILABLE_ICONS[selectedIconName] ?: Icons.Rounded.Event
                        }
                        val headerBg =
                            if (selectedColor == "dynamic") MaterialTheme.colorScheme.surfaceVariant else try {
                                Color(parseColor(selectedColor))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        val headerTint =
                            if (selectedColor == "dynamic") MaterialTheme.colorScheme.primary else Color.Black.copy(
                                0.7f
                            )

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
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it; nameError = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                stringResource(R.string.name_hint),
                                fontFamily = GoogleSansFlex
                            )
                        },
                        singleLine = true,
                        isError = nameError
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                stringResource(R.string.notes_hint),
                                fontFamily = GoogleSansFlex
                            )
                        },
                        minLines = 2,
                        maxLines = 4
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.notification_type_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        val notifOptions = listOf(
                            stringResource(R.string.notification_type_default),
                            stringResource(R.string.notification_type_none),
                            stringResource(R.string.notification_type_normal),
                            stringResource(R.string.notification_type_alarm)
                        )

                        SingleSelectConnectedButtonGroupWithFlowLayout(
                            options = notifOptions,
                            selectedIndex = notificationType,
                            onOptionSelected = { notificationType = it }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.frequency_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        val freqOptions = listOf(
                            stringResource(R.string.freq_mode_daily),
                            stringResource(R.string.freq_mode_days),
                            stringResource(R.string.freq_mode_interval)
                        )

                        SingleSelectConnectedButtonGroupWithFlowLayout(
                            options = freqOptions,
                            selectedIndex = frequencyType,
                            onOptionSelected = { frequencyType = it }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                when (frequencyType) {
                    0 -> {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.times_per_day_label),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (timesPerDay > 1) timesPerDay-- }) {
                                        Icon(Icons.Rounded.Remove, contentDescription = null)
                                    }
                                    Text(
                                        text = timesPerDay.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    IconButton(onClick = { if (timesPerDay < 10) timesPerDay++ }) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        itemsIndexed(selectedTimes) { index, time ->
                            TimeSelectorItem(
                                label = stringResource(
                                    R.string.schedule_label_format,
                                    index + 1
                                ), time = time
                            ) { showTimePickerForIndex = index }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    1 -> {
                        item {
                            val days = listOf(
                                DayOfWeek.MONDAY,
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.FRIDAY,
                                DayOfWeek.SATURDAY,
                                DayOfWeek.SUNDAY
                            )
                            val dayLabels = days.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
                            val selectedIndices = days.mapIndexedNotNull { index, day ->
                                if (selectedDays.contains(day)) index else null
                            }.toSet()

                            MultiSelectConnectedButtonGroupWithFlowLayout(
                                options = dayLabels,
                                selectedIndices = selectedIndices,
                                onOptionSelected = { index ->
                                    val day = days[index]
                                    selectedDays = if (selectedDays.contains(day)) {
                                        if (selectedDays.size > 1) selectedDays - day else selectedDays
                                    } else {
                                        selectedDays + day
                                    }
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            TimeSelectorItem(
                                label = stringResource(R.string.time_label),
                                time = selectedTimes.firstOrNull() ?: LocalTime.now()
                            ) { showTimePickerForIndex = 0 }
                        }
                    }

                    2 -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.interval_every),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedTextField(
                                    value = intervalDays,
                                    onValueChange = {
                                        if (it.isEmpty() || it.all { char -> char.isDigit() }) intervalDays =
                                            it
                                    },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = expandedInterval,
                                    onExpandedChange = { expandedInterval = !expandedInterval },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    val options = listOf(
                                        stringResource(R.string.interval_days),
                                        stringResource(R.string.interval_weeks),
                                        stringResource(R.string.interval_months)
                                    )

                                    OutlinedTextField(
                                        value = options[intervalUnit],
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterval)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expandedInterval,
                                        onDismissRequest = { expandedInterval = false },
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        options.forEachIndexed { index, selectionOption ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        selectionOption,
                                                        fontFamily = GoogleSansFlex
                                                    )
                                                },
                                                onClick = {
                                                    intervalUnit = index
                                                    expandedInterval = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            TimeSelectorItem(
                                label = stringResource(R.string.time_label),
                                time = selectedTimes.firstOrNull() ?: LocalTime.now()
                            ) { showTimePickerForIndex = 0 }
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(cancelCorner),
                        interactionSource = cancelInteractionSource
                    ) {
                        Text(
                            stringResource(R.string.cancel_action),
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                if (initialItem != null) {
                                    val isModified = run {
                                        val initialFreqType = when {
                                            initialItem.intervalGap != null && initialItem.intervalGap > 1 -> 2
                                            initialItem.recurrenceDays != null -> 1
                                            else -> 0
                                        }
                                        val initialDaysSet = initialItem.recurrenceDays?.toSet()
                                            ?: setOf(LocalDate.now().dayOfWeek)

                                        val currentBase = intervalDays.toIntOrNull() ?: 2
                                        val currentGap = when (intervalUnit) {
                                            1 -> currentBase * 7
                                            2 -> currentBase * 30
                                            else -> currentBase
                                        }

                                        text != initialItem.title ||
                                                notes != (initialItem.notes ?: "") ||
                                                selectedIconName != (initialItem.iconName
                                            ?: "MedicalServices") ||
                                                selectedColor != (initialItem.colorCode
                                            ?: "dynamic") ||
                                                notificationType != initialItem.notificationType ||
                                                selectedTimes != listOf(initialItem.creationTime) ||
                                                frequencyType != initialFreqType ||
                                                (frequencyType == 1 && selectedDays != initialDaysSet) ||
                                                (frequencyType == 2 && currentGap != (initialItem.intervalGap
                                                    ?: 2))
                                    }

                                    if (isModified) {
                                        showSaveFrequencyPopup = true
                                    } else {
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            if (!sheetState.isVisible) {
                                                val days =
                                                    if (frequencyType == 1) selectedDays.toList() else null
                                                val gap = if (frequencyType == 2) {
                                                    val base = intervalDays.toIntOrNull() ?: 2
                                                    when (intervalUnit) {
                                                        1 -> base * 7
                                                        2 -> base * 30
                                                        else -> base
                                                    }
                                                } else null

                                                onConfirm(
                                                    text,
                                                    selectedIconName,
                                                    selectedColor,
                                                    selectedTimes,
                                                    days,
                                                    notes.takeIf { it.isNotBlank() },
                                                    gap,
                                                    notificationType,
                                                    null,
                                                    null
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            val days =
                                                if (frequencyType == 1) selectedDays.toList() else null
                                            val gap = if (frequencyType == 2) {
                                                val base = intervalDays.toIntOrNull() ?: 2
                                                when (intervalUnit) {
                                                    1 -> base * 7
                                                    2 -> base * 30
                                                    else -> base
                                                }
                                            } else null

                                            onConfirm(
                                                text,
                                                selectedIconName,
                                                selectedColor,
                                                selectedTimes,
                                                days,
                                                notes.takeIf { it.isNotBlank() },
                                                gap,
                                                notificationType,
                                                null,
                                                null
                                            )
                                        }
                                    }
                                }
                            } else {
                                nameError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(saveCorner),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        interactionSource = saveInteractionSource
                    ) {
                        Text(
                            stringResource(R.string.save_action),
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultiSelectConnectedButtonGroupWithFlowLayout(
    options: List<String>,
    selectedIndices: Set<Int>,
    onOptionSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selectedIndices.contains(index),
                onCheckedChange = { onOptionSelected(index) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    text = option,
                    fontFamily = GoogleSansFlex,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}