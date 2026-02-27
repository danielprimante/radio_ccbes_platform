package com.radio.ccbes.ui.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.News,
        Screen.Radio,
        Screen.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->

            val interactionSource = remember { MutableInteractionSource() }

            NavigationBarItem(
                icon = { screen.icon?.let { Icon(imageVector = it, contentDescription = screen.title) } },
                selected = currentRoute?.startsWith(screen.navRoute) == true,
                interactionSource = interactionSource,
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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray
                )
            )
        }
    }
}
