package com.radio.ccbes.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.data.settings.SettingsManager
import com.radio.ccbes.ui.navigation.AdaptiveNavigationRail
import com.radio.ccbes.ui.navigation.AppNavigation
import com.radio.ccbes.ui.navigation.BottomNavigationBar
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.ScreenSizeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun MainScreen(
    initialPostId: String? = null,
    initialChatId: String? = null,
    windowSizeClass: WindowSizeClass
) {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf<String?>(null) }

    // Detectar si es tablet
    val isTablet = ScreenSizeUtils.isTablet(windowSizeClass)

    // Determinar el destino inicial
    LaunchedEffect(Unit) {
        // Esperar un momento para que Firebase Auth se estabilice
        delay(500)
        val user = auth.currentUser

        if (user == null) {
            startDestination = Screen.Welcome.route
        } else {
            try {
                val profile = userRepository.getUser(user.uid)
                if (profile != null) {
                    // Si el perfil existe, verificamos términos, privacidad y baneo
                    if (!profile.termsAccepted || !profile.privacyAccepted || profile.isBanned) {
                        startDestination = Screen.Welcome.route
                    } else {
                        startDestination = Screen.Home.route
                    }
                } else {
                    // Si el perfil es null, podría ser un error de red. 
                    // En lugar de cerrar sesión, permitimos entrar a Home si ya teníamos un usuario.
                    // El resto de la app manejará si faltan datos del perfil.
                    startDestination = Screen.Home.route
                }
            } catch (e: Exception) {
                // Ante cualquier error de red, preferimos dejarlo entrar si ya está autenticado en Firebase
                startDestination = Screen.Home.route
            }
        }

        // Autoplay radio if enabled
        if (startDestination == Screen.Home.route) {
            val autoPlay = settingsManager.autoPlayRadio.first()
            if (autoPlay) {
                val intent = android.content.Intent(context, com.radio.ccbes.data.service.RadioService::class.java)
                context.startService(intent)
            }
        }

        isLoading = false
    }

    // Handle deep link navigation from notification
    LaunchedEffect(initialPostId, initialChatId, isLoading) {
        if (!isLoading && startDestination == Screen.Home.route) {
            if (initialPostId != null) {
                navController.navigate(Screen.Comments.createRoute(initialPostId))
            } else if (initialChatId != null) {
                navController.navigate(Screen.Chat.createRoute(initialChatId))
            }
        }
    }

    // Escuchar cambios de autenticación
    LaunchedEffect(currentUser) {
        if (!isLoading && startDestination != null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentUser == null && currentRoute != Screen.Welcome.route) {
                try {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (isLoading || startDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RedAccent)
        }
    } else {
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val mainScreens = listOf(
            Screen.Home.route,
            Screen.Search.route,
            Screen.CreatePost.route,
            Screen.Radio.route,
            Screen.Profile.route,
            "profile" // Handle raw route without arguments if needed (though usually matched by pattern)
        )

        // Check if current route matches any of the main screens pattern
        val showBottomBar = currentRoute != null && mainScreens.any {
            currentRoute.startsWith(it.substringBefore("?"))
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar && !isTablet) {
                    Box(modifier = Modifier.windowInsetsPadding(NavigationBarDefaults.windowInsets)) {
                        BottomNavigationBar(navController = navController)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            if (isTablet && showBottomBar) {
                // Layout con NavigationRail para tablets
                Row(modifier = Modifier.fillMaxSize()) {
                    AdaptiveNavigationRail(navController = navController)
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination!!,
                        modifier = Modifier
                            .weight(1f)
                            .padding(innerPadding),
                        windowSizeClass = windowSizeClass
                    )
                }
            } else {
                // Layout tradicional para teléfonos
                AppNavigation(
                    navController = navController,
                    startDestination = startDestination!!,
                    modifier = Modifier.padding(innerPadding),
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}
