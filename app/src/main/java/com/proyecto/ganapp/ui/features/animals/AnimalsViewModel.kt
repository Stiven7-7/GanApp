package com.proyecto.ganapp.ui.features.animals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.repository.AnimalRepository
import com.proyecto.ganapp.domain.usecase.animal.GetAnimalsUseCase
import com.proyecto.ganapp.domain.usecase.animal.InsertAnimalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimalsViewModel @Inject constructor(
    private val getAnimalsUseCase: GetAnimalsUseCase,
    private val insertAnimalUseCase: InsertAnimalUseCase,
    private val animalRepository: AnimalRepository
) : ViewModel() {

    private val _animals = MutableStateFlow<List<Animal>>(emptyList())
    val animals: StateFlow<List<Animal>> = _animals

    fun loadAnimals(userId: Long) {
        viewModelScope.launch {
            animalRepository.getAnimalsByUser(userId).collect { list ->
                _animals.value = list
            }
        }
    }

    fun insertAnimal(nombre: String, edad: Int, peso: Float, color: String, idUsuario: Long, fotoUri: String?) {
        viewModelScope.launch {
            val animal = Animal(
                nombre = nombre,
                edad = edad,
                peso = peso,
                color = color,
                idUsuario = idUsuario,
                fotoUri = fotoUri
            )
            animalRepository.insertAnimal(animal)
        }
    }
}
