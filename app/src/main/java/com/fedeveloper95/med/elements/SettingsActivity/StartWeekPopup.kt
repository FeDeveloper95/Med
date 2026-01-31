package com.fedeveloper95.med.elements.SettingsActivity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fedeveloper95.med.R

@Composable
fun StartWeekPopup(
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ExpressiveSingleChoiceDialog(
        icon = Icons.Default.DateRange,
        title = stringResource(R.string.settings_week_start_title),
        options = listOf(
            stringResource(R.string.monday),
            stringResource(R.string.sunday)
        ),
        selectedIndex = selectedIndex,
        onOptionSelected = onOptionSelected,
        onDismiss = onDismiss
    )
}