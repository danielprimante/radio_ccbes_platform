package com.radio.ccbes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handle permission result if needed
    }

    private var initialPostId by mutableStateOf<String?>(null)
    private var initialChatId by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle notification data if app was opened from a notification
        handleIntent(intent)

        // Request notifications permission for Android 13+
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
                    MainScreen(
                        initialPostId = initialPostId,
                        initialChatId = initialChatId,
                        windowSizeClass = windowSizeClass
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Handle Notification Extras
        intent?.extras?.let { bundle ->
            val postId = bundle.getString("postId")
            if (postId != null) {
                initialPostId = postId
            }
            val chatId = bundle.getString("chatId")
            if (chatId != null) {
                initialChatId = chatId
            }
        }

        // Handle Deep Links (URL)
        intent?.data?.let { uri ->
            val pathSegments = uri.pathSegments
            if (pathSegments.size >= 2) {
                // https://www.ccbes.com.ar/post/{postId}
                if (pathSegments[0] == "post") {
                    initialPostId = pathSegments[1]
                }
                // https://www.ccbes.com.ar/profile/{userId}
                // Note: MainScreen might need update to handle initialUserId if we want to support this too,
                // but setting initialPostId is the priority request.
            }
        }
    }
}
