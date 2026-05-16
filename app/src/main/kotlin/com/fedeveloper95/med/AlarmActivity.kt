@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.services.DataRepository
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AlarmActivity : ComponentActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finishAndRemoveTask()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val filter = IntentFilter("ACTION_CLOSE_ALARM_ACTIVITY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        val itemTitle = intent.getStringExtra("ITEM_TITLE") ?: getString(R.string.alarm_default_title)

        val itemIds = intent.getLongArrayExtra("ITEM_IDS") ?: run {
            val singleId = intent.getLongExtra("ITEM_ID", -1L)
            if (singleId != -1L) longArrayOf(singleId) else longArrayOf()
        }
        val notifId = intent.getIntExtra("NOTIF_ID", -1)

        val prefs = getSharedPreferences("med_settings", MODE_PRIVATE)
        val currentTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)
        val snoozeDuration = prefs.getInt(PREF_SNOOZE_DURATION, 10)
        val useSlider = prefs.getBoolean("pref_alarm_style_slider", true)

        val items = DataRepository.loadData(this)
        val currentItem = items.find { it.id == itemIds.firstOrNull() }

        val iconName = currentItem?.iconName
        val colorCodeStr = currentItem?.colorCode
        val isGrouped = itemIds.size > 1

        setContent {
            MedTheme(themeOverride = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmScreen(
                        title = itemTitle,
                        snoozeDuration = snoozeDuration,
                        useSlider = useSlider,
                        iconName = iconName,
                        colorCode = colorCodeStr,
                        isGrouped = isGrouped,
                        onTake = {
                            val actionIntent = Intent(
                                this@AlarmActivity,
                                com.fedeveloper95.med.services.NotificationReceiver::class.java
                            ).apply {
                                action = "ACTION_TAKEN"
                                putExtra("ITEM_IDS", itemIds)
                                putExtra("NOTIF_ID", notifId)
                            }
                            sendBroadcast(actionIntent)
                            finishAndRemoveTask()
                        },
                        onSnooze = {
                            val actionIntent = Intent(
                                this@AlarmActivity,
                                com.fedeveloper95.med.services.NotificationReceiver::class.java
                            ).apply {
                                action = "ACTION_SNOOZE"
                                putExtra("ITEM_IDS", itemIds)
                                putExtra("NOTIF_ID", notifId)
                            }
                            sendBroadcast(actionIntent)
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeReceiver)
    }
}

@Composable
fun AnimatedAlarmButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnMorph"
    )

    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(88.dp),
            shape = RoundedCornerShape(cornerPercent),
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontFamily = GoogleSansFlex,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(88.dp),
            shape = RoundedCornerShape(cornerPercent),
            interactionSource = interactionSource,
            contentPadding = PaddingValues(0.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontFamily = GoogleSansFlex,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AlarmScreen(
    title: String,
    snoozeDuration: Int,
    useSlider: Boolean,
    iconName: String?,
    colorCode: String?,
    isGrouped: Boolean = false,
    onTake: () -> Unit,
    onSnooze: () -> Unit
) {
    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val icon = when (iconName) {
        "MixtureMed" -> icSick
        "Bed" -> icMind
        "Mood" -> icMixture
        else -> if (iconName != null && AVAILABLE_ICONS.containsKey(iconName)) AVAILABLE_ICONS[iconName]!!
        else Icons.Rounded.MedicalServices
    }

    val customColor = remember(colorCode) {
        if (colorCode != null && colorCode != "dynamic") try {
            Color(android.graphics.Color.parseColor(colorCode))
        } catch (e: Exception) {
            null
        } else null
    }

    val containerColor = customColor ?: MaterialTheme.colorScheme.tertiaryContainer
    val onContainerColor =
        if (customColor != null) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onTertiaryContainer

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_twelve_sided_cookie),
                contentDescription = null,
                tint = containerColor,
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation)
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = onContainerColor
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = if (isGrouped) title else stringResource(R.string.alarm_time_to_take, title),
            fontFamily = GoogleSansFlex,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(80.dp))

        if (useSlider) {
            AlarmSlider(
                onTake = onTake,
                onSnooze = onSnooze,
                modifier = Modifier.widthIn(max = 400.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.alarm_swipe_to_respond),
                fontFamily = GoogleSansFlex,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        } else {
            Row(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedAlarmButton(
                    text = stringResource(R.string.alarm_snooze_action, snoozeDuration),
                    icon = Icons.Rounded.Snooze,
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f),
                    isPrimary = false
                )

                AnimatedAlarmButton(
                    text = if (isGrouped) stringResource(R.string.notif_action_take_all) else stringResource(R.string.alarm_taken_action),
                    icon = Icons.Rounded.Check,
                    onClick = onTake,
                    modifier = Modifier.weight(1f),
                    isPrimary = true
                )
            }
        }
    }
}

@Composable
fun AlarmSlider(
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var containerWidth by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val thumbWidth = 96.dp
    val thumbWidthPx = with(density) { thumbWidth.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Snooze,
                contentDescription = stringResource(R.string.alarm_snooze_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.alarm_taken_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .width(thumbWidth)
                .height(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val maxDrag = (containerWidth - thumbWidthPx) / 2f
                            val threshold = maxDrag * 0.6f
                            scope.launch {
                                if (offsetX.value > threshold) {
                                    offsetX.animateTo(maxDrag, spring())
                                    onTake()
                                } else if (offsetX.value < -threshold) {
                                    offsetX.animateTo(-maxDrag, spring())
                                    onSnooze()
                                } else {
                                    offsetX.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val maxDrag = (containerWidth - thumbWidthPx) / 2f
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + dragAmount).coerceIn(
                                        -maxDrag,
                                        maxDrag
                                    )
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}