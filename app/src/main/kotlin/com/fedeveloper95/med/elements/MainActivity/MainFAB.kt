@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class,
    ExperimentalTextApi::class
)

package com.fedeveloper95.med.elements.MainActivity

import android.graphics.Color.parseColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.ExperimentalTextApi
import com.fedeveloper95.med.ItemType
import com.fedeveloper95.med.R
import com.fedeveloper95.med.TooltipPosition
import com.fedeveloper95.med.rememberCustomTooltipPositionProvider
import com.fedeveloper95.med.ui.theme.GoogleSansFlex

@Composable
fun MainFAB(
    fabMenuExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuItems: List<Triple<ItemType, androidx.compose.ui.graphics.vector.ImageVector, Triple<String, String?, String?>>>,
    onMenuItemClick: (ItemType, String, String?, String?) -> Unit
) {
    FloatingActionButtonMenu(
        expanded = fabMenuExpanded,
        button = {
            val tooltipPos = if (fabMenuExpanded) TooltipPosition.Start else TooltipPosition.Above
            val expandedString = stringResource(R.string.expanded_state)
            val collapsedString = stringResource(R.string.collapsed_state)
            val menuActionDesc = stringResource(R.string.menu_action_desc)
            TooltipBox(
                positionProvider = rememberCustomTooltipPositionProvider(tooltipPos),
                tooltip = { PlainTooltip { Text(stringResource(R.string.menu_tooltip), fontFamily = GoogleSansFlex) } },
                state = rememberTooltipState()
            ) {
                ToggleFloatingActionButton(
                    modifier = Modifier
                        .semantics {
                            traversalIndex = -1f
                            stateDescription = if (fabMenuExpanded) expandedString else collapsedString
                            contentDescription = menuActionDesc
                        }
                        .animateFloatingActionButton(visible = true, alignment = Alignment.BottomEnd)
                        .focusRequester(remember { FocusRequester() }),
                    checked = fabMenuExpanded,
                    onCheckedChange = { onExpandedChange(!fabMenuExpanded) }
                ) {
                    val imageVector by remember { derivedStateOf { if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add } }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = null,
                        modifier = Modifier.animateIcon({ checkedProgress })
                    )
                }
            }
        }
    ) {
        val closeMenuString = stringResource(R.string.close_menu_action)
        menuItems.forEachIndexed { i, item ->
            val (name, iconName, colorCode) = item.third

            FloatingActionButtonMenuItem(
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                    if (i == menuItems.size - 1) customActions =
                        listOf(CustomAccessibilityAction(label = closeMenuString, action = { onExpandedChange(false); true }))
                },
                onClick = {
                    onExpandedChange(false)
                    onMenuItemClick(item.first, name, iconName, colorCode)
                },
                icon = {
                    if (colorCode != null && colorCode != "dynamic") {
                        val color = try {
                            Color(parseColor(colorCode))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Icon(item.second, contentDescription = null, tint = color)
                    } else {
                        Icon(item.second, contentDescription = null)
                    }
                },
                text = { Text(text = name, fontFamily = GoogleSansFlex) }
            )
        }
    }
}