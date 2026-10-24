package com.proyecto.ganapp.ui.features.animals

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.proyecto.ganapp.domain.model.Animal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalsScreen(
    userId: Long,
    onAddAnimal: () -> Unit,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit,
    viewModel: AnimalsViewModel = hiltViewModel()
) {
    val listState by viewModel.listUiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadAnimals(userId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Mis Animales") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAnimal,
                containerColor = Color(0xFF00E676)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar animal", tint = Color.White)
            }
        }
    ) { padding ->
        when (val state = listState) {
            AnimalsListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            AnimalsListUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No tienes animales registrados", fontSize = 16.sp)
                }
            }

            is AnimalsListUiState.Error -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No fue posible cargar los animales.", fontSize = 16.sp)
                }
            }

            is AnimalsListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    items(state.animals) { animal ->
                        AnimalCard(
                            animal = animal,
                            onAssignNotification = { animalId ->
                                onAssignNotification(animalId)
                            },
                            onViewNotifications = { animalId ->
                                onViewNotifications(animalId)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

/* --------------------------------------------------------------------- */
/* ------------------------- TARJETA DE ANIMAL -------------------------- */
/* --------------------------------------------------------------------- */

@Composable
fun AnimalCard(
    animal: Animal,
    onAssignNotification: (Long) -> Unit,
    onViewNotifications: (Long) -> Unit
) {
    val metadata = buildAnimalMetadata(animal)
    val fallbackRes = if (animal.fotoUri.isNullOrEmpty()) {
        getAnimalImageRes(animal.especie)
    } else {
        null
    }
    val showImage = !animal.fotoUri.isNullOrEmpty() || fallbackRes != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FFF9))
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                if (!animal.fotoUri.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = animal.fotoUri),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (fallbackRes != null) {
                    Image(
                        painter = painterResource(id = fallbackRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                if (showImage) {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(animal.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (metadata != null) {
                        Text(
                            metadata,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Vaccines,
                    contentDescription = "Vacunación",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {

                Button(
                    onClick = { onAssignNotification(animal.idAnimal) },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("Asignar", color = Color.White)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { onViewNotifications(animal.idAnimal) },
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("Ver Notificaciones", color = Color.White)
                }
            }
        }
    }
}
