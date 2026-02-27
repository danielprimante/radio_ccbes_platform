package com.radio.ccbes.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.auth.AuthManager
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.ui.navigation.Screen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    viewModel: AccountSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isOperating by remember { mutableStateOf(false) }
    
    val cacheSize by viewModel.cacheSize.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCacheSize(context)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isOperating) showDeleteDialog = false },
            title = { Text("¿Eliminar cuenta?", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Esta acción es irreversible. Se eliminará todo rastro del usuario, likes y tus publicaciones de la plataforma.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isOperating) {
                            isOperating = true
                            scope.launch {
                                try {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        // 1. Delete data from Firestore
                                        userRepository.deleteUserCompletely(user.uid)
                                        
                                        // 2. Delete from Firebase Auth
                                        user.delete().await()
                                        
                                        // 3. Clear Google Session
                                        AuthManager.signOut(context)
                                        navController.navigate(Screen.Welcome.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Error handling could be improved with a snackbar
                                    isOperating = false
                                    showDeleteDialog = false
                                }
                            }
                        }
                    },
                    enabled = !isOperating
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = RedAccent)
                    } else {
                        Text("Eliminar", color = RedAccent, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isOperating) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuenta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Management Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "GESTIÓN DE ALMACENAMIENTO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Memoria en caché",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Espacio consumido: $cacheSize",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.clearCache(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Borrar caché", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SESIÓN Y SEGURIDAD",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Button(
                onClick = {
                    scope.launch {
                        AuthManager.signOut(context)
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cerrar Sesión", modifier = Modifier.padding(vertical = 8.dp))
            }

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent.copy(alpha = 0.1f), contentColor = RedAccent),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Eliminar Cuenta", modifier = Modifier.padding(vertical = 8.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                "Al eliminar tu cuenta, se perderá permanentemente el acceso a tu perfil y contenido.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }
    }
}

