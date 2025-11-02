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
import com.proyecto.ganapp.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalsScreen(
    userId: Long,
    onAddAnimal: () -> Unit,
    viewModel: AnimalsViewModel = hiltViewModel()
) {
    val animals by viewModel.animals.collectAsState()

    // Cargar los animales del usuario actual
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            items(animals) { animal ->
                AnimalCard(animal = animal)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (animals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No tienes animales registrados 🐮", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimalCard(animal: Animal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FFF9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!animal.fotoUri.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = animal.fotoUri),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = getAnimalImageRes(animal.nombre)),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(animal.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${animal.edad} años • ${animal.peso}kg • ${animal.color}",
                    fontSize = 14.sp, color = Color.Gray)
            }

            Icon(
                imageVector = Icons.Default.Vaccines,
                contentDescription = "Ver vacunación",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun getAnimalImageRes(nombre: String): Int {
    return when {
        nombre.contains("vaca", true) || nombre.contains("manch", true) -> R.drawable.def_vaca
        nombre.contains("cerdo", true) || nombre.contains("ros", true) -> R.drawable.def_cerdo
        nombre.contains("pollo", true) -> R.drawable.def_gallina
        nombre.contains("cabra", true) -> R.drawable.def_oveja
        else -> R.drawable.def_oveja
    }
}
