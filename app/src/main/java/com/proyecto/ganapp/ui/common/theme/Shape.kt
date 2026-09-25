package com.proyecto.ganapp.ui.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas Material 3 de GANAPP v1.
 * medium ≈ campos y contenedor de error, large ≈ cards, extraLarge ≈ diálogos.
 * extraSmall permanece en el default de Material 3 (4.dp).
 */
val GanAppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
