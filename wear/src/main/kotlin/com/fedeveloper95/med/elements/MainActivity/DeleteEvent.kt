package com.fedeveloper95.med.elements.MainActivity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text

@Composable
fun DeleteElement(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = "Delete $itemName?",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = onConfirm
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = onDismiss
            )
        }
    )
}