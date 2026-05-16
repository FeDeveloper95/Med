@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.AdvancedSettingsActivity

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fedeveloper95.med.R
import com.fedeveloper95.med.elements.SettingsActivity.TextButtonWithAnimatedShape
import com.fedeveloper95.med.ui.theme.GoogleSansFlex

@Composable
fun RestorePopup(
    onDismiss: () -> Unit,
    onRestart: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnMorph"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = {
            Text(
                text = stringResource(R.string.dialog_restart_title),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_restart_desc),
                fontFamily = GoogleSansFlex
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 6.dp,
        confirmButton = {
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(cornerPercent),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.restart_now),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButtonWithAnimatedShape(
                onClick = onDismiss,
                text = stringResource(R.string.cancel)
            )
        }
    )
}