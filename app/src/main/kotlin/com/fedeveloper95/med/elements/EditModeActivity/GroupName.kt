@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)

package com.fedeveloper95.med.elements.EditModeActivity

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedeveloper95.med.R
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.launch

@Composable
fun GroupNameBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        stringResource(R.string.group_name_label),
                        fontFamily = GoogleSansFlex
                    )
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val cornerPercent by animateIntAsState(
                targetValue = if (isPressed) 15 else 50,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = ""
            )

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onConfirm(text)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp),
                shape = RoundedCornerShape(cornerPercent),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                interactionSource = interactionSource,
                enabled = text.isNotBlank()
            ) {
                Text(
                    text = stringResource(R.string.ok_action),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}