package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.AnimalDefaults
import com.proyecto.ganapp.domain.model.EspecieAnimal

data class AnimalDetailRow(
    val label: String,
    val value: String,
)

fun animalSpeciesLabel(especie: EspecieAnimal): String {
    return when (especie) {
        EspecieAnimal.VACA -> "Vaca"
        EspecieAnimal.CERDO -> "Cerdo"
        EspecieAnimal.OVEJA -> "Oveja"
        EspecieAnimal.SIN_ESPECIFICAR -> "Sin especificar"
    }
}

fun shouldShowBreed(raza: String): Boolean {
    return raza != AnimalDefaults.RAZA_SIN_ESPECIFICAR
}

fun animalBreedLabel(raza: String): String {
    return "Raza · $raza"
}

fun buildAnimalDetailRows(animal: Animal): List<AnimalDetailRow> {
    return buildList {
        animal.edad?.let { edad ->
            add(AnimalDetailRow(label = "Edad", value = formatAnimalAge(edad)))
        }
        animal.peso?.let { peso ->
            add(AnimalDetailRow(label = "Peso", value = "$peso kg"))
        }
        val color = animal.color
        if (color != null && color.isNotBlank()) {
            add(AnimalDetailRow(label = "Color", value = color))
        }
    }
}

private fun formatAnimalAge(edad: Int): String {
    return if (edad == 1) "1 año" else "$edad años"
}
