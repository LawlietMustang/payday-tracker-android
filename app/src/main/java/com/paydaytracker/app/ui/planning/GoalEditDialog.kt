package com.paydaytracker.app.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paydaytracker.app.data.SavingsGoal
import java.time.LocalDate
import java.util.UUID

@Composable
fun GoalEditDialog(
    onDismiss: () -> Unit,
    onSaveGoal: (SavingsGoal) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }
    var auto by remember { mutableStateOf(false) }
    var autoAmount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neues Sparziel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name des Sparziels") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Zielbetrag (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = saved,
                    onValueChange = { saved = it },
                    label = { Text("Bereits gespart (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = auto,
                        onCheckedChange = { auto = it }
                    )
                    Text(text = "Automatisch monatlich zurücklegen")
                }

                if (auto) {
                    OutlinedTextField(
                        value = autoAmount,
                        onValueChange = { autoAmount = it },
                        label = { Text("Monatlicher Sparbeitrag (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedTarget = target.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val parsedSaved = saved.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val parsedAuto = autoAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && parsedTarget > 0) {
                        val nextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1).toString()
                        val newGoal = SavingsGoal(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                            target = parsedTarget,
                            saved = parsedSaved,
                            auto = auto,
                            autoInterval = "monthly",
                            autoAmount = parsedAuto,
                            autoAnchor = 1,
                            autoNext = if (auto) nextMonth else "",
                            autoEpoch = UUID.randomUUID().toString()
                        )
                        onSaveGoal(newGoal)
                    }
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
