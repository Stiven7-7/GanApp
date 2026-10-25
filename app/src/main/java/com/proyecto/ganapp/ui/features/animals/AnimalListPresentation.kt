package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.R
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal

data class AnimalCardMetadata(
    val lineOne: String?,
    val lineTwo: String?,
)

fun buildAnimalCardMetadata(animal: Animal): AnimalCardMetadata? {
    val breed = animal.raza.takeIf { shouldShowBreed(it) }
    val age = animal.edad?.let(::formatAnimalCardAge)
    val weight = animal.peso?.let { peso -> "$peso kg" }
    val color = animal.color?.takeIf { it.isNotBlank() }
    val lineOne = listOfNotNull(breed, age).joinToString(separator = " • ").ifEmpty { null }
    val lineTwo = listOfNotNull(weight, color).joinToString(separator = " • ").ifEmpty { null }
    if (lineOne == null && lineTwo == null) return null
    return AnimalCardMetadata(lineOne = lineOne, lineTwo = lineTwo)
}

fun getAnimalImageRes(especie: EspecieAnimal): Int? {
    return when (especie) {
        EspecieAnimal.VACA -> R.drawable.def_vaca
        EspecieAnimal.CERDO -> R.drawable.def_cerdo
        EspecieAnimal.OVEJA -> R.drawable.def_oveja
        EspecieAnimal.SIN_ESPECIFICAR -> null
    }
}

private fun formatAnimalCardAge(edad: Int): String {
    return if (edad == 1) "1 año" else "$edad años"
}
