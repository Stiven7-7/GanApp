package com.proyecto.ganapp.ui.features.animals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.proyecto.ganapp.domain.model.Animal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalsScreen(
    userId: Long,
    viewModel: AnimalsViewModel = hiltViewModel()
) {
    val animals by viewModel.animals.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAnimals(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis animales") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(animals) { animal ->
                AnimalCard(animal)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AnimalCard(animal: Animal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(animal.nombre, style = MaterialTheme.typography.titleMedium)
            Text("Edad: ${animal.edad} años")
            Text("Peso: ${animal.peso} kg")
            Text("Color: ${animal.color}")
        }
    }
}
