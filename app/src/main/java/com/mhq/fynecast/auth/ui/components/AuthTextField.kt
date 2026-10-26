package com.mhq.fynecast.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mhq.fynecast.R

/**
 * A slim 52 dp glass field: icon, hint, optional trailing action.
 * The focus ring uses the accent colour and an error turns the border red.
 * [label] is not drawn; it is what screen readers announce for the field.
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    isError: Boolean,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val onBackground = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.onPrimary
    val error = MaterialTheme.colorScheme.error
    val shape = RoundedCornerShape(16.dp)

    val borderColor = when {
        isError -> error.copy(alpha = 0.85f)
        isFocused -> accent.copy(alpha = 0.6f)
        else -> onBackground.copy(alpha = 0.12f)
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = onBackground),
        cursorBrush = SolidColor(if (isError) error else accent),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(onBackground.copy(alpha = if (isFocused) 0.09f else 0.06f))
                    .border(1.dp, borderColor, shape)
                    .padding(start = 14.dp, end = if (trailingContent != null) 4.dp else 14.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isFocused) accent else onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = onBackground.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
                trailingContent?.invoke()
            }
        }
    )
}

/** The eye button at the end of a password field. */
@Composable
fun AuthPasswordToggle(
    isPasswordVisible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onToggle)
    ) {
        Icon(
            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = stringResource(
                if (isPasswordVisible) R.string.hide_password else R.string.show_password
            ),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
    }
}
