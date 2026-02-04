package com.radio.ccbes.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radio.ccbes.ui.theme.RedAccent

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val reportReasons = listOf(
        "Spam o contenido engañoso",
        "Discurso de odio o acoso",
        "Contenido sexualmente explícito",
        "Violencia o contenido gráfico",
        "Información errónea",
        "Otro"
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Reportar contenido", fontSize = 18.sp) },
        text = {
            Column(Modifier.selectableGroup()) {
                reportReasons.forEach { reason ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (reason == selectedReason),
                                onClick = { selectedReason = reason },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (reason == selectedReason),
                            onClick = null, // null because the row handles the click
                            colors = RadioButtonDefaults.colors(selectedColor = RedAccent)
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReport(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
            ) {
                Text("Reportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = RedAccent)
            }
        }
    )
}
