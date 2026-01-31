@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.SettingsActivity

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.R
import com.fedeveloper95.med.UpdateStatus
import com.fedeveloper95.med.ui.theme.GoogleSansFlex

@Composable
fun AnimatedUpdateButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btnMorph"
    )

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(cornerPercent),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        interactionSource = interactionSource
    ) {
        Text(
            text = stringResource(R.string.update_action),
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun AnimatedCheckAgainButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "btnMorph"
    )

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(cornerPercent),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        interactionSource = interactionSource
    ) {
        Text(
            text = stringResource(R.string.settings_check_updates_title),
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateDialog(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit,
    onCheckAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        when(status) {
                            is UpdateStatus.Error -> MaterialTheme.colorScheme.errorContainer
                            is UpdateStatus.Available -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(status) {
                        is UpdateStatus.Error -> Icons.Default.Error
                        is UpdateStatus.Available -> Icons.Default.SystemUpdate
                        else -> Icons.Default.CloudDownload
                    },
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = when(status) {
                        is UpdateStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                        is UpdateStatus.Available -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        title = {
            Text(
                text = when(status) {
                    is UpdateStatus.Checking -> stringResource(R.string.update_checking)
                    is UpdateStatus.NoUpdate -> stringResource(R.string.update_no_update)
                    is UpdateStatus.Available -> stringResource(R.string.update_available, status.info.version)
                    is UpdateStatus.Error -> stringResource(R.string.update_error)
                    else -> ""
                },
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when(status) {
                    is UpdateStatus.Checking -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        LoadingIndicator(
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is UpdateStatus.NoUpdate -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.update_latest_version_msg),
                            fontFamily = GoogleSansFlex,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    is UpdateStatus.Available -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = status.info.changelog,
                                    fontFamily = GoogleSansFlex,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    is UpdateStatus.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.update_error_msg),
                            fontFamily = GoogleSansFlex,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (status is UpdateStatus.Available) {
                AnimatedUpdateButton(onClick = { onUpdate(status.info.downloadUrl) })
            } else if (status is UpdateStatus.NoUpdate || status is UpdateStatus.Error) {
                AnimatedCheckAgainButton(onClick = onCheckAgain)
            }
        },
        dismissButton = {
            if (status is UpdateStatus.Available) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.later),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (status is UpdateStatus.NoUpdate || status is UpdateStatus.Error) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.close),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 6.dp
    )
}