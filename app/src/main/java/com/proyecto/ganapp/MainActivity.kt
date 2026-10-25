package com.proyecto.ganapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.proyecto.ganapp.ui.common.theme.GanAppTheme
import com.proyecto.ganapp.ui.navigation.RootNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GanAppTheme {
                RootNavigation()
            }
        }
    }
}
