package com.mhq.fynecast.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.ui.theme.FyneCastTheme
import com.mhq.fynecast.ui.theme.LilacBlue
import com.mhq.fynecast.ui.theme.MidnightBlue

@Composable
fun AuthInputField(
    value: String,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MidnightBlue
            )
        },
        trailingIcon = trailingIcon,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MidnightBlue,
            unfocusedTextColor = MidnightBlue,
            focusedLabelColor = MidnightBlue,
            focusedBorderColor = MidnightBlue,
            focusedContainerColor = LilacBlue,
            unfocusedLabelColor = MidnightBlue.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.DarkGray,
            unfocusedContainerColor = LilacBlue,
        ),
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
        )
}

@Preview
@Composable
private fun AuthInputFieldPreview() {
    FyneCastTheme {
        AuthInputField(
            value = "value",
            label = "label",
            placeholder = "placeholder",
            leadingIcon = Icons.Default.AccessAlarm,
            trailingIcon = null,
            isError = false,
            onValueChange = {}
        )
    }
}