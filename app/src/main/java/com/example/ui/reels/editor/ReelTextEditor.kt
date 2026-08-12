package com.example.ui.reels.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReelTextEditor(
    initialText: String,
    onApply: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Editar texto")
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            label = { Text("Texto") }
        )
        Row(Modifier.fillMaxWidth()) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("Cancelar") }
            Button(onClick = { onApply(text) }, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("Aplicar") }
        }
    }
}
