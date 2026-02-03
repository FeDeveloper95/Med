@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.MainActivity

import android.graphics.Color.parseColor
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Event
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
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.*
import com.fedeveloper95.med.R
import com.fedeveloper95.med.elements.TimePickerSwitchable
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.darken
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun EventBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, List<LocalTime>, List<DayOfWeek>?) -> Unit,
    initialText: String = ""
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(initialText) }
    var selectedTimes by remember { mutableStateOf(listOf(LocalTime.now())) }

    var selectedIconName by remember { mutableStateOf("Event") }
    var selectedColor by remember { mutableStateOf("dynamic") }
    var showIconPicker by remember { mutableStateOf(false) }
    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }

    val focusRequester = remember { FocusRequester() }

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
                text = stringResource(R.string.new_event_title),
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

            val time = selectedTimes.firstOrNull() ?: LocalTime.now()
            TimeSelectorItem(label = stringResource(R.string.time_label), time = time) { showTimePickerForIndex = 0 }

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
                            onConfirm(text, selectedIconName, selectedColor, selectedTimes, null)
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