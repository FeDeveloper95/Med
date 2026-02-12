@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.MainActivity

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.AVAILABLE_ICONS
import com.fedeveloper95.med.ExpressiveTextButton
import com.fedeveloper95.med.R
import com.fedeveloper95.med.ui.theme.GoogleSansFlex

@Composable
fun IconPickerDialog(currentIcon: String, onDismiss: () -> Unit, onIconSelected: (String) -> Unit) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.choose_icon), fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                val rows = displayIcons.chunked(4)
                rows.forEachIndexed { index, rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((name, icon) in rowItems) {
                            val isSelected = currentIcon == name

                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val cornerPercent by animateIntAsState(
                                targetValue = if (isPressed) 15 else 50,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
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
                                    .clickable(interactionSource = interactionSource, indication = null) { onIconSelected(name) },
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
                    }
                    if (index < rows.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { ExpressiveTextButton(onClick = onDismiss, text = stringResource(R.string.cancel_action)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp)
    )
}