package com.proyecto.ganapp.ui.features.animals

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.proyecto.ganapp.domain.model.EspecieAnimal
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

@Composable
internal fun AnimalSpeciesChip(
    especie: EspecieAnimal,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = animalSpeciesLabel(especie),
            modifier = Modifier.padding(
                horizontal = GanAppSpacing.sm,
                vertical = GanAppSpacing.xxs,
            ),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
