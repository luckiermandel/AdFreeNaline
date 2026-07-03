package com.luckierdev.adfreenaline.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.luckierdev.adfreenaline.BiologicalSex
import com.luckierdev.adfreenaline.R
import com.luckierdev.adfreenaline.ui.theme.Dimens

@Composable
fun OnboardingDialog(
    onSave: (BiologicalSex, Int, Int) -> Unit,
    onSkip: () -> Unit
) {
    var sex by rememberSaveable { mutableStateOf(BiologicalSex.MALE) }
    var age by rememberSaveable { mutableStateOf("30") }
    var height by rememberSaveable { mutableStateOf("175") }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.onboarding_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                Text(stringResource(R.string.onboarding_body))
                Text(
                    stringResource(R.string.onboarding_profile_explanation),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    stringResource(R.string.calorie_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                    SexChoiceButton(
                        label = stringResource(R.string.sex_male),
                        selected = sex == BiologicalSex.MALE,
                        modifier = Modifier.weight(1f)
                    ) { sex = BiologicalSex.MALE }
                    SexChoiceButton(
                        label = stringResource(R.string.sex_female),
                        selected = sex == BiologicalSex.FEMALE,
                        modifier = Modifier.weight(1f)
                    ) { sex = BiologicalSex.FEMALE }
                }
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text(stringResource(R.string.label_age)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text(stringResource(R.string.label_height_cm)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(sex, age.toIntOrNull() ?: 30, height.toIntOrNull() ?: 175) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Button(onClick = onSkip) { Text(stringResource(R.string.action_skip)) }
        }
    )
}

@Composable
private fun SexChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) { Text(label) }
}
