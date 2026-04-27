@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.ui.text.ExperimentalTextApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.fedeveloper95.med.elements.MainActivity.Tabs

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.R
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.services.MedViewModel
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class DayStatus {
    ALL_TAKEN, PARTIAL, NONE_TAKEN, NO_MEDS, FUTURE
}

fun getScheduledMedsForDate(date: LocalDate, items: List<MedData>): List<MedData> {
    return items.filter { item ->
        item.type == ItemType.Medicine &&
                !date.isBefore(item.creationDate) &&
                (item.endDate == null || !date.isAfter(item.endDate)) &&
                (item.recurrenceDays.isNullOrEmpty() || item.recurrenceDays.contains(date.dayOfWeek)) &&
                (item.intervalGap == null || ChronoUnit.DAYS.between(item.creationDate, date) % item.intervalGap == 0L)
    }
}

fun getStatusForDate(date: LocalDate, items: List<MedData>): DayStatus {
    if (date.isAfter(LocalDate.now())) return DayStatus.FUTURE

    val scheduledMeds = getScheduledMedsForDate(date, items)
    if (scheduledMeds.isEmpty()) return DayStatus.NO_MEDS

    val takenCount = scheduledMeds.count { it.takenHistory.containsKey(date) }

    return when {
        takenCount == 0 -> DayStatus.NONE_TAKEN
        takenCount == scheduledMeds.size -> DayStatus.ALL_TAKEN
        else -> DayStatus.PARTIAL
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatsTab(
    viewModel: MedViewModel,
    onNavigateToHome: (LocalDate) -> Unit
) {
    val initialPage = 1200
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2400 })
    val currentMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())
    }

    val today = LocalDate.now()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp > 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val allMeds = viewModel.items.filter { it.type == ItemType.Medicine }
    val hasMeds = allMeds.isNotEmpty()

    var streak = 0
    var adherence = 0
    var missedPerWeekStr = "0.0"

    if (hasMeds) {
        val earliestDate = allMeds.minOfOrNull { it.creationDate } ?: today
        var totalScheduled = 0
        var totalTaken = 0
        var totalMissed = 0

        var currentStreak = 0
        var streakBroken = false

        var dateCursor = today
        while (!dateCursor.isBefore(earliestDate)) {
            val scheduled = getScheduledMedsForDate(dateCursor, allMeds)
            if (scheduled.isNotEmpty()) {
                val takenCount = scheduled.count { it.takenHistory.containsKey(dateCursor) }
                val scheduledCount = scheduled.size

                if (!dateCursor.isEqual(today) || takenCount > 0) {
                    totalScheduled += scheduledCount
                    totalTaken += takenCount
                    totalMissed += (scheduledCount - takenCount)
                }

                if (!streakBroken) {
                    if (takenCount == scheduledCount) {
                        currentStreak++
                    } else if (dateCursor.isBefore(today)) {
                        streakBroken = true
                    }
                }
            }
            dateCursor = dateCursor.minusDays(1)
        }

        streak = currentStreak
        adherence = if (totalScheduled > 0) (totalTaken * 100f / totalScheduled).toInt() else 0
        val totalDays = java.lang.Math.max(1L, ChronoUnit.DAYS.between(earliestDate, today.plusDays(1)))
        val weeks = java.lang.Math.max(1f, totalDays / 7f)
        missedPerWeekStr = String.format(Locale.US, "%.1f", totalMissed / weeks)
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            delay(1000)
            viewModel.reloadData()
            isRefreshing = false
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (isExpanded) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatsSummaryCard(
                                streak = streak.toString(),
                                adherence = "$adherence%",
                                missedPerWeek = missedPerWeekStr,
                                isVertical = true
                            )
                        }
                        Box(modifier = Modifier.weight(1.5f)) {
                            CalendarCard(
                                currentMonth = currentMonth,
                                pagerState = pagerState,
                                today = today,
                                items = viewModel.items,
                                onNavigateToHome = onNavigateToHome,
                                initialPage = initialPage
                            )
                        }
                    }
                } else {
                    StatsSummaryCard(
                        streak = streak.toString(),
                        adherence = "$adherence%",
                        missedPerWeek = missedPerWeekStr,
                        isVertical = false
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CalendarCard(
                        currentMonth = currentMonth,
                        pagerState = pagerState,
                        today = today,
                        items = viewModel.items,
                        onNavigateToHome = onNavigateToHome,
                        initialPage = initialPage
                    )
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun StatsSummaryCard(streak: String, adherence: String, missedPerWeek: String, isVertical: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        if (isVertical) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatItem(
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = streak,
                    label = stringResource(R.string.stats_streak_label),
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    backgroundRes = R.drawable.ic_ghost
                )
                StatItem(
                    icon = Icons.Rounded.CheckCircle,
                    value = adherence,
                    label = stringResource(R.string.stats_on_time_label),
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    backgroundRes = R.drawable.ic_twelve_sided_cookie
                )
                StatItem(
                    icon = Icons.Rounded.Warning,
                    value = missedPerWeek,
                    label = stringResource(R.string.stats_missed_label),
                    iconColor = MaterialTheme.colorScheme.onErrorContainer,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    backgroundRes = R.drawable.ic_pentagon
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = streak,
                    label = stringResource(R.string.stats_streak_label),
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    backgroundRes = R.drawable.ic_ghost
                )
                StatItem(
                    icon = Icons.Rounded.CheckCircle,
                    value = adherence,
                    label = stringResource(R.string.stats_on_time_label),
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    backgroundRes = R.drawable.ic_twelve_sided_cookie
                )
                StatItem(
                    icon = Icons.Rounded.Warning,
                    value = missedPerWeek,
                    label = stringResource(R.string.stats_missed_label),
                    iconColor = MaterialTheme.colorScheme.onErrorContainer,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    backgroundRes = R.drawable.ic_pentagon
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarCard(
    currentMonth: YearMonth,
    pagerState: PagerState,
    today: LocalDate,
    items: List<MedData>,
    onNavigateToHome: (LocalDate) -> Unit,
    initialPage: Int
) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CalendarHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNextMonth = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageMonth = YearMonth.now().plusMonths((page - initialPage).toLong())
                CalendarGrid(
                    currentMonth = pageMonth,
                    today = today,
                    onDateClick = onNavigateToHome,
                    getStatusForDate = { date -> getStatusForDate(date, items) }
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    containerColor: Color,
    backgroundRes: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = backgroundRes),
                contentDescription = null,
                tint = containerColor,
                modifier = Modifier.fillMaxSize()
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = GoogleSansFlex,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$monthName ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    getStatusForDate: (LocalDate) -> DayStatus
) {
    val daysOfWeek = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val firstDayOfMonth = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value - 1

        var currentDay = 1

        while (currentDay <= daysInMonth) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0..6) {
                    if (currentDay == 1 && i < firstDayOfWeekIndex) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else if (currentDay > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = currentMonth.atDay(currentDay)
                        val status = getStatusForDate(date)
                        val isToday = date == today

                        CalendarDayCell(
                            date = date,
                            status = status,
                            isToday = isToday,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateClick(date) }
                        )
                        currentDay++
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate,
    status: DayStatus,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when (status) {
            DayStatus.ALL_TAKEN -> MaterialTheme.colorScheme.primary
            DayStatus.PARTIAL -> MaterialTheme.colorScheme.secondary
            DayStatus.NONE_TAKEN -> MaterialTheme.colorScheme.error
            DayStatus.NO_MEDS -> Color.Transparent
            DayStatus.FUTURE -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "cell_color"
    )

    val textColor = when (status) {
        DayStatus.ALL_TAKEN -> MaterialTheme.colorScheme.onPrimary
        DayStatus.PARTIAL -> MaterialTheme.colorScheme.onSecondary
        DayStatus.NONE_TAKEN -> MaterialTheme.colorScheme.onError
        DayStatus.FUTURE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 25 else 50,
        animationSpec = tween(durationMillis = 200),
        label = "corner"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(cornerPercent))
            .background(containerColor)
            .then(
                if (isToday) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(cornerPercent))
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = GoogleSansFlex,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}