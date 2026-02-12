@file:OptIn(ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.SettingsActivity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.R
import com.fedeveloper95.med.ui.theme.GoogleSansFlex

@Composable
fun ThemePopup(
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ExpressiveSingleChoiceDialog(
        icon = Icons.Default.Palette,
        title = stringResource(R.string.settings_theme_title),
        options = listOf(
            stringResource(R.string.settings_theme_system),
            stringResource(R.string.settings_theme_light),
            stringResource(R.string.settings_theme_dark)
        ),
        selectedIndex = selectedIndex,
        onOptionSelected = onOptionSelected,
        onDismiss = onDismiss
    )
}

@Composable
fun ExpressiveSingleChoiceDialog(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEachIndexed { index, text ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = (index == selectedIndex),
                                onClick = { onOptionSelected(index) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == selectedIndex),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = text,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.discard),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 6.dp
    )
}