package com.example.ui.settings.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    testTag: String? = null
) {
    val fieldModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF90A4AE)) },
        placeholder = if (placeholder != null) { { Text(placeholder, color = Color.Gray) } } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF25D366),
            unfocusedBorderColor = Color(0xFF37474F),
            focusedLabelColor = Color(0xFF25D366),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedPlaceholderColor = Color.Gray,
            unfocusedPlaceholderColor = Color.Gray
        ),
        singleLine = singleLine,
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = Color(0xFF90A4AE)) }
        } else null,
        modifier = fieldModifier,
        shape = RoundedCornerShape(12.dp)
    )
}
