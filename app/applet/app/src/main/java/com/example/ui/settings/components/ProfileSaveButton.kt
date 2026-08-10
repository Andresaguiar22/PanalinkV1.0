package com.example.ui.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSaveButton(
    onSave: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSave,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("profile_save_button"),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF0F2027), modifier = Modifier.size(24.dp))
        } else {
            Text(
                text = "Guardar Cambios de Perfil",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2027)
            )
        }
    }
}
