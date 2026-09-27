@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalTextApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)

package com.fedeveloper95.med.elements.MainActivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color.parseColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
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
import com.fedeveloper95.med.elements.TimePicker
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.ExposedDropdownMenu

@SuppressLint("NewApi")
@Composable
fun MedicineBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, List<LocalTime>, List<DayOfWeek>?, String?, Int?, Int, Long?, Long?, Int, Double?, String?, Double?) -> Unit,
    initialItem: MedData? = null,
    initialText: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("med_custom_types_prefs", Context.MODE_PRIVATE) }

    fun loadCustomTypes(): List<String> {
        val jsonStr = prefs.getString("custom_types_list", null) ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveCustomTypes(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString("custom_types_list", arr.toString()).apply()
    }

    val typeMedicineStr = stringResource(R.string.type_medicine)
    val typeSupplementStr = stringResource(R.string.type_supplement)
    val typeLotionStr = stringResource(R.string.type_lotion)
    val typeSyrupStr = stringResource(R.string.type_syrup)
    val typeInjectionStr = stringResource(R.string.type_injection)
    val typeCustomStr = stringResource(R.string.type_custom)

    val baseTypeOptions = listOf(
        typeMedicineStr,
        typeSupplementStr,
        typeLotionStr,
        typeSyrupStr,
        typeInjectionStr
    )

    var customTypes by remember { mutableStateOf(loadCustomTypes()) }
    val allTypeOptions: List<String> = baseTypeOptions + customTypes + typeCustomStr

    val initialTypeIndex = initialItem?.medicineType ?: 0

    var expandedTitleType by remember { mutableStateOf(false) }
    var selectedTypeIndex by remember { mutableIntStateOf(initialTypeIndex) }
    var customTitleText by remember { mutableStateOf("") }
    var isCustomTypeActive by remember { mutableStateOf(false) }

    var text by remember { mutableStateOf(initialItem?.title ?: initialText) }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    val formatDouble: (Double?) -> String = { d ->
        if (d == null) ""
        else if (d == d.toLong().toDouble()) d.toLong().toString()
        else d.toString()
    }

    var dose by remember { mutableStateOf(formatDouble(initialItem?.dose)) }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "") }
    var totalQuantity by remember { mutableStateOf(formatDouble(initialItem?.totalQuantity)) }
    var expandedUnit by remember { mutableStateOf(false) }

    var frequencyType by remember {
        mutableIntStateOf(
            when {
                initialItem?.intervalGap != null -> 2
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
                initGap != null && initGap > 0 && initGap % 30 == 0 -> 3
                initGap != null && initGap > 0 && initGap % 7 == 0 -> 2
                initGap != null && initGap > 0 -> 1
                else -> 0
            }
        )
    }

    var intervalDays by remember {
        mutableStateOf(
            when {
                initGap != null && initGap > 0 && initGap % 30 == 0 -> (initGap / 30).toString()
                initGap != null && initGap > 0 && initGap % 7 == 0 -> (initGap / 7).toString()
                initGap != null && initGap > 0 -> initGap.toString()
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

    val performSave = { start: Long?, end: Long? ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                val finalTimes: List<LocalTime>
                val finalDays: List<DayOfWeek>?
                val finalGap: Int?

                if (frequencyType == 2 && intervalUnit == 0) {
                    finalGap = null
                    finalDays = null

                    val base = intervalDays.toIntOrNull() ?: 2
                    val safeGap = if (base <= 0) 1 else base
                    val maxTimes = (24 / safeGap).coerceAtMost(12)
                    val calcTimes = mutableListOf<LocalTime>()
                    var currTime = selectedTimes.firstOrNull() ?: LocalTime.now()

                    for (i in 0 until maxTimes) {
                        calcTimes.add(currTime)
                        currTime = currTime.plusHours(safeGap.toLong())
                    }
                    finalTimes = calcTimes.sorted()
                } else {
                    finalTimes = selectedTimes
                    finalDays = if (frequencyType == 1) selectedDays.toList() else null
                    finalGap = if (frequencyType == 2) {
                        val base = intervalDays.toIntOrNull() ?: 2
                        when (intervalUnit) {
                            1 -> base
                            2 -> base * 7
                            3 -> base * 30
                            else -> base
                        }
                    } else null
                }

                val doseVal = dose.replace(",", ".").toDoubleOrNull()
                val totalQuantityVal = totalQuantity.replace(",", ".").toDoubleOrNull()

                onConfirm(
                    text,
                    selectedIconName,
                    selectedColor,
                    finalTimes,
                    finalDays,
                    notes.takeIf { it.isNotBlank() },
                    finalGap,
                    notificationType,
                    start,
                    end,
                    selectedTypeIndex,
                    doseVal,
                    unit.takeIf { it.isNotBlank() },
                    totalQuantityVal
                )
            }
        }
    }

    if (showSaveFrequencyPopup) {
        SaveFrequencyPopup(
            onDismiss = { showSaveFrequencyPopup = false },
            onApply = { start, end ->
                showSaveFrequencyPopup = false
                performSave(start, end)
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
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cancelCorner"
    )
    val saveCorner by animateIntAsState(
        targetValue = if (isSavePressed) 15 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
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
                    val editPrefixStr = stringResource(R.string.edit_prefix)
                    val newPrefixStr = stringResource(R.string.new_prefix)
                    val isEdit = initialItem != null
                    val prefix = if (isEdit) editPrefixStr else newPrefixStr

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = prefix,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        ExposedDropdownMenuBox(
                            expanded = expandedTitleType,
                            onExpandedChange = { expandedTitleType = it }
                        ) {
                            OutlinedTextField(
                                value = allTypeOptions.getOrElse(selectedTypeIndex) { typeMedicineStr },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTitleType)
                                },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .widthIn(min = 140.dp, max = 220.dp),
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold
                                ),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = expandedTitleType,
                                onDismissRequest = { expandedTitleType = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                allTypeOptions.forEachIndexed { index, selectionOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                selectionOption,
                                                fontFamily = GoogleSansFlex
                                            )
                                        },
                                        onClick = {
                                            selectedTypeIndex = index
                                            expandedTitleType = false
                                        }
                                    )
                                }
                            }
                        }
                    }
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
                            if (selectedColor == "dynamic") MaterialTheme.colorScheme.primary else try {
                                Color(parseColor(selectedColor))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                        val headerTint =
                            if (selectedColor == "dynamic") MaterialTheme.colorScheme.onPrimary else Color.Black.copy(
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = dose,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                                    dose = it
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.dose_label), fontFamily = GoogleSansFlex) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        val defaultUnits = listOf(
                            stringResource(R.string.unit_mg),
                            stringResource(R.string.unit_g),
                            stringResource(R.string.unit_ml),
                            stringResource(R.string.unit_drops),
                            stringResource(R.string.unit_pills),
                            stringResource(R.string.unit_pieces)
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedUnit,
                            onExpandedChange = { expandedUnit = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                readOnly = true,
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                placeholder = { Text(stringResource(R.string.unit_label), fontFamily = GoogleSansFlex) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnit) },
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = expandedUnit,
                                onDismissRequest = { expandedUnit = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                defaultUnits.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u, fontFamily = GoogleSansFlex) },
                                        onClick = {
                                            unit = u
                                            expandedUnit = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    OutlinedTextField(
                        value = totalQuantity,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' || char == ',' }) {
                                totalQuantity = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.total_quantity_label), fontFamily = GoogleSansFlex) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        suffix = {
                            if (unit.isNotBlank()) {
                                Text(text = unit, fontFamily = GoogleSansFlex)
                            }
                        }
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

                        OutlinedSingleSelectButtonGroup(
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

                        OutlinedSingleSelectButtonGroup(
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
                            val count = 1 + selectedTimes.size
                            val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                            ) {
                                SegmentedListItem(
                                    onClick = {},
                                    colors = itemColors,
                                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = count),
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (timesPerDay > 1) timesPerDay-- }) {
                                                Icon(Icons.Rounded.Remove, contentDescription = null)
                                            }
                                            Text(
                                                text = timesPerDay.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(onClick = { if (timesPerDay < 10) timesPerDay++ }) {
                                                Icon(Icons.Rounded.Add, contentDescription = null)
                                            }
                                        }
                                    },
                                    content = {
                                        Text(
                                            text = stringResource(R.string.times_per_day_label),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontFamily = GoogleSansFlex
                                        )
                                    }
                                )

                                selectedTimes.forEachIndexed { index, time ->
                                    SegmentedListItem(
                                        onClick = { showTimePickerForIndex = index },
                                        colors = itemColors,
                                        shapes = ListItemDefaults.segmentedShapes(index = index + 1, count = count),
                                        trailingContent = {
                                            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                            Text(
                                                text = time.format(formatter),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontFamily = GoogleSansFlex
                                            )
                                        },
                                        content = {
                                            Text(
                                                text = stringResource(R.string.schedule_label_format, index + 1),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontFamily = GoogleSansFlex
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
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
                            val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            SegmentedListItem(
                                onClick = { showTimePickerForIndex = 0 },
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                                colors = itemColors,
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                                trailingContent = {
                                    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                    Text(
                                        text = (selectedTimes.firstOrNull() ?: LocalTime.now()).format(formatter),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = GoogleSansFlex
                                    )
                                },
                                content = {
                                    Text(
                                        text = stringResource(R.string.time_label),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = GoogleSansFlex
                                    )
                                }
                            )
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
                                    onExpandedChange = { expandedInterval = it },
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    val options = listOf(
                                        stringResource(R.string.interval_hours),
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
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
                            val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            SegmentedListItem(
                                onClick = { showTimePickerForIndex = 0 },
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                                colors = itemColors,
                                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
                                trailingContent = {
                                    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                                    Text(
                                        text = (selectedTimes.firstOrNull() ?: LocalTime.now()).format(formatter),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = GoogleSansFlex
                                    )
                                },
                                content = {
                                    Text(
                                        text = stringResource(R.string.time_label),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = GoogleSansFlex
                                    )
                                }
                            )
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
                                    val initialFreqType = when {
                                        initialItem.intervalGap != null -> 2
                                        initialItem.recurrenceDays != null -> 1
                                        else -> 0
                                    }

                                    val effectivelyDaily = (frequencyType == 2 && intervalUnit == 0)
                                    val initialMedType = initialItem.medicineType

                                    val doseVal = dose.replace(",", ".").toDoubleOrNull()
                                    val totalQuantityVal = totalQuantity.replace(",", ".").toDoubleOrNull()

                                    val isModified = selectedTypeIndex != initialMedType ||
                                            text != initialItem.title ||
                                            notes != (initialItem.notes ?: "") ||
                                            selectedIconName != (initialItem.iconName ?: "MedicalServices") ||
                                            selectedColor != (initialItem.colorCode ?: "dynamic") ||
                                            notificationType != initialItem.notificationType ||
                                            effectivelyDaily ||
                                            frequencyType != initialFreqType ||
                                            doseVal != initialItem.dose ||
                                            unit.takeIf { it.isNotBlank() } != initialItem.unit ||
                                            totalQuantityVal != initialItem.totalQuantity

                                    if (isModified) {
                                        showSaveFrequencyPopup = true
                                    } else {
                                        performSave(null, null)
                                    }
                                } else {
                                    performSave(null, null)
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
fun OutlinedSingleSelectButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            OutlinedToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { onOptionSelected(index) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = OutlinedToggleButtonDefaults.colors(
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
            OutlinedToggleButton(
                checked = selectedIndices.contains(index),
                onCheckedChange = { onOptionSelected(index) },
                modifier = Modifier.weight(1f),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = OutlinedToggleButtonDefaults.colors(
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
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