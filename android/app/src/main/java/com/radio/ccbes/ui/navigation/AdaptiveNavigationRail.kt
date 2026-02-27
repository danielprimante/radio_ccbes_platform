package com.radio.ccbes.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.radio.ccbes.ui.theme.RedAccent

/**
 * NavigationRail adaptativo para tablets
 * Muestra la navegación vertical en el lado izquierdo de la pantalla
 */
@Composable
fun AdaptiveNavigationRail(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.News,
        Screen.Radio,
        Screen.Profile
    )
    
    NavigationRail(
        containerColor = Color.White,
        modifier = Modifier.width(80.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationRailItem(
                icon = { 
                    screen.icon?.let { 
                        Icon(
                            imageVector = it, 
                            contentDescription = screen.title
                        ) 
                    } 
                },
                label = { Text(text = screen.title) },
                selected = currentRoute?.startsWith(screen.navRoute) == true,
                alwaysShowLabel = false,
                onClick = {
                    val destinationRoute = screen.navRoute
                    navController.navigate(destinationRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = (screen != Screen.Home)
                            inclusive = (screen == Screen.Home && currentRoute != Screen.Home.route)
                        }
                        launchSingleTop = true
                        restoreState = (screen != Screen.Home)
                    }
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = Color.White.copy(alpha = 0f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                )
            )
        }
    }
}
