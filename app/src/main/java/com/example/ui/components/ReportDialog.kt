package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReportContentDialog(
  title: String,
  onDismiss: () -> Unit,
  onSubmit: (reason: String, details: String) -> Unit
) {
  val reasons = listOf(
    "Spam or misleading",
    "Harassment or hate speech",
    "Inappropriate content",
    "Copyright or impersonation",
    "Safety concern"
  )
  var selectedReason by remember { mutableStateOf(reasons.first()) }
  var details by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        reasons.forEach { reason ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedReason = reason }
              .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = selectedReason == reason,
              onClick = { selectedReason = reason }
            )
            Text(reason, style = MaterialTheme.typography.bodyMedium)
          }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
          value = details,
          onValueChange = { details = it.take(300) },
          label = { Text("Additional context (optional)") },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 3
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onSubmit(selectedReason, details) }) {
        Text("Submit Report", color = MaterialTheme.colorScheme.error)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
