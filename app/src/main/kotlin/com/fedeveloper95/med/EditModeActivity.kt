@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.fedeveloper95.med

import android.content.Context
import android.content.Intent
import android.graphics.Color.parseColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fedeveloper95.med.elements.EditModeActivity.GroupNameBottomSheet
import com.fedeveloper95.med.elements.EditModeActivity.SavePopup
import com.fedeveloper95.med.services.DataRepository
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Collections

class EditModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val epochDay = intent.getLongExtra("SELECTED_DATE", LocalDate.now().toEpochDay())
        val selectedDate = LocalDate.ofEpochDay(epochDay)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("med_settings", MODE_PRIVATE) }
            val currentTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)

            MedTheme(themeOverride = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditModeScreen(
                        selectedDate = selectedDate,
                        isExpandedScreen = isExpandedScreen,
                        onFinish = { finish() }
                    )
                }
            }
        }
    }
}

fun cleanEmptyDividers(list: List<MedData>): List<MedData> {
    var currentList = list
    var changed = true
    while (changed) {
        changed = false
        val toRemove = currentList.filterIndexed { i, current ->
            current.iconName == "DIVIDER" && (currentList.getOrNull(i + 1) == null || currentList.getOrNull(i + 1)?.iconName == "DIVIDER")
        }
        if (toRemove.isNotEmpty()) {
            currentList = currentList.filterNot { it in toRemove }
            changed = true
        }
    }
    return currentList
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditModeScreen(selectedDate: LocalDate, isExpandedScreen: Boolean, onFinish: () -> Unit) {
    val context = LocalContext.current
    var allItems by remember { mutableStateOf(DataRepository.loadData(context)) }

    var pageItems by remember {
        mutableStateOf(
            cleanEmptyDividers(
                allItems.filter { item ->
                    when (item.type) {
                        ItemType.Event -> item.creationDate == selectedDate
                        ItemType.Medicine -> {
                            val isAfterStart = !selectedDate.isBefore(item.creationDate)
                            val isBeforeEnd = item.endDate == null || !selectedDate.isAfter(item.endDate)
                            val isCorrectDay = item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(selectedDate.dayOfWeek)
                            val isCorrectGap = item.intervalGap == null || ChronoUnit.DAYS.between(item.creationDate, selectedDate) % item.intervalGap == 0L
                            isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                        }
                    }
                }.sortedWith(compareBy({ it.displayOrder }, { it.creationTime }))
            )
        )
    }

    val initialPageItems = remember { pageItems.toList() }
    val hasChanges = pageItems != initialPageItems

    var showSavePopup by remember { mutableStateOf(false) }
    var showGroupNameSheet by remember { mutableStateOf(false) }
    var isDeleteMode by rememberSaveable { mutableStateOf(false) }
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }

    BackHandler(enabled = hasChanges) {
        showSavePopup = true
    }

    fun saveOrder() {
        val finalPageItems = cleanEmptyDividers(pageItems)
        val updatedAllItems = allItems.toMutableList()

        val dividersOnDate = updatedAllItems.filter { it.iconName == "DIVIDER" && it.creationDate == selectedDate }
        val finalDividerIds = finalPageItems.filter { it.iconName == "DIVIDER" }.map { it.id }
        val dividersToDelete = dividersOnDate.filter { it.id !in finalDividerIds }
        updatedAllItems.removeAll(dividersToDelete)

        finalPageItems.forEachIndexed { newIndex, item ->
            val globalIndex = updatedAllItems.indexOfFirst { it.id == item.id }
            if (globalIndex != -1) {
                updatedAllItems[globalIndex] = updatedAllItems[globalIndex].copy(displayOrder = newIndex)
            } else if (item.iconName == "DIVIDER") {
                updatedAllItems.add(item.copy(displayOrder = newIndex))
            }
        }
        DataRepository.saveData(context, updatedAllItems)

        val prefs = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_SORT_ORDER, "custom").apply()

        context.sendBroadcast(Intent("com.fedeveloper95.med.REFRESH_DATA").setPackage(context.packageName))
        onFinish()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                .then(if (isExpandedScreen) Modifier.padding(horizontal = 64.dp) else Modifier)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveIconButton(
                        onClick = {
                            if (hasChanges) showSavePopup = true else onFinish()
                        },
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cancel_action),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (pageItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_no_cards),
                            contentDescription = null,
                            modifier = Modifier.size(128.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.nothing_here_yet),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
                var draggedItemOffset by remember { mutableFloatStateOf(0f) }
                val density = LocalDensity.current
                val itemHeightPx = with(density) { 80.dp.toPx() }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .floatingToolbarVerticalNestedScroll(
                            expanded = toolbarExpanded,
                            onExpand = { toolbarExpanded = true },
                            onCollapse = { toolbarExpanded = false }
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(items = pageItems, key = { _, item -> item.id }) { index, item ->
                        val isDivider = item.iconName == "DIVIDER"
                        val isFirstInGroup = index == 0 || pageItems.getOrNull(index - 1)?.iconName == "DIVIDER"
                        val isLastInGroup = index == pageItems.lastIndex || pageItems.getOrNull(index + 1)?.iconName == "DIVIDER"

                        val topRadius = if (isFirstInGroup) 20.dp else 4.dp
                        val bottomRadius = if (isLastInGroup) 20.dp else 4.dp
                        val shape = RoundedCornerShape(
                            topStart = topRadius,
                            topEnd = topRadius,
                            bottomStart = bottomRadius,
                            bottomEnd = bottomRadius
                        )

                        val isDragging = index == draggingItemIndex
                        val offset = if (isDragging) draggedItemOffset else 0f
                        val animatedOffset by animateFloatAsState(targetValue = offset, label = "dragOffset")

                        val dragModifier = if (!isDeleteMode) {
                            Modifier.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingItemIndex = index
                                        draggedItemOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggedItemOffset += dragAmount.y

                                        val currentDraggingIdx = draggingItemIndex ?: return@detectDragGesturesAfterLongPress

                                        if (draggedItemOffset > itemHeightPx && currentDraggingIdx < pageItems.lastIndex) {
                                            val newList = pageItems.toMutableList()
                                            Collections.swap(newList, currentDraggingIdx, currentDraggingIdx + 1)
                                            pageItems = newList
                                            draggingItemIndex = currentDraggingIdx + 1
                                            draggedItemOffset -= itemHeightPx
                                        } else if (draggedItemOffset < -itemHeightPx && currentDraggingIdx > 0) {
                                            val newList = pageItems.toMutableList()
                                            Collections.swap(newList, currentDraggingIdx, currentDraggingIdx - 1)
                                            pageItems = newList
                                            draggingItemIndex = currentDraggingIdx - 1
                                            draggedItemOffset += itemHeightPx
                                        }
                                    },
                                    onDragEnd = {
                                        draggingItemIndex = null
                                        draggedItemOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingItemIndex = null
                                        draggedItemOffset = 0f
                                    }
                                )
                            }
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = animatedOffset }
                                .animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                .then(dragModifier)
                        ) {
                            if (isDivider) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (isDeleteMode) {
                                        IconButton(onClick = {
                                            pageItems = cleanEmptyDividers(pageItems.filter { it.id != item.id })
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = stringResource(R.string.delete_mode_action),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.DragHandle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            } else {
                                EditMedDataCard(
                                    item = item,
                                    currentViewDate = selectedDate,
                                    shape = shape,
                                    isDeleteMode = isDeleteMode,
                                    onDeleteClick = {
                                        pageItems = cleanEmptyDividers(pageItems.filter { it.id != item.id })
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(140.dp)) }
                }
            }
        }

        if (pageItems.isNotEmpty()) {
            HorizontalFloatingToolbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                    .zIndex(1f),
                expanded = toolbarExpanded,
                leadingContent = {
                    IconButton(onClick = { saveOrder() }) {
                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.save_action))
                    }
                },
                trailingContent = {
                    IconButton(onClick = { isDeleteMode = !isDeleteMode }) {
                        val icon = if (isDeleteMode) Icons.Rounded.Close else Icons.Rounded.Delete
                        Icon(icon, contentDescription = stringResource(R.string.delete_mode_action))
                    }
                },
                content = {
                    FilledIconButton(
                        modifier = Modifier.width(64.dp),
                        onClick = { showGroupNameSheet = true }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_divider))
                    }
                }
            )
        }
    }

    if (showGroupNameSheet) {
        GroupNameBottomSheet(
            onDismiss = { showGroupNameSheet = false },
            onConfirm = { groupName ->
                showGroupNameSheet = false
                val newDivider = MedData(
                    id = System.nanoTime(),
                    groupId = System.currentTimeMillis(),
                    type = ItemType.Event,
                    title = groupName,
                    iconName = "DIVIDER",
                    colorCode = "dynamic",
                    frequencyLabel = "",
                    creationDate = selectedDate,
                    creationTime = LocalTime.MIN,
                    takenHistory = hashMapOf(),
                    recurrenceDays = null,
                    endDate = null,
                    notes = null,
                    displayOrder = pageItems.size,
                    intervalGap = null,
                    category = null
                )
                pageItems = pageItems + newDivider
            }
        )
    }

    if (showSavePopup) {
        SavePopup(
            onDismiss = { showSavePopup = false },
            onDiscard = {
                showSavePopup = false
                onFinish()
            },
            onSave = {
                showSavePopup = false
                saveOrder()
            }
        )
    }
}

