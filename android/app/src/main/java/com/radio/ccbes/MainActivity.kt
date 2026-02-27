package com.radio.ccbes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.radio.ccbes.ui.screens.main.MainScreen
import com.radio.ccbes.ui.theme.RadioCCBESTheme
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handle permission result if needed
    }

    private var initialPostId by mutableStateOf<String?>(null)
    private var initialChatId by mutableStateOf<String?>(null)
    private var initialUserId by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Habilitar el diseño de borde a borde (Edge-to-Edge) de forma moderna
        enableEdgeToEdge()

        // Manejar datos de notificación si la app se abrió desde una notificación
        handleIntent(intent)

        // Solicitar permiso de notificaciones para Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            RadioCCBESTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Inicializar la pantalla principal con los IDs obtenidos del intent (si existen)
                    MainScreen(
                        initialPostId = initialPostId,
                        initialChatId = initialChatId,
                        initialUserId = initialUserId,
                        windowSizeClass = windowSizeClass
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Manejar el nuevo intent cuando la actividad ya está en ejecución
        handleIntent(intent)
    }

    /**
     * Procesa el intent recibido para extraer parámetros de notificaciones o Deep Links.
     */
    private fun handleIntent(intent: Intent?) {
        // Manejar Extras de Notificación (OneSignal / FCM)
        intent?.extras?.let { bundle ->
            val postId = bundle.getString("postId")
            if (postId != null) {
                initialPostId = postId
            }
            val chatId = bundle.getString("chatId")
            if (chatId != null) {
                initialChatId = chatId
            }
            val userId = bundle.getString("userId")
            if (userId != null) {
                initialUserId = userId
            }
        }

        // Manejar Deep Links (URLs personalizadas)
        intent?.data?.let { uri ->
            val pathSegments = uri.pathSegments
            if (pathSegments.size >= 2) {
                // Formato esperado: https://www.ccbes.com.ar/post/{postId}
                if (pathSegments[0] == "post") {
                    initialPostId = pathSegments[1]
                } else if (pathSegments[0] == "profile") {
                    initialUserId = pathSegments[1]
                }
            }
        }
    }
}
