package com.proyecto.ganapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.proyecto.ganapp.ui.common.theme.GanAppTheme
import com.proyecto.ganapp.ui.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GanAppTheme {
                NavGraph()  // ⬅️ Carga el grafo de navegación (Login → Animales → Notificaciones)
            }
        }
    }
}
