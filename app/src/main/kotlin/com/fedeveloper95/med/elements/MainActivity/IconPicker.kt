@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.MainActivity

import android.graphics.Color.parseColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.AVAILABLE_COLORS
import com.fedeveloper95.med.AVAILABLE_ICONS
import com.fedeveloper95.med.ExpressiveTextButton
import com.fedeveloper95.med.R
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.darken

@Composable
fun IconPickerDialog(
    currentIcon: String,
    currentColor: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val icSick = ImageVector.vectorResource(R.drawable.ic_sick)
    val icMind = ImageVector.vectorResource(R.drawable.ic_mind)
    val icMixture = ImageVector.vectorResource(R.drawable.ic_mixture)

    val displayIcons = remember(icSick, icMind, icMixture) {
        val tempMap = AVAILABLE_ICONS.toMutableMap()
        tempMap["MixtureMed"] = icSick
        tempMap["Bed"] = icMind
        tempMap["Mood"] = icMixture
        tempMap.toList()
    }

    var selectedIcon by remember { mutableStateOf(currentIcon) }
    var selectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 400.dp),
        title = {
            Text(
                text = stringResource(R.string.choose_icon),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val rows = displayIcons.chunked(4)
                rows.forEachIndexed { index, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for ((name, icon) in rowItems) {
                            val isSelected = selectedIcon == name

                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val cornerPercent by animateIntAsState(
                                targetValue = if (isPressed) 15 else 50,
                                animationSpec = tween(durationMillis = 200),
                                label = "corner"
                            )

                            val baseColor = MaterialTheme.colorScheme.surfaceVariant
                            val selectedColorBg = MaterialTheme.colorScheme.primaryContainer

                            val containerColor by animateColorAsState(if (isSelected) selectedColorBg else baseColor)
                            val iconTint by animateColorAsState(
                                if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(percent = cornerPercent))
                                    .background(containerColor)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { selectedIcon = name },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = iconTint,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        val remaining = 4 - rowItems.size
                        if (remaining > 0) {
                            repeat(remaining) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                    if (index < rows.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.select_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AVAILABLE_COLORS.forEach { colorCode ->
                        val isSelected = selectedColor == colorCode
                        val isDynamic = colorCode == "dynamic"

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val targetCorner = when {
                            isPressed -> 15
                            isSelected -> 35
                            else -> 50
                        }

                        val cornerPercent by animateIntAsState(
                            targetValue = targetCorner,
                            animationSpec = tween(durationMillis = 200),
                            label = "colorCorner"
                        )

                        val backgroundColor = if (isDynamic) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            try {
                                Color(parseColor(colorCode))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                        }

                        val borderWidth = if (isSelected) 3.dp else 0.dp
                        val borderColor = if (isDynamic) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            backgroundColor.darken(0.7f)
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(percent = cornerPercent))
                                .background(backgroundColor)
                                .then(
                                    if (isDynamic && isSelected) Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer
                                    ) else Modifier
                                )
                                .border(
                                    borderWidth,
                                    borderColor,
                                    RoundedCornerShape(percent = cornerPercent)
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { selectedColor = colorCode },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDynamic) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            ExpressiveTextButton(
                onClick = { onConfirm(selectedIcon, selectedColor) },
                text = stringResource(R.string.save_action)
            )
        },
        dismissButton = {
            ExpressiveTextButton(
                onClick = onDismiss,
                text = stringResource(R.string.cancel_action)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp)
    )
}