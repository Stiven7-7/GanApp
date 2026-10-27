package com.proyecto.ganapp.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * AppBar autenticada sobre el mismo lienzo que el contenido.
 * No cambia título, navegación ni acciones: solo el color del contenedor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ganAppTopAppBarColors(): TopAppBarColors {
    val canvas = MaterialTheme.colorScheme.background
    val content = MaterialTheme.colorScheme.onSurface
    return TopAppBarDefaults.topAppBarColors(
        containerColor = canvas,
        scrolledContainerColor = canvas,
        navigationIconContentColor = content,
        titleContentColor = content,
        actionIconContentColor = content,
    )
}
