@file:OptIn(
    ExperimentalTextApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.fedeveloper95.med.services

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.AVAILABLE_ICONS
import com.fedeveloper95.med.BirthdayCard
import com.fedeveloper95.med.EditModeActivity
import com.fedeveloper95.med.ExpressiveIconButton
import com.fedeveloper95.med.ExpressiveTextButton
import com.fedeveloper95.med.IllnessCard
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.R
import com.fedeveloper95.med.SettingsActivity
import com.fedeveloper95.med.SwipeableSquishItem
import com.fedeveloper95.med.elements.MainActivity.CommunityBottomSheet
import com.fedeveloper95.med.elements.MainActivity.EventBottomSheet
import com.fedeveloper95.med.elements.MainActivity.IllnessesBottomSheet
import com.fedeveloper95.med.elements.MainActivity.MainFAB
import com.fedeveloper95.med.elements.MainActivity.MedDataCard
import com.fedeveloper95.med.elements.MainActivity.MedSnackbarHost
import com.fedeveloper95.med.elements.MainActivity.MedicineBottomSheet
import com.fedeveloper95.med.elements.MainActivity.NotesBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.StatsTab
import com.fedeveloper95.med.elements.MainActivity.Tabs.YouTab
import com.fedeveloper95.med.elements.MainActivity.WeeklyCalendarPager
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MedApp(
    viewModel: MedViewModel,
    weekStart: String,
    sortOrder: String,
    presets: List<String>,
    useBottomSheet: Boolean,
    isExpandedScreen: Boolean,
    showCommunitySheet: Boolean,
    userDob: String,
    onCommunitySheetDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    var showMedicineDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showIllnessDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MedData?>(null) }
    var preFilledText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var activeSwipingItemId by remember { mutableStateOf<Long?>(null) }
    var noteToShow by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val menuItems = remember(presets, icSick, icMind, icMixture) {
        val defaultItems: List<Triple<ItemType, ImageVector, Triple<String, String?, String?>>> =
            listOf(
                Triple(
                    ItemType.Medicine,
                    Icons.Rounded.MedicalServices,
                    Triple(
                        context.getString(R.string.medicine_label),
                        null as String?,
                        null as String?
                    )
                ),
                Triple(
                    ItemType.Event,
                    Icons.Rounded.Event,
                    Triple(context.getString(R.string.event_label), null, null)
                ),
                Triple(
                    ItemType.Illness,
                    icMind,
                    Triple(context.getString(R.string.illness_label), null, null)
                )
            )
        val presetItems: List<Triple<ItemType, ImageVector, Triple<String, String?, String?>>> =
            presets.mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    val type = when (parts[0]) {
                        ItemType.Medicine.name -> ItemType.Medicine
                        ItemType.Illness.name -> ItemType.Illness
                        else -> ItemType.Event
                    }
                    val name = parts[1]
                    val iconName = parts.getOrNull(2)
                    val colorCode = parts.getOrNull(3)
                    val icon: ImageVector = when (iconName) {
                        "MixtureMed" -> icSick
                        "Bed" -> icMind
                        "Mood" -> icMixture
                        else -> if (iconName != null && AVAILABLE_ICONS.containsKey(iconName)) AVAILABLE_ICONS[iconName]!!
                        else if (type == ItemType.Medicine) Icons.Rounded.MedicalServices
                        else if (type == ItemType.Illness) icMind
                        else Icons.Rounded.Event
                    }
                    Triple(type, icon, Triple(name, iconName, colorCode))
                } else null
            }
        defaultItems + presetItems
    }

    BackHandler(enabled = fabMenuExpanded || selectedTab != 0) {
        if (fabMenuExpanded) {
            fabMenuExpanded = false
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isExpandedScreen) {
                val wideNavRailState =
                    rememberWideNavigationRailState(initialValue = WideNavigationRailValue.Expanded)
                WideNavigationRail(
                    state = wideNavRailState,
                    modifier = Modifier.width(IntrinsicSize.Max)
                ) {
                    val isRailExpanded =
                        wideNavRailState.currentValue == WideNavigationRailValue.Expanded
                    WideNavigationRailItem(
                        railExpanded = isRailExpanded,
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        label = {
                            Text(
                                stringResource(R.string.home_tab_title),
                                fontFamily = GoogleSansFlex
                            )
                        }
                    )
                    WideNavigationRailItem(
                        railExpanded = isRailExpanded,
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
                        label = {
                            Text(
                                stringResource(R.string.stats_tab_title),
                                fontFamily = GoogleSansFlex
                            )
                        }
                    )
                    WideNavigationRailItem(
                        railExpanded = isRailExpanded,
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        label = {
                            Text(
                                stringResource(R.string.you_tab_title),
                                fontFamily = GoogleSansFlex
                            )
                        }
                    )
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButton = {
                    if (selectedTab == 0) {
                        Box(modifier = Modifier.wrapContentSize(unbounded = true)) {
                            MainFAB(
                                fabMenuExpanded = fabMenuExpanded,
                                onExpandedChange = { fabMenuExpanded = it },
                                menuItems = menuItems,
                                onMenuItemClick = { type, name, iconName, colorCode ->
                                    fabMenuExpanded = false
                                    if (iconName == null) {
                                        preFilledText = ""
                                        if (type == ItemType.Medicine) showMedicineDialog = true
                                        else if (type == ItemType.Illness) showIllnessDialog = true
                                        else showEventDialog = true
                                    } else {
                                        viewModel.addItem(
                                            type,
                                            name,
                                            iconName,
                                            colorCode,
                                            listOf(LocalTime.now()),
                                            null
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        ShortNavigationBar {
                            ShortNavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                                label = {
                                    Text(
                                        stringResource(R.string.home_tab_title),
                                        fontFamily = GoogleSansFlex
                                    )
                                }
                            )
                            ShortNavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
                                label = {
                                    Text(
                                        stringResource(R.string.stats_tab_title),
                                        fontFamily = GoogleSansFlex
                                    )
                                }
                            )
                            ShortNavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                                label = {
                                    Text(
                                        stringResource(R.string.you_tab_title),
                                        fontFamily = GoogleSansFlex
                                    )
                                }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding()
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
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
                                if (selectedTab == 0) {
                                    ExpressiveIconButton(
                                        onClick = {
                                            val intent = Intent(
                                                context,
                                                EditModeActivity::class.java
                                            ).apply {
                                                putExtra(
                                                    "SELECTED_DATE",
                                                    viewModel.selectedDate.toEpochDay()
                                                )
                                            }
                                            context.startActivity(intent)
                                        },
                                        icon = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.edit_mode_desc),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ExpressiveIconButton(
                                        onClick = { showDatePicker = true },
                                        icon = Icons.Rounded.CalendarMonth,
                                        contentDescription = stringResource(R.string.choose_date_desc),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                ExpressiveIconButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                context,
                                                SettingsActivity::class.java
                                            )
                                        )
                                    },
                                    icon = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.settings_desc),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)).togetherWith(
                                    fadeOut(
                                        animationSpec = tween(200)
                                    )
                                )
                            },
                            label = "tab_transition",
                            modifier = Modifier.weight(1f)
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(if (isExpandedScreen) Modifier.padding(horizontal = 64.dp) else Modifier)
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val localeForCalendar =
                                            if (weekStart == "sunday") Locale.US else Locale.ITALY
                                        WeeklyCalendarPager(
                                            selectedDate = viewModel.selectedDate,
                                            onDateSelected = { viewModel.selectedDate = it },
                                            locale = localeForCalendar
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))

                                        val initialPage = 10000
                                        val dayPagerState = rememberPagerState(
                                            initialPage = initialPage,
                                            pageCount = { 20000 })
                                        val today = remember { LocalDate.now() }
                                        var isProgrammaticScroll by remember { mutableStateOf(false) }

                                        LaunchedEffect(viewModel.selectedDate) {
                                            val daysDiff = ChronoUnit.DAYS.between(
                                                today,
                                                viewModel.selectedDate
                                            ).toInt()
                                            val targetPage = initialPage + daysDiff
                                            if (dayPagerState.currentPage != targetPage) {
                                                isProgrammaticScroll = true
                                                try {
                                                    dayPagerState.animateScrollToPage(targetPage)
                                                } finally {
                                                    isProgrammaticScroll = false
                                                }
                                            }
                                        }

                                        LaunchedEffect(dayPagerState) {
                                            snapshotFlow { dayPagerState.currentPage }.collect { page ->
                                                if (!isProgrammaticScroll) {
                                                    val daysDiff = page - initialPage
                                                    val newDate = today.plusDays(daysDiff.toLong())
                                                    if (viewModel.selectedDate != newDate) {
                                                        viewModel.selectedDate = newDate
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalPager(
                                            state = dayPagerState,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                        ) { page ->
                                            val daysDiff = page - initialPage
                                            val pageDate = today.plusDays(daysDiff.toLong())

                                            val isPageBirthday = remember(userDob, pageDate) {
                                                if (userDob.isNotBlank() && userDob.length >= 5) {
                                                    val parts = userDob.split("/")
                                                    if (parts.size >= 2) {
                                                        val bDay = parts[0].toIntOrNull()
                                                        val bMonth = parts[1].toIntOrNull()
                                                        bDay == pageDate.dayOfMonth && bMonth == pageDate.monthValue
                                                    } else false
                                                } else false
                                            }

                                            val pageItems = viewModel.items.filter { item ->
                                                when (item.type) {
                                                    ItemType.Event -> item.creationDate == pageDate
                                                    ItemType.Illness -> {
                                                        val isAfterStart =
                                                            !pageDate.isBefore(item.creationDate)
                                                        val isBeforeEnd =
                                                            item.endDate == null || !pageDate.isAfter(
                                                                item.endDate
                                                            )
                                                        isAfterStart && isBeforeEnd
                                                    }

                                                    ItemType.Medicine -> {
                                                        val isAfterStart =
                                                            !pageDate.isBefore(item.creationDate)
                                                        val isBeforeEnd =
                                                            item.endDate == null || !pageDate.isAfter(
                                                                item.endDate
                                                            )
                                                        val isCorrectDay =
                                                            item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(
                                                                pageDate.dayOfWeek
                                                            )
                                                        val isCorrectGap =
                                                            item.intervalGap == null || ChronoUnit.DAYS.between(
                                                                item.creationDate,
                                                                pageDate
                                                            ) % item.intervalGap == 0L
                                                        isAfterStart && isBeforeEnd && isCorrectDay && isCorrectGap
                                                    }
                                                }
                                            }

                                            val illnesses =
                                                pageItems.filter { it.type == ItemType.Illness }

                                            val displayItems =
                                                pageItems.filterNot { it in illnesses }
                                                    .let { list ->
                                                        if (sortOrder == "time") {
                                                            list.filter { it.iconName != "DIVIDER" }
                                                                .sortedWith(compareBy { it.creationTime })
                                                        } else {
                                                            var currentList = list.sortedWith(
                                                                compareBy(
                                                                    { it.displayOrder },
                                                                    { it.creationTime })
                                                            )
                                                            var changed = true
                                                            while (changed) {
                                                                changed = false
                                                                val toRemove =
                                                                    currentList.filterIndexed { i, current ->
                                                                        if (current.iconName == "DIVIDER" && current.title.isBlank()) {
                                                                            val next =
                                                                                currentList.getOrNull(
                                                                                    i + 1
                                                                                )
                                                                            next == null || next.iconName == "DIVIDER"
                                                                        } else false
                                                                    }
                                                                if (toRemove.isNotEmpty()) {
                                                                    currentList =
                                                                        currentList.filterNot { it in toRemove }
                                                                    changed = true
                                                                }
                                                            }
                                                            currentList
                                                        }
                                                    }

                                            var isRefreshing by remember { mutableStateOf(false) }
                                            val pullRefreshState = rememberPullToRefreshState()
                                            val onRefresh: () -> Unit = {
                                                isRefreshing = true; scope.launch {
                                                delay(1000); viewModel.reloadData(); isRefreshing =
                                                false
                                            }
                                            }

                                            PullToRefreshBox(
                                                state = pullRefreshState,
                                                isRefreshing = isRefreshing,
                                                onRefresh = onRefresh,
                                                modifier = Modifier.fillMaxSize(),
                                                indicator = {
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentAlignment = Alignment.TopCenter
                                                    ) {
                                                        PullToRefreshDefaults.LoadingIndicator(
                                                            state = pullRefreshState,
                                                            isRefreshing = isRefreshing
                                                        )
                                                    }
                                                }
                                            ) {
                                                if (displayItems.isEmpty() && illnesses.isEmpty()) {
                                                    Column(modifier = Modifier.fillMaxSize()) {
                                                        if (isPageBirthday) {
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                            Box(
                                                                modifier = Modifier.padding(
                                                                    horizontal = 16.dp
                                                                )
                                                            ) {
                                                                BirthdayCard()
                                                            }
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxWidth(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                                Icon(
                                                                    Icons.Rounded.Event,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(64.dp),
                                                                    tint = MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                                Spacer(modifier = Modifier.height(16.dp))
                                                                Text(
                                                                    stringResource(R.string.no_events_label),
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (isExpandedScreen) {
                                                        LazyVerticalGrid(
                                                            columns = GridCells.Adaptive(minSize = 340.dp),
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 16.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                12.dp
                                                            ),
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                12.dp
                                                            )
                                                        ) {
                                                            if (illnesses.isNotEmpty()) {
                                                                items(
                                                                    count = illnesses.size,
                                                                    key = { "illness_${illnesses[it].id}" },
                                                                    span = {
                                                                        GridItemSpan(
                                                                            maxLineSpan
                                                                        )
                                                                    }
                                                                ) { idx ->
                                                                    val item = illnesses[idx]
                                                                    val isConfirmed =
                                                                        item.takenHistory.containsKey(
                                                                            pageDate
                                                                        ) || pageDate == item.creationDate
                                                                    val shape =
                                                                        RoundedCornerShape(24.dp)
                                                                    SwipeableSquishItem(
                                                                        item = item,
                                                                        shape = shape,
                                                                        onDeleteThresholdReached = { resetAnimation ->
                                                                            val itemToRestore = item
                                                                            viewModel.deleteItem(
                                                                                item,
                                                                                pageDate
                                                                            )
                                                                            val deletedMsg =
                                                                                context.getString(
                                                                                    R.string.item_deleted,
                                                                                    item.title
                                                                                )
                                                                            val undoMsg =
                                                                                context.getString(R.string.undo_action)
                                                                            scope.launch {
                                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                                val dismissJob =
                                                                                    launch {
                                                                                        delay(2000); snackbarHostState.currentSnackbarData?.dismiss()
                                                                                    }
                                                                                val result =
                                                                                    snackbarHostState.showSnackbar(
                                                                                        message = deletedMsg,
                                                                                        actionLabel = undoMsg,
                                                                                        withDismissAction = true,
                                                                                        duration = SnackbarDuration.Indefinite
                                                                                    )
                                                                                dismissJob.cancel()
                                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                                    viewModel.restoreItem(
                                                                                        itemToRestore
                                                                                    )
                                                                                    resetAnimation()
                                                                                }
                                                                            }
                                                                        },
                                                                        onSwipeStart = {
                                                                            activeSwipingItemId =
                                                                                item.id
                                                                        },
                                                                        onSwipeCancel = {
                                                                            activeSwipingItemId =
                                                                                null
                                                                        }
                                                                    ) {
                                                                        IllnessCard(
                                                                            item = item,
                                                                            isConfirmed = isConfirmed,
                                                                            onYes = {
                                                                                val now =
                                                                                    LocalTime.now()
                                                                                var currDate =
                                                                                    item.creationDate
                                                                                while (!currDate.isAfter(
                                                                                        pageDate
                                                                                    )
                                                                                ) {
                                                                                    item.takenHistory[currDate] =
                                                                                        now
                                                                                    currDate =
                                                                                        currDate.plusDays(
                                                                                            1
                                                                                        )
                                                                                }
                                                                                viewModel.confirmIllness(
                                                                                    item,
                                                                                    pageDate
                                                                                )
                                                                                viewModel.reloadData()
                                                                            },
                                                                            onNo = {
                                                                                viewModel.deleteItem(
                                                                                    item,
                                                                                    pageDate
                                                                                )
                                                                            },
                                                                            onLongClick = {
                                                                                editingItem = item
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            if (isPageBirthday) {
                                                                item(span = {
                                                                    GridItemSpan(
                                                                        maxLineSpan
                                                                    )
                                                                }) {
                                                                    Column {
                                                                        BirthdayCard()
                                                                        Spacer(
                                                                            modifier = Modifier.height(
                                                                                4.dp
                                                                            )
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            items(
                                                                count = displayItems.size,
                                                                key = { displayItems[it].id },
                                                                span = { idx ->
                                                                    if (displayItems[idx].iconName == "DIVIDER") GridItemSpan(
                                                                        maxLineSpan
                                                                    ) else GridItemSpan(1)
                                                                }
                                                            ) { idx ->
                                                                val item = displayItems[idx]
                                                                val isDivider =
                                                                    item.iconName == "DIVIDER"
                                                                Box(
                                                                    modifier = Modifier.animateItem(
                                                                        placementSpec = spring(
                                                                            stiffness = Spring.StiffnessMediumLow,
                                                                            visibilityThreshold = IntOffset.VisibilityThreshold
                                                                        ),
                                                                        fadeOutSpec = spring(
                                                                            stiffness = Spring.StiffnessMediumLow
                                                                        ),
                                                                        fadeInSpec = spring(
                                                                            stiffness = Spring.StiffnessMediumLow
                                                                        )
                                                                    )
                                                                ) {
                                                                    if (isDivider) {
                                                                        if (item.title.isNotBlank()) {
                                                                            Row(
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .padding(
                                                                                        start = 8.dp,
                                                                                        top = if (idx == 0) 8.dp else 8.dp,
                                                                                        bottom = 8.dp,
                                                                                        end = 8.dp
                                                                                    ),
                                                                                verticalAlignment = Alignment.CenterVertically
                                                                            ) {
                                                                                Text(
                                                                                    text = item.title,
                                                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                                                        fontFamily = GoogleSansFlex,
                                                                                        fontWeight = FontWeight.Normal
                                                                                    ),
                                                                                    color = MaterialTheme.colorScheme.primary
                                                                                )
                                                                            }
                                                                        } else {
                                                                            Spacer(
                                                                                modifier = Modifier.height(
                                                                                    if (idx == 0) 0.dp else 32.dp
                                                                                )
                                                                            )
                                                                        }
                                                                    } else {
                                                                        val shape =
                                                                            RoundedCornerShape(24.dp)
                                                                        SwipeableSquishItem(
                                                                            item = item,
                                                                            shape = shape,
                                                                            onDeleteThresholdReached = { resetAnimation ->
                                                                                val itemToRestore =
                                                                                    item
                                                                                viewModel.deleteItem(
                                                                                    item,
                                                                                    pageDate
                                                                                )
                                                                                val deletedMsg =
                                                                                    context.getString(
                                                                                        R.string.item_deleted,
                                                                                        item.title
                                                                                    )
                                                                                val undoMsg =
                                                                                    context.getString(
                                                                                        R.string.undo_action
                                                                                    )
                                                                                scope.launch {
                                                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                                                    val dismissJob =
                                                                                        launch {
                                                                                            delay(
                                                                                                2000
                                                                                            ); snackbarHostState.currentSnackbarData?.dismiss()
                                                                                        }
                                                                                    val result =
                                                                                        snackbarHostState.showSnackbar(
                                                                                            message = deletedMsg,
                                                                                            actionLabel = undoMsg,
                                                                                            withDismissAction = true,
                                                                                            duration = SnackbarDuration.Indefinite
                                                                                        )
                                                                                    dismissJob.cancel()
                                                                                    if (result == SnackbarResult.ActionPerformed) {
                                                                                        viewModel.restoreItem(
                                                                                            itemToRestore
                                                                                        )
                                                                                        resetAnimation()
                                                                                    }
                                                                                }
                                                                            },
                                                                            onSwipeStart = {
                                                                                activeSwipingItemId =
                                                                                    item.id
                                                                            },
                                                                            onSwipeCancel = {
                                                                                activeSwipingItemId =
                                                                                    null
                                                                            }
                                                                        ) {
                                                                            MedDataCard(
                                                                                item = item,
                                                                                currentViewDate = pageDate,
                                                                                shape = shape,
                                                                                onToggle = {
                                                                                    viewModel.toggleMedicine(
                                                                                        item,
                                                                                        pageDate
                                                                                    )
                                                                                },
                                                                                onClick = {
                                                                                    if (!item.notes.isNullOrBlank()) noteToShow =
                                                                                        item.notes
                                                                                },
                                                                                onLongClick = {
                                                                                    editingItem =
                                                                                        item
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                                Spacer(
                                                                    modifier = Modifier.height(100.dp)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        LazyColumn(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 16.dp),
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                2.dp
                                                            )
                                                        ) {
                                                            if (illnesses.isNotEmpty()) {
                                                                itemsIndexed(
                                                                    items = illnesses,
                                                                    key = { _, item -> "illness_${item.id}" }
                                                                ) { _, item ->
                                                                    val isConfirmed =
                                                                        item.takenHistory.containsKey(
                                                                            pageDate
                                                                        ) || pageDate == item.creationDate
                                                                    val shape =
                                                                        RoundedCornerShape(24.dp)
                                                                    SwipeableSquishItem(
                                                                        item = item,
                                                                        shape = shape,
                                                                        onDeleteThresholdReached = { resetAnimation ->
                                                                            val itemToRestore = item
                                                                            viewModel.deleteItem(
                                                                                item,
                                                                                pageDate
                                                                            )
                                                                            val deletedMsg =
                                                                                context.getString(
                                                                                    R.string.item_deleted,
                                                                                    item.title
                                                                                )
                                                                            val undoMsg =
                                                                                context.getString(R.string.undo_action)
                                                                            scope.launch {
                                                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                                                val dismissJob =
                                                                                    launch {
                                                                                        delay(2000); snackbarHostState.currentSnackbarData?.dismiss()
                                                                                    }
                                                                                val result =
                                                                                    snackbarHostState.showSnackbar(
                                                                                        message = deletedMsg,
                                                                                        actionLabel = undoMsg,
                                                                                        withDismissAction = true,
                                                                                        duration = SnackbarDuration.Indefinite
                                                                                    )
                                                                                dismissJob.cancel()
                                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                                    viewModel.restoreItem(
                                                                                        itemToRestore
                                                                                    )
                                                                                    resetAnimation()
                                                                                }
                                                                            }
                                                                        },
                                                                        onSwipeStart = {
                                                                            activeSwipingItemId =
                                                                                item.id
                                                                        },
                                                                        onSwipeCancel = {
                                                                            activeSwipingItemId =
                                                                                null
                                                                        }
                                                                    ) {
                                                                        IllnessCard(
                                                                            item = item,
                                                                            isConfirmed = isConfirmed,
                                                                            onYes = {
                                                                                val now =
                                                                                    LocalTime.now()
                                                                                var currDate =
                                                                                    item.creationDate
                                                                                while (!currDate.isAfter(
                                                                                        pageDate
                                                                                    )
                                                                                ) {
                                                                                    item.takenHistory[currDate] =
                                                                                        now
                                                                                    currDate =
                                                                                        currDate.plusDays(
                                                                                            1
                                                                                        )
                                                                                }
                                                                                viewModel.confirmIllness(
                                                                                    item,
                                                                                    pageDate
                                                                                )
                                                                                viewModel.reloadData()
                                                                            },
                                                                            onNo = {
                                                                                viewModel.deleteItem(
                                                                                    item,
                                                                                    pageDate
                                                                                )
                                                                            },
                                                                            onLongClick = {
                                                                                editingItem = item
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            if (isPageBirthday) {
                                                                item {
                                                                    Spacer(
                                                                        modifier = Modifier.height(
                                                                            8.dp
                                                                        )
                                                                    )
                                                                    BirthdayCard()
                                                                    Spacer(
                                                                        modifier = Modifier.height(
                                                                            14.dp
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                            itemsIndexed(
                                                                items = displayItems,
                                                                key = { _, item -> item.id }) { index, item ->
                                                                val isDivider =
                                                                    item.iconName == "DIVIDER"
                                                                val isFirstInGroup =
                                                                    index == 0 || displayItems.getOrNull(
                                                                        index - 1
                                                                    )?.iconName == "DIVIDER"
                                                                val isLastInGroup =
                                                                    index == displayItems.lastIndex || displayItems.getOrNull(
                                                                        index + 1
                                                                    )?.iconName == "DIVIDER"
                                                                val topRadius =
                                                                    if (isFirstInGroup) 20.dp else 4.dp
                                                                val bottomRadius =
                                                                    if (isLastInGroup) 20.dp else 4.dp
                                                                val shape = RoundedCornerShape(
                                                                    topStart = topRadius,
                                                                    topEnd = topRadius,
                                                                    bottomStart = bottomRadius,
                                                                    bottomEnd = bottomRadius
                                                                )
                                                                Box(
                                                                    modifier = Modifier.animateItem(
                                                                        placementSpec = spring(
                                                                            stiffness = Spring.StiffnessMediumLow,
                                                                            visibilityThreshold = IntOffset.VisibilityThreshold
                                                                        ),
                                                                        fadeOutSpec = spring(
                                                                            stiffness = Spring.StiffnessMediumLow
                                                                        ),
                                                                        fadeInSpec = spring(
                                                                            stiffness = Spring.StiffnessMediumLow
                                                                        )
                                                                    )
                                                                ) {
                                                                    if (isDivider) {
                                                                        if (item.title.isNotBlank()) {
                                                                            Row(
                                                                                modifier = Modifier
                                                                                    .fillMaxWidth()
                                                                                    .padding(
                                                                                        start = 16.dp,
                                                                                        top = if (index == 0) 16.dp else 16.dp,
                                                                                        bottom = 16.dp,
                                                                                        end = 16.dp
                                                                                    ),
                                                                                verticalAlignment = Alignment.CenterVertically
                                                                            ) {
                                                                                Text(
                                                                                    text = item.title,
                                                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                                                        fontFamily = GoogleSansFlex,
                                                                                        fontWeight = FontWeight.Normal
                                                                                    ),
                                                                                    color = MaterialTheme.colorScheme.primary
                                                                                )
                                                                            }
                                                                        } else {
                                                                            Spacer(
                                                                                modifier = Modifier.height(
                                                                                    if (index == 0) 0.dp else 0.dp
                                                                                )
                                                                            )
                                                                        }
                                                                    } else {
                                                                        SwipeableSquishItem(
                                                                            item = item,
                                                                            shape = shape,
                                                                            onDeleteThresholdReached = { resetAnimation ->
                                                                                val itemToRestore =
                                                                                    item
                                                                                viewModel.deleteItem(
                                                                                    item,
                                                                                    pageDate
                                                                                )
                                                                                val deletedMsg =
                                                                                    context.getString(
                                                                                        R.string.item_deleted,
                                                                                        item.title
                                                                                    )
                                                                                val undoMsg =
                                                                                    context.getString(
                                                                                        R.string.undo_action
                                                                                    )
                                                                                scope.launch {
                                                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                                                    val dismissJob =
                                                                                        launch {
                                                                                            delay(
                                                                                                2000
                                                                                            ); snackbarHostState.currentSnackbarData?.dismiss()
                                                                                        }
                                                                                    val result =
                                                                                        snackbarHostState.showSnackbar(
                                                                                            message = deletedMsg,
                                                                                            actionLabel = undoMsg,
                                                                                            withDismissAction = true,
                                                                                            duration = SnackbarDuration.Indefinite
                                                                                        )
                                                                                    dismissJob.cancel()
                                                                                    if (result == SnackbarResult.ActionPerformed) {
                                                                                        viewModel.restoreItem(
                                                                                            itemToRestore
                                                                                        )
                                                                                        resetAnimation()
                                                                                    }
                                                                                }
                                                                            },
                                                                            onSwipeStart = {
                                                                                activeSwipingItemId =
                                                                                    item.id
                                                                            },
                                                                            onSwipeCancel = {
                                                                                activeSwipingItemId =
                                                                                    null
                                                                            }
                                                                        ) {
                                                                            MedDataCard(
                                                                                item = item,
                                                                                currentViewDate = pageDate,
                                                                                shape = shape,
                                                                                onToggle = {
                                                                                    viewModel.toggleMedicine(
                                                                                        item,
                                                                                        pageDate
                                                                                    )
                                                                                },
                                                                                onClick = {
                                                                                    if (!item.notes.isNullOrBlank()) noteToShow =
                                                                                        item.notes
                                                                                },
                                                                                onLongClick = {
                                                                                    editingItem =
                                                                                        item
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            item {
                                                                Spacer(
                                                                    modifier = Modifier.height(
                                                                        100.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    StatsTab(
                                        viewModel = viewModel,
                                        onNavigateToHome = { date ->
                                            viewModel.selectedDate = date
                                            selectedTab = 0
                                        }
                                    )
                                }

                                2 -> {
                                    YouTab()
                                }
                            }
                        }
                    }
                    if (fabMenuExpanded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { fabMenuExpanded = false }
                        )
                    }
                }
            }
        }
        MedSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (!isExpandedScreen) 180.dp else 120.dp)
        )
    }

    if (showCommunitySheet) {
        CommunityBottomSheet(
            onDismiss = onCommunitySheetDismiss
        )
    }

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(initialSelectedDateMillis = viewModel.selectedDate.toEpochDay() * 24 * 60 * 60 * 1000)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                ExpressiveTextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.selectedDate =
                            LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    }; showDatePicker = false
                }, text = stringResource(R.string.ok_action))
            },
            dismissButton = {
                ExpressiveTextButton(
                    onClick = { showDatePicker = false },
                    text = stringResource(R.string.cancel_action)
                )
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 6.dp
        ) { DatePicker(state = datePickerState) }
    }

    if (editingItem != null) {
        val itemToEdit = editingItem!!
        val isMed = itemToEdit.type == ItemType.Medicine
        val isIllness = itemToEdit.type == ItemType.Illness
        if (useBottomSheet) {
            if (isMed) {
                MedicineBottomSheet(
                    onDismiss = { editingItem = null },
                    onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap, notificationType, rangeStart, rangeEnd ->
                        viewModel.updateItem(
                            itemToEdit,
                            title,
                            iconName,
                            colorCode,
                            times,
                            days,
                            notes,
                            intervalGap,
                            notificationType,
                            rangeStart,
                            rangeEnd
                        )
                        editingItem = null
                    },
                    initialItem = itemToEdit
                )
            } else if (isIllness) {
                IllnessesBottomSheet(
                    onDismiss = { editingItem = null },
                    onConfirm = { title, iconName, colorCode, startDate ->
                        val prevSelected = viewModel.selectedDate
                        viewModel.selectedDate = startDate
                        viewModel.updateItem(
                            itemToEdit,
                            title,
                            iconName,
                            colorCode,
                            listOf(itemToEdit.creationTime),
                            null,
                            null,
                            null
                        )
                        viewModel.selectedDate = prevSelected
                        editingItem = null
                    },
                    initialItem = itemToEdit
                )
            } else {
                EventBottomSheet(
                    onDismiss = { editingItem = null },
                    onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                        viewModel.updateItem(
                            itemToEdit,
                            title,
                            iconName,
                            colorCode,
                            times,
                            days,
                            notes,
                            intervalGap
                        )
                        editingItem = null
                    },
                    initialItem = itemToEdit
                )
            }
        }
    }

    if (showMedicineDialog) {
        if (useBottomSheet) {
            MedicineBottomSheet(
                onDismiss = { showMedicineDialog = false },
                onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap, notificationType, rangeStart, rangeEnd ->
                    viewModel.addItem(
                        ItemType.Medicine,
                        title,
                        iconName,
                        colorCode,
                        times,
                        days,
                        notes = notes,
                        intervalGap = intervalGap,
                        notificationType = notificationType
                    )
                    showMedicineDialog = false
                },
                initialText = preFilledText
            )
        }
    }

    if (showIllnessDialog) {
        if (useBottomSheet) {
            IllnessesBottomSheet(
                onDismiss = { showIllnessDialog = false },
                onConfirm = { title, iconName, colorCode, startDate ->
                    val prevSelected = viewModel.selectedDate
                    viewModel.selectedDate = startDate
                    viewModel.addItem(
                        ItemType.Illness,
                        title,
                        iconName,
                        colorCode,
                        listOf(LocalTime.now()),
                        null,
                        notes = null,
                        intervalGap = null
                    )
                    viewModel.selectedDate = prevSelected
                    showIllnessDialog = false
                },
                initialText = preFilledText
            )
        }
    }

    if (showEventDialog) {
        if (useBottomSheet) {
            EventBottomSheet(
                onDismiss = { showEventDialog = false },
                onConfirm = { title, iconName, colorCode, times, days, notes, intervalGap ->
                    viewModel.addItem(
                        ItemType.Event,
                        title,
                        iconName,
                        colorCode,
                        times,
                        days,
                        notes = notes,
                        intervalGap = intervalGap
                    )
                    showEventDialog = false
                },
                initialText = preFilledText
            )
        }
    }

    if (noteToShow != null) {
        NotesBottomSheet(notes = noteToShow!!, onDismiss = { noteToShow = null })
    }
}