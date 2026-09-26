package com.paydaytracker.app.ui.payslips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paydaytracker.app.data.PayrollCalculator
import com.paydaytracker.app.data.Payslip
import com.paydaytracker.app.ui.MainViewModel
import com.paydaytracker.app.ui.common.Formatters
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val payslips by viewModel.payslips.collectAsState()
    val workplaces by viewModel.workplaces.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var inputMonth by remember { mutableStateOf(viewModel.selectedMonth.value) }
    var inputWorkplaceId by remember { mutableStateOf(workplaces.firstOrNull()?.id ?: "default") }
    var inputGross by remember { mutableStateOf("") }
    var inputNet by remember { mutableStateOf("") }
    var wpDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lohnabrechnungen") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    inputMonth = viewModel.selectedMonth.value
                    inputWorkplaceId = workplaces.firstOrNull()?.id ?: "default"
                    inputGross = ""
                    inputNet = ""
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Lohnabrechnung hinzufügen")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (payslips.isEmpty()) {
                item {
                    Text(
                        text = "Noch keine Lohnabrechnungen gespeichert. Trage Brutto und Netto aus deiner Abrechnung ein, um sie mit deinen geschätzten Werten zu vergleichen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(payslips) { payslip ->
                    val s = PayrollCalculator.summary(
                        monthKey = payslip.month,
                        shifts = shifts,
                        settings = settings,
                        workplaces = workplaces,
                        filterWorkplaceId = payslip.workplaceId
                    )
                    val grossDiff = payslip.actualGross - s.gross
                    val netDiff = payslip.actualNet - s.est.net
                    val wpName = workplaces.firstOrNull { it.id == payslip.workplaceId }?.name ?: "Arbeitsplatz"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = wpName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = payslip.month, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deletePayslip(payslip.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Löschen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = "Brutto Schätzung / Ist", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${Formatters.formatMoney(s.gross, settings.currency)} / ${Formatters.formatMoney(payslip.actualGross, settings.currency)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = (if (grossDiff >= 0) "+ " else "") + Formatters.formatMoney(grossDiff, settings.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (grossDiff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = "Netto Schätzung / Ist", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${Formatters.formatMoney(s.est.net, settings.currency)} / ${Formatters.formatMoney(payslip.actualNet, settings.currency)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = (if (netDiff >= 0) "+ " else "") + Formatters.formatMoney(netDiff, settings.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netDiff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Lohnabrechnung eintragen") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputMonth,
                            onValueChange = { inputMonth = it },
                            label = { Text("Monat (JJJJ-MM)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        ExposedDropdownMenuBox(
                            expanded = wpDropdownExpanded,
                            onExpandedChange = { wpDropdownExpanded = !wpDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = workplaces.firstOrNull { it.id == inputWorkplaceId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Arbeitsplatz") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wpDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = wpDropdownExpanded,
                                onDismissRequest = { wpDropdownExpanded = false }
                            ) {
                                workplaces.forEach { wp ->
                                    DropdownMenuItem(
                                        text = { Text(wp.name) },
                                        onClick = {
                                            inputWorkplaceId = wp.id
                                            wpDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputGross,
                            onValueChange = { inputGross = it },
                            label = { Text("Tatsächliches Brutto (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = inputNet,
                            onValueChange = { inputNet = it },
                            label = { Text("Tatsächliches Netto (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val gross = inputGross.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val net = inputNet.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (inputMonth.isNotBlank() && gross > 0) {
                                viewModel.savePayslip(
                                    Payslip(
                                        id = UUID.randomUUID().toString(),
                                        month = inputMonth.trim(),
                                        workplaceId = inputWorkplaceId,
                                        actualGross = gross,
                                        actualNet = net
                                    )
                                )
                                showDialog = false
                            }
                        }
                    ) {
                        Text("Speichern")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Abbrechen")
                    }
                }
            )
        }
    }
}
