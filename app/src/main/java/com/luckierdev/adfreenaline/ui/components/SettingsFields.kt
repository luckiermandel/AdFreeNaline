package com.luckierdev.adfreenaline.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Text field that commits its value when focus is lost, IME "done" is pressed,
 * the keyboard is dismissed, or back is pressed.
 */
@Composable
fun DeferredCommitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    keyboardType: KeyboardType,
    onApply: () -> Unit,
    onFocusedChange: (Boolean) -> Unit = {},
    settingsResetVersion: Int = 0,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(settingsResetVersion) {
        focused = false
    }
    val exit = {
        onApply()
        focusManager.clearFocus()
    }
    BackHandler(enabled = focused) { exit() }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var wasKeyboardVisible by remember { mutableStateOf(imeBottom > 0) }
    LaunchedEffect(imeBottom, focused) {
        val keyboardVisible = imeBottom > 0
        if (wasKeyboardVisible && !keyboardVisible && focused) {
            exit()
        }
        wasKeyboardVisible = keyboardVisible
    }
    DisposableEffect(Unit) {
        onDispose {
            if (focused) onApply()
        }
    }
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.replace("\n", "")) },
        singleLine = true,
        label = label,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onDone = { exit() }),
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key == Key.Back) {
                    exit()
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { focus ->
                focused = focus.isFocused
                onFocusedChange(focus.isFocused)
                if (!focus.isFocused) onApply()
            }
    )
}

@Composable
fun SettingsIntField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    settingsResetVersion: Int,
    keyboardType: KeyboardType = KeyboardType.Number,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var text by rememberSaveable(settingsResetVersion) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!focused) {
            text = value.toString()
        }
    }
    val apply = {
        val snapped = text.toIntOrNull() ?: value
        text = snapped.toString()
        onValueChange(snapped)
    }
    DeferredCommitTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        keyboardType = keyboardType,
        onApply = apply,
        onFocusedChange = { focused = it },
        settingsResetVersion = settingsResetVersion,
        modifier = modifier
    )
}

@Composable
fun SettingsDoubleField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    settingsResetVersion: Int,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var text by rememberSaveable(settingsResetVersion) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!focused) {
            text = value.toString()
        }
    }
    val apply = {
        val snapped = text.toDoubleOrNull() ?: value
        text = snapped.toString()
        onValueChange(snapped)
    }
    DeferredCommitTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        keyboardType = KeyboardType.Decimal,
        onApply = apply,
        onFocusedChange = { focused = it },
        settingsResetVersion = settingsResetVersion,
        modifier = modifier
    )
}
