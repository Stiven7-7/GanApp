package com.proyecto.ganapp.ui.navigation

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.ganapp.R
import com.proyecto.ganapp.ui.common.theme.GanAppSpacing

internal sealed interface GanAppNavIcon {
    data class Vector(val imageVector: ImageVector) : GanAppNavIcon
    data class Resource(@DrawableRes val resId: Int) : GanAppNavIcon
}

internal enum class GanAppTopLevelDestination(
    val route: String,
    val label: String,
    val icon: GanAppNavIcon,
) {
    Home(
        route = Screen.Home.route,
        label = "Inicio",
        icon = GanAppNavIcon.Vector(Icons.Outlined.Home),
    ),
    Animals(
        route = Screen.Animals.route,
        label = "Animales",
        icon = GanAppNavIcon.Resource(R.drawable.def_vaca),
    ),
    Notifications(
        route = Screen.Notifications.route,
        label = "Notificaciones",
        icon = GanAppNavIcon.Vector(Icons.Outlined.Notifications),
    ),
    ;

    companion object {
        fun fromRoute(route: String?): GanAppTopLevelDestination? {
            return entries.firstOrNull { it.route == route }
        }
    }
}

@Composable
internal fun GanAppNavigationBar(
    currentRoute: String?,
    onDestinationSelected: (GanAppTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = GanAppTopLevelDestination.fromRoute(currentRoute)
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp,
    ) {
        GanAppTopLevelDestination.entries.forEach { destination ->
            val isSelected = destination == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onDestinationSelected(destination) },
                alwaysShowLabel = true,
                icon = {
                    when (val icon = destination.icon) {
                        is GanAppNavIcon.Vector -> Icon(
                            imageVector = icon.imageVector,
                            contentDescription = null,
                        )
                        is GanAppNavIcon.Resource -> Icon(
                            painter = painterResource(icon.resId),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 0.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.heightIn(min = GanAppSpacing.xxl),
            )
        }
    }
}
