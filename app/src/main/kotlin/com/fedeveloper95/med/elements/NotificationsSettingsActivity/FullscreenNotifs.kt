@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.NotificationsSettingsActivity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.R
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.launch

var hasShownFullscreenWarningSession = false

@Composable
fun FullscreenNotifsHandler() {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasShownFullscreenWarningSession) return@LaunchedEffect

        val prefs = context.getSharedPreferences("med_settings", Context.MODE_PRIVATE)
        val fullScreenAlarmEnabled = prefs.getBoolean("pref_full_screen_alarm", true)
        val showNotifications = prefs.getBoolean("pref_show_notifications", true)

        val missingPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.canUseFullScreenIntent() == false
        } else {
            false
        }

        if (fullScreenAlarmEnabled && showNotifications && missingPermission) {
            showSheet = true
            hasShownFullscreenWarningSession = true
        }
    }

    if (showSheet) {
        FullscreenNotifsBottomSheet(
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
fun FullscreenNotifsBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val cancelInteractionSource = remember { MutableInteractionSource() }
    val saveInteractionSource = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteractionSource.collectIsPressedAsState()
    val isSavePressed by saveInteractionSource.collectIsPressedAsState()

    val cancelCorner by animateIntAsState(
        targetValue = if (isCancelPressed) 15 else 50,
        animationSpec = tween(durationMillis = 200),
        label = "cancelCorner"
    )
    val saveCorner by animateIntAsState(
        targetValue = if (isSavePressed) 15 else 50,
        animationSpec = tween(durationMillis = 200),
        label = "saveCorner"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.fullscreen_permission_title),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.fullscreen_permission_desc),
                fontFamily = GoogleSansFlex,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        stringResource(R.string.no_thanks),
                        fontFamily = GoogleSansFlex,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
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
                        stringResource(R.string.enable_action),
                        fontFamily = GoogleSansFlex,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}