@Composable
fun EditMedDataCard(
    item: MedData,
    currentViewDate: LocalDate,
    shape: Shape,
    isDeleteMode: Boolean,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isMedicine = item.type == ItemType.Medicine
    LocalDate.now() == currentViewDate

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val icon = when (item.iconName) {
        "MixtureMed" -> icSick
        "Bed" -> icMind
        "Mood" -> icMixture
        else -> if (item.iconName != null && AVAILABLE_ICONS.containsKey(item.iconName)) AVAILABLE_ICONS[item.iconName]!!
        else if (isMedicine) Icons.Rounded.MedicalServices
        else Icons.Rounded.Event
    }

    val customColor = remember(item.colorCode) {
        if (item.colorCode != null && item.colorCode != "dynamic") try {
            Color(parseColor(item.colorCode))
        } catch (e: Exception) {
            null
        } else null
    }

    val cardContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val cardContentColor = MaterialTheme.colorScheme.onSurface
    val iconBoxColor = customColor ?: if (isMedicine) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val iconBoxTintColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else if (isMedicine) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .alpha(if (isDeleteMode) 0.38f else 1f),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cardContainerColor, contentColor = cardContentColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    item.title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            },
            supportingContent = {
                Column {
                    if (isMedicine && !item.frequencyLabel.isNullOrBlank()) {
                        Text(
                            item.frequencyLabel,
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.bodySmall,
                            color = cardContentColor.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val scheduledTime = item.creationTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        Icon(
                            Icons.Rounded.Schedule,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = cardContentColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.status_scheduled_format, scheduledTime),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cardContentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBoxColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconBoxTintColor, modifier = Modifier.size(24.dp))
                }
            },
            trailingContent = {
                if (isDeleteMode) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.delete_mode_action),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            },
            modifier = Modifier.padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}