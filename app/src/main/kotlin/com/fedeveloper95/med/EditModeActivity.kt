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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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

sealed class EditItem {
    abstract val uniqueId: String
    data class Card(val med: MedData) : EditItem() {
        override val uniqueId = "card_${med.id}"
    }
    data class Group(val divider: MedData, val cards: List<MedData>) : EditItem() {
        override val uniqueId = "group_${divider.id}"
    }
}

fun buildEditItems(flatList: List<MedData>): List<EditItem> {
    val result = mutableListOf<EditItem>()
    var currentGroupDivider: MedData? = null
    val currentGroupCards = mutableListOf<MedData>()

    for (item in flatList) {
        if (item.iconName == "DIVIDER") {
            if (currentGroupDivider != null) {
                result.add(EditItem.Group(currentGroupDivider, currentGroupCards.toList()))
            } else if (currentGroupCards.isNotEmpty()) {
                currentGroupCards.forEach { result.add(EditItem.Card(it)) }
            }

            if (item.title.isNotBlank()) {
                currentGroupDivider = item
                currentGroupCards.clear()
            } else {
                currentGroupDivider = null
                currentGroupCards.clear()
            }
        } else {
            if (currentGroupDivider != null) {
                currentGroupCards.add(item)
            } else {
                result.add(EditItem.Card(item))
            }
        }
    }
    if (currentGroupDivider != null) {
        result.add(EditItem.Group(currentGroupDivider, currentGroupCards.toList()))
    } else if (currentGroupCards.isNotEmpty()) {
        currentGroupCards.forEach { result.add(EditItem.Card(it)) }
    }
    return result
}

