package com.proyecto.ganapp.ui.features.animals

import com.proyecto.ganapp.R
import com.proyecto.ganapp.domain.model.Animal
import com.proyecto.ganapp.domain.model.EspecieAnimal

fun buildAnimalMetadata(animal: Animal): String? {
    val segments = buildList {
        animal.edad?.let { edad -> add("$edad años") }
        animal.peso?.let { peso -> add("$peso kg") }
        val color = animal.color
        if (color != null && color.isNotBlank()) {
            add(color)
        }
    }
    if (segments.isEmpty()) return null
    return segments.joinToString(separator = " • ")
}

fun getAnimalImageRes(especie: EspecieAnimal): Int? {
    return when (especie) {
        EspecieAnimal.VACA -> R.drawable.def_vaca
        EspecieAnimal.CERDO -> R.drawable.def_cerdo
        EspecieAnimal.OVEJA -> R.drawable.def_oveja
        EspecieAnimal.SIN_ESPECIFICAR -> null
    }
}
