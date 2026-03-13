@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.MainActivity

import android.graphics.Color.parseColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.AVAILABLE_ICONS
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.R
import com.fedeveloper95.med.services.MedData
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MedDataCard(
    item: MedData,
    currentViewDate: LocalDate,
    shape: Shape,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "anim_shape"
    )

    val animatedShape = remember(shape, pressProgress) {
        if (shape is RoundedCornerShape) {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val targetPx = with(density) { 20.dp.toPx() }
                    fun lerp(start: Float, stop: Float, fraction: Float) =
                        (1 - fraction) * start + fraction * stop

                    val ts = lerp(shape.topStart.toPx(size, density), targetPx, pressProgress)
                    val te = lerp(shape.topEnd.toPx(size, density), targetPx, pressProgress)
                    val bs = lerp(shape.bottomStart.toPx(size, density), targetPx, pressProgress)
                    val be = lerp(shape.bottomEnd.toPx(size, density), targetPx, pressProgress)

                    return Outline.Rounded(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                0f,
                                0f,
                                size.width,
                                size.height
                            ),
                            topLeft = androidx.compose.ui.geometry.CornerRadius(ts),
                            topRight = androidx.compose.ui.geometry.CornerRadius(te),
                            bottomRight = androidx.compose.ui.geometry.CornerRadius(be),
                            bottomLeft = androidx.compose.ui.geometry.CornerRadius(bs)
                        )
                    )
                }
            }
        } else shape
    }

    val isMedicine = item.type == ItemType.Medicine
    val isTakenToday = if (isMedicine) item.takenHistory.containsKey(currentViewDate) else false
    val timestamp = if (isMedicine) item.takenHistory[currentViewDate] else item.creationTime
    val isToday = LocalDate.now() == currentViewDate

    val toggleEnabled = isMedicine && !currentViewDate.isAfter(LocalDate.now())

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
    val iconBoxColor = customColor
        ?: if (isMedicine) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val iconBoxTintColor =
        if (customColor != null) Color.Black.copy(alpha = 0.7f) else if (isMedicine) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    val scale = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    var currentShape by remember { mutableIntStateOf(R.drawable.ic_ten_sided_cookie) }

    LaunchedEffect(isTakenToday) {
        if (isTakenToday) {
            val shapes = listOf(
                R.drawable.ic_ten_sided_cookie,
                R.drawable.ic_twelve_sided_cookie,
                R.drawable.ic_triangle,
                R.drawable.ic_gem,
                R.drawable.ic_ghost,
                R.drawable.ic_pentagon
            )
            currentShape = shapes.random()

            launch {
                alpha.animateTo(1f, animationSpec = tween(200))
            }
            launch {
                scale.snapTo(0f)
                scale.animateTo(1.1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            launch {
                rotation.snapTo(0f)
                rotation.animateTo(180f, animationSpec = tween(900, easing = FastOutSlowInEasing))
                launch {
                    scale.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
                launch {
                    alpha.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                }
            }
        } else {
            launch {
                scale.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                rotation.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
            launch {
                alpha.animateTo(0f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(animatedShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = animatedShape,
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
            contentColor = cardContentColor
        ),
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
                        if (isMedicine) {
                            val scheduledTime =
                                item.creationTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                            if (isTakenToday && timestamp != null) {
                                val takenTime =
                                    timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))
                                val datePart = if (isToday) "" else "${
                                    currentViewDate.format(
                                        DateTimeFormatter.ofPattern("dd/MM")
                                    )
                                } "

                                Icon(
                                    Icons.Rounded.Schedule,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = cardContentColor.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(
                                        R.string.status_taken_format,
                                        datePart,
                                        takenTime
                                    ),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cardContentColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            } else {
                                Text(
                                    stringResource(
                                        R.string.status_scheduled_format,
                                        scheduledTime
                                    ),
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cardContentColor.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        } else if (timestamp != null) {
                            Icon(
                                Icons.Rounded.Schedule,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = cardContentColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
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
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconBoxTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            trailingContent = if (isMedicine) {
                {
                    Box(contentAlignment = Alignment.Center) {
                        if (alpha.value > 0f) {
                            Icon(
                                painter = painterResource(id = currentShape),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha.value),
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(scale.value)
                                    .rotate(rotation.value)
                            )
                        }
                        RadioButton(
                            selected = isTakenToday,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggle()
                            },
                            enabled = toggleEnabled
                        )
                    }
                }
            } else null,
            modifier = Modifier.padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun WeeklyCalendarPager(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    locale: Locale
) {
    val pagerState = rememberPagerState(initialPage = 1000, pageCount = { 2000 })
    val today = remember { LocalDate.now() }
    val currentWeekStart =
        remember(locale, today) { today.with(WeekFields.of(locale).dayOfWeek(), 1L) }

    LaunchedEffect(selectedDate, locale) {
        val weeksDiff = ChronoUnit.WEEKS.between(
            currentWeekStart,
            selectedDate.with(WeekFields.of(locale).dayOfWeek(), 1L)
        )
        val targetPage = 1000 + weeksDiff.toInt()
        if (pagerState.currentPage != targetPage) pagerState.animateScrollToPage(targetPage)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 16.dp
    ) { page ->
        val weekStart = currentWeekStart.plusWeeks((page - 1000).toLong())
        val monthName = when (weekStart.month) {
            java.time.Month.JANUARY -> R.string.month_january
            java.time.Month.FEBRUARY -> R.string.month_february
            java.time.Month.MARCH -> R.string.month_march
            java.time.Month.APRIL -> R.string.month_april
            java.time.Month.MAY -> R.string.month_may
            java.time.Month.JUNE -> R.string.month_june
            java.time.Month.JULY -> R.string.month_july
            java.time.Month.AUGUST -> R.string.month_august
            java.time.Month.SEPTEMBER -> R.string.month_september
            java.time.Month.OCTOBER -> R.string.month_october
            java.time.Month.NOVEMBER -> R.string.month_november
            java.time.Month.DECEMBER -> R.string.month_december
            else -> R.string.unknown
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(monthName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (dayOffset in 0..6) {
                    val date = weekStart.plusDays(dayOffset.toLong())
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CalendarDayItem(
                            date = date,
                            isSelected = date == selectedDate,
                            onClick = { onDateSelected(date) })
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayItem(date: LocalDate, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )
    val cornerRadius by animateIntAsState(
        targetValue = if (isPressed) 12 else 32,
        animationSpec = tween(durationMillis = 200),
        label = "corner"
    )

    val isToday = date == LocalDate.now()
    val dayInitial = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> R.string.day_short_mon
        DayOfWeek.TUESDAY -> R.string.day_short_tue
        DayOfWeek.WEDNESDAY -> R.string.day_short_wed
        DayOfWeek.THURSDAY -> R.string.day_short_thu
        DayOfWeek.FRIDAY -> R.string.day_short_fri
        DayOfWeek.SATURDAY -> R.string.day_short_sat
        DayOfWeek.SUNDAY -> R.string.day_short_sun
        else -> R.string.unknown
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor)
            .then(
                if (isToday) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(cornerRadius.dp)
                )
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(dayInitial),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}