@Composable
fun Modifier.draggableItem(
    id: String,
    index: Int,
    maxIndex: Int,
    draggingId: String?,
    draggedOffset: Float,
    itemHeightPx: Float,
    onDragStart: (String) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onSwap: (Int, Int) -> Unit
): Modifier {
    val currentIndex by rememberUpdatedState(index)
    val currentMaxIndex by rememberUpdatedState(maxIndex)
    val currentDraggingId by rememberUpdatedState(draggingId)
    val currentDraggedOffset by rememberUpdatedState(draggedOffset)

    return this.pointerInput(id) {
        detectDragGesturesAfterLongPress(
            onDragStart = { onDragStart(id) },
            onDrag = { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.y)

                if (currentDraggingId == id) {
                    if (currentDraggedOffset > itemHeightPx && currentIndex < currentMaxIndex) {
                        onSwap(currentIndex, currentIndex + 1)
                    } else if (currentDraggedOffset < -itemHeightPx && currentIndex > 0) {
                        onSwap(currentIndex, currentIndex - 1)
                    }
                }
            },
            onDragEnd = onDragEnd,
            onDragCancel = onDragEnd
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditModeScreen(selectedDate: LocalDate, isExpandedScreen: Boolean, onFinish: () -> Unit) {
    val context = LocalContext.current
    var allItems by remember { mutableStateOf(DataRepository.loadData(context)) }

    val initialPageItems = remember {
        allItems.filter { item ->
            when (item.type) {
                ItemType.Event -> item.creationDate == selectedDate
                ItemType.Medicine -> {
                    val isAfterStart = !selectedDate.isBefore(item.creationDate)
                    val isBeforeEnd = item.endDate == null || !selectedDate.isAfter(item.endDate)
                    val isCorrectDay = item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(selectedDate.dayOfWeek)
                    val isCorrectGap = item.intervalGap == null || java.time.temporal.ChronoUnit.DAYS.between(item.creationDate, selectedDate) % item.intervalGap == 0L
                    isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                }
            }
        }.sortedWith(compareBy({ it.displayOrder }, { it.creationTime }))
    }

    var editItems by remember { mutableStateOf(buildEditItems(initialPageItems)) }

    var showSavePopup by remember { mutableStateOf(false) }
    var showGroupNameSheet by remember { mutableStateOf(false) }
    var isDeleteMode by rememberSaveable { mutableStateOf(false) }
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }

    var topDraggingId by remember { mutableStateOf<String?>(null) }
    var topDraggedOffset by remember { mutableFloatStateOf(0f) }
    var innerDraggingId by remember { mutableStateOf<String?>(null) }
    var innerDraggedOffset by remember { mutableFloatStateOf(0f) }

    val hasChanges = remember(editItems, initialPageItems) {
        val currentFlatList = mutableListOf<MedData>()
        var lastWasGroup = false
        editItems.forEach { item ->
            when (item) {
                is EditItem.Card -> {
                    if (lastWasGroup) {
                        currentFlatList.add(MedData(
                            id = 0, groupId = 0, type = ItemType.Event, title = "", iconName = "DIVIDER",
                            colorCode = "", frequencyLabel = "", creationDate = selectedDate, creationTime = LocalTime.MIN,
                            takenHistory = hashMapOf(), recurrenceDays = null, endDate = null, notes = null,
                            displayOrder = 0, category = null, intervalGap = null
                        ))
                    }
                    currentFlatList.add(item.med)
                    lastWasGroup = false
                }
                is EditItem.Group -> {
                    currentFlatList.add(item.divider)
                    currentFlatList.addAll(item.cards)
                    lastWasGroup = true
                }
            }
        }

        val filteredCurrent = currentFlatList.filter { it.title.isNotBlank() || it.iconName != "DIVIDER" }
        val filteredInitial = initialPageItems.filter { it.title.isNotBlank() || it.iconName != "DIVIDER" }

        filteredCurrent.map { it.id } != filteredInitial.map { it.id }
    }

    BackHandler(enabled = hasChanges) {
        showSavePopup = true
    }

    fun saveOrder() {
        val flatList = mutableListOf<MedData>()
        var lastWasGroup = false

        for (item in editItems) {
            when (item) {
                is EditItem.Card -> {
                    if (lastWasGroup) {
                        flatList.add(MedData(
                            id = System.nanoTime(),
                            groupId = System.currentTimeMillis(),
                            type = ItemType.Event,
                            title = "",
                            iconName = "DIVIDER",
                            colorCode = "dynamic",
                            frequencyLabel = "",
                            creationDate = selectedDate,
                            creationTime = LocalTime.MIN,
                            takenHistory = hashMapOf(),
                            recurrenceDays = null,
                            endDate = null,
                            notes = null,
                            displayOrder = 0,
                            intervalGap = null,
                            category = null
                        ))
                    }
                    flatList.add(item.med)
                    lastWasGroup = false
                }
                is EditItem.Group -> {
                    flatList.add(item.divider)
                    flatList.addAll(item.cards)
                    lastWasGroup = true
                }
            }
        }

        val flatListIds = flatList.map { it.id }.toSet()
        val updatedAllItems = allItems.filter {
            it.creationDate != selectedDate || it.iconName != "DIVIDER" || flatListIds.contains(it.id)
        }.toMutableList()

        flatList.forEachIndexed { newIndex, item ->
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

            if (editItems.isEmpty()) {
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
                    itemsIndexed(items = editItems, key = { _, item -> item.uniqueId }) { index, editItem ->
                        val isTopDragging = topDraggingId == editItem.uniqueId
                        val topOffset = if (isTopDragging) topDraggedOffset else 0f
                        val animatedTopOffset by animateFloatAsState(targetValue = topOffset, label = "topDrag")

                        val topDragMod = if (!isDeleteMode) {
                            Modifier.draggableItem(
                                id = editItem.uniqueId,
                                index = index,
                                maxIndex = editItems.lastIndex,
                                draggingId = topDraggingId,
                                draggedOffset = topDraggedOffset,
                                itemHeightPx = itemHeightPx,
                                onDragStart = { topDraggingId = it; topDraggedOffset = 0f },
                                onDrag = { topDraggedOffset += it },
                                onDragEnd = { topDraggingId = null; topDraggedOffset = 0f },
                                onSwap = { from, to ->
                                    val newItems = editItems.toMutableList()
                                    Collections.swap(newItems, from, to)
                                    editItems = newItems
                                    topDraggedOffset += if (to > from) -itemHeightPx else itemHeightPx
                                }
                            )
                        } else Modifier

                        if (editItem is EditItem.Card && index > 0 && editItems.getOrNull(index - 1) is EditItem.Group) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Box(
                            modifier = Modifier
                                .zIndex(if (isTopDragging) 1f else 0f)
                                .graphicsLayer { translationY = animatedTopOffset }
                                .animateItem(placementSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                .then(if (editItem is EditItem.Card) topDragMod else Modifier)
                        ) {
                            when (editItem) {
                                is EditItem.Card -> {
                                    val isFirst = index == 0 || editItems.getOrNull(index - 1) is EditItem.Group
                                    val isLast = index == editItems.lastIndex || editItems.getOrNull(index + 1) is EditItem.Group
                                    val shape = RoundedCornerShape(
                                        topStart = if (isFirst) 20.dp else 4.dp,
                                        topEnd = if (isFirst) 20.dp else 4.dp,
                                        bottomStart = if (isLast) 20.dp else 4.dp,
                                        bottomEnd = if (isLast) 20.dp else 4.dp
                                    )

                                    EditMedDataCard(
                                        item = editItem.med,
                                        shape = shape,
                                        isDeleteMode = isDeleteMode,
                                        availableGroups = editItems.filterIsInstance<EditItem.Group>(),
                                        currentGroupId = null,
                                        currentGroupTitle = null,
                                        onDeleteClick = { editItems = editItems.filter { it.uniqueId != editItem.uniqueId } },
                                        onRemoveFromGroup = null,
                                        onAddToGroup = { targetGroupId ->
                                            val newItems = editItems.toMutableList()
                                            newItems.remove(editItem)
                                            val gIdx = newItems.indexOfFirst { it is EditItem.Group && it.divider.id == targetGroupId }
                                            if (gIdx != -1) {
                                                val group = newItems[gIdx] as EditItem.Group
                                                newItems[gIdx] = group.copy(cards = group.cards + editItem.med)
                                            }
                                            editItems = newItems
                                        }
                                    )
                                }
                                is EditItem.Group -> {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp, top = if (index == 0) 0.dp else 16.dp, bottom = 16.dp, end = 16.dp)
                                                .then(topDragMod),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = editItem.divider.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontFamily = GoogleSansFlex,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                                color = if (isDeleteMode) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary
                                            )
                                            if (isDeleteMode) {
                                                IconButton(onClick = {
                                                    val newItems = editItems.toMutableList()
                                                    val gIdx = newItems.indexOf(editItem)
                                                    newItems.removeAt(gIdx)
                                                    newItems.addAll(gIdx, editItem.cards.map { EditItem.Card(it) })
                                                    editItems = newItems
                                                }) {
                                                    Icon(
                                                        Icons.Rounded.Delete,
                                                        contentDescription = stringResource(R.string.delete_mode_action),
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    Icons.Rounded.DragHandle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        editItem.cards.forEachIndexed { cIdx, card ->
                                            val isInnerDragging = innerDraggingId == card.id.toString()
                                            val innerOffset = if (isInnerDragging) innerDraggedOffset else 0f
                                            val innerAnimOffset by animateFloatAsState(targetValue = innerOffset, label = "innerDrag")

                                            val innerDragMod = if (!isDeleteMode) {
                                                Modifier.draggableItem(
                                                    id = card.id.toString(),
                                                    index = cIdx,
                                                    maxIndex = editItem.cards.lastIndex,
                                                    draggingId = innerDraggingId,
                                                    draggedOffset = innerDraggedOffset,
                                                    itemHeightPx = itemHeightPx,
                                                    onDragStart = { innerDraggingId = it; innerDraggedOffset = 0f },
                                                    onDrag = { innerDraggedOffset += it },
                                                    onDragEnd = { innerDraggingId = null; innerDraggedOffset = 0f },
                                                    onSwap = { from, to ->
                                                        val gIdx = editItems.indexOf(editItem)
                                                        if (gIdx != -1) {
                                                            val group = editItems[gIdx] as EditItem.Group
                                                            val newCards = group.cards.toMutableList()
                                                            Collections.swap(newCards, from, to)
                                                            val newItems = editItems.toMutableList()
                                                            newItems[gIdx] = group.copy(cards = newCards)
                                                            editItems = newItems
                                                            innerDraggedOffset += if (to > from) -itemHeightPx else itemHeightPx
                                                        }
                                                    }
                                                )
                                            } else Modifier

                                            Box(
                                                modifier = Modifier
                                                    .padding(bottom = 2.dp)
                                                    .zIndex(if (isInnerDragging) 1f else 0f)
                                                    .graphicsLayer { translationY = innerAnimOffset }
                                                    .then(innerDragMod)
                                            ) {
                                                val shape = RoundedCornerShape(
                                                    topStart = if (cIdx == 0) 20.dp else 4.dp,
                                                    topEnd = if (cIdx == 0) 20.dp else 4.dp,
                                                    bottomStart = if (cIdx == editItem.cards.lastIndex) 20.dp else 4.dp,
                                                    bottomEnd = if (cIdx == editItem.cards.lastIndex) 20.dp else 4.dp
                                                )
                                                EditMedDataCard(
                                                    item = card,
                                                    shape = shape,
                                                    isDeleteMode = isDeleteMode,
                                                    availableGroups = emptyList(),
                                                    currentGroupId = editItem.divider.id,
                                                    currentGroupTitle = editItem.divider.title,
                                                    onDeleteClick = {
                                                        val newItems = editItems.toMutableList()
                                                        val gIdx = newItems.indexOf(editItem)
                                                        if (gIdx != -1) {
                                                            val newCards = editItem.cards.filter { it.id != card.id }
                                                            if (newCards.isEmpty()) newItems.removeAt(gIdx)
                                                            else newItems[gIdx] = editItem.copy(cards = newCards)
                                                            editItems = newItems
                                                        }
                                                    },
                                                    onRemoveFromGroup = {
                                                        val newItems = editItems.toMutableList()
                                                        val gIdx = newItems.indexOf(editItem)
                                                        if (gIdx != -1) {
                                                            val newCards = editItem.cards.filter { it.id != card.id }
                                                            if (newCards.isEmpty()) {
                                                                newItems.removeAt(gIdx)
                                                                newItems.add(gIdx, EditItem.Card(card))
                                                            } else {
                                                                newItems[gIdx] = editItem.copy(cards = newCards)
                                                                newItems.add(gIdx + 1, EditItem.Card(card))
                                                            }
                                                            editItems = newItems
                                                        }
                                                    },
                                                    onAddToGroup = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(140.dp)) }
                }
            }
        }

        if (editItems.isNotEmpty()) {
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
                        Icon(Icons.Rounded.Add, contentDescription = null)
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
                    displayOrder = 0,
                    intervalGap = null,
                    category = null
                )
                editItems = editItems + EditItem.Group(newDivider, emptyList())
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
    shape: Shape,
    isDeleteMode: Boolean,
    availableGroups: List<EditItem.Group>,
    currentGroupId: Long?,
    currentGroupTitle: String?,
    onDeleteClick: () -> Unit = {},
    onRemoveFromGroup: (() -> Unit)? = null,
    onAddToGroup: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isMedicine = item.type == ItemType.Medicine

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

    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                if (currentGroupId != null) {
                                    DropdownMenuItem(
                                        text = {
                                            currentGroupTitle?.let {
                                                Text(
                                                    text = stringResource(R.string.remove_from_group, it),
                                                    fontFamily = GoogleSansFlex
                                                )
                                            }
                                        },
                                        onClick = {
                                            onRemoveFromGroup?.invoke()
                                            menuExpanded = false
                                        }
                                    )
                                } else {
                                    if (availableGroups.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No groups available", fontFamily = GoogleSansFlex) },
                                            onClick = { menuExpanded = false },
                                            enabled = false
                                        )
                                    } else {
                                        availableGroups.forEach { group ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = stringResource(R.string.add_to_group, group.divider.title),
                                                        fontFamily = GoogleSansFlex
                                                    )
                                                },
                                                onClick = {
                                                    onAddToGroup?.invoke(group.divider.id)
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}