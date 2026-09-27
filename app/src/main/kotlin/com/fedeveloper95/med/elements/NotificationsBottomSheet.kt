@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med.elements

import android.content.Intent
import android.graphics.Color.parseColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.AVAILABLE_ICONS
import com.fedeveloper95.med.PREF_THEME
import com.fedeveloper95.med.R
import com.fedeveloper95.med.THEME_SYSTEM
import com.fedeveloper95.med.services.DataRepository
import com.fedeveloper95.med.services.NotificationReceiver
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.launch

class NotificationBottomSheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemTitle = intent.getStringExtra("ITEM_TITLE") ?: getString(R.string.alarm_default_title)
        val itemIds = intent.getLongArrayExtra("ITEM_IDS") ?: run {
            val singleId = intent.getLongExtra("ITEM_ID", -1L)
            if (singleId != -1L) longArrayOf(singleId) else longArrayOf()
        }
        val notifId = intent.getIntExtra("NOTIF_ID", -1)

        val items = try { DataRepository.loadData(this) } catch (e: Exception) { emptyList() }
        val currentItem = items.find { it.id == itemIds.firstOrNull() }
        val iconName = currentItem?.iconName
        val colorCodeStr = currentItem?.colorCode

        setContent {
            val prefs = getSharedPreferences("med_settings", MODE_PRIVATE)
            val currentTheme = prefs.getInt(PREF_THEME, THEME_SYSTEM)

            MedTheme(themeOverride = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    NotificationsBottomSheet(
                        medicineName = itemTitle,
                        iconName = iconName,
                        colorCode = colorCodeStr,
                        onTake = {
                            sendBroadcastAction("ACTION_TAKEN", itemIds, notifId)
                            finish()
                        },
                        onSkip = {
                            sendBroadcastAction("ACTION_SKIP", itemIds, notifId)
                            finish()
                        },
                        onSnooze = {
                            sendBroadcastAction("ACTION_SNOOZE", itemIds, notifId)
                            finish()
                        },
                        onDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun sendBroadcastAction(actionStr: String, itemIds: LongArray, notifId: Int) {
        val actionIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = actionStr
            putExtra("ITEM_IDS", itemIds)
            putExtra("NOTIF_ID", notifId)
        }
        sendBroadcast(actionIntent)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}

@Composable
fun NotificationsBottomSheet(
    medicineName: String,
    iconName: String?,
    colorCode: String?,
    onTake: () -> Unit,
    onSkip: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val closeSheet: (action: () -> Unit) -> Unit = { action ->
        scope.launch {
            sheetState.hide()
            action()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        scrimColor = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
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

                val customColor = remember(colorCode) {
                    if (colorCode != null && colorCode != "dynamic") try {
                        Color(parseColor(colorCode))
                    } catch (e: Exception) {
                        null
                    } else null
                }

                val containerColor = customColor ?: MaterialTheme.colorScheme.tertiaryContainer
                val onContainerColor = if (customColor != null) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onTertiaryContainer

                Icon(
                    painter = painterResource(id = R.drawable.ic_twelve_sided_cookie),
                    contentDescription = null,
                    tint = containerColor,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .rotate(rotation)
                )

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

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onContainerColor,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = medicineName,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                OutlinedToggleButton(
                    checked = false,
                    onCheckedChange = { closeSheet(onSkip) },
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = OutlinedToggleButtonDefaults.colors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_skip),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedToggleButton(
                    checked = false,
                    onCheckedChange = { closeSheet(onSnooze) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = OutlinedToggleButtonDefaults.colors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_snooze),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val takeInteractionSource = remember { MutableInteractionSource() }
            val isTakePressed by takeInteractionSource.collectIsPressedAsState()
            val takeCorner by animateIntAsState(
                targetValue = if (isTakePressed) 15 else 50,
                animationSpec = spring(),
                label = "takeCorner"
            )

            Button(
                onClick = { closeSheet(onTake) },
                shape = RoundedCornerShape(takeCorner),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                interactionSource = takeInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_take),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}