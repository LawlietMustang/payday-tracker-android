package com.paydaytracker.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paydaytracker.app.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()

    var name by remember(profile) { mutableStateOf(profile.name) }
    var street by remember(profile) { mutableStateOf(profile.street) }
    var city by remember(profile) { mutableStateOf(profile.city) }
    var taxId by remember(profile) { mutableStateOf(profile.taxId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Persönliches Profil") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Vollständiger Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("Straße & Hausnummer") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("PLZ & Ort") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = taxId,
                onValueChange = { taxId = it },
                label = { Text("Steuer-ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.saveProfile(
                        profile.copy(
                            name = name.trim(),
                            street = street.trim(),
                            city = city.trim(),
                            taxId = taxId.trim()
                        )
                    )
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Profil speichern")
            }
        }
    }
}
