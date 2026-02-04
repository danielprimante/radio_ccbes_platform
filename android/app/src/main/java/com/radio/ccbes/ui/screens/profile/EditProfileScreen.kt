package com.radio.ccbes.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.repository.ImageUploadRepository
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.ui.theme.RedAccent
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }
    val uploadRepository = remember { ImageUploadRepository() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var userProfile by remember { mutableStateOf<User?>(null) }
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    
    var isHandleAvailable by remember { mutableStateOf(true) }
    var isCheckingHandle by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploading = true
                val result = uploadRepository.uploadImage(context, it)
                result.onSuccess { newUrl ->
                    photoUrl = newUrl
                    auth.currentUser?.let { user ->
                        userRepository.updatePhotoUrl(user.uid, newUrl)
                    }
                }.onFailure {
                    // Handle error (show snackbar/toast)
                }
                isUploading = false
            }
        }
    }

    // Handle Debounce Check
    LaunchedEffect(handle) {
        if (handle.isBlank() || handle == (userProfile?.handle ?: "")) {
            isHandleAvailable = true
            return@LaunchedEffect
        }
        
        isCheckingHandle = true
        delay(600) // Debounce delay
        auth.currentUser?.let { user ->
            isHandleAvailable = userRepository.isHandleAvailable(handle.trim(), user.uid)
        }
        isCheckingHandle = false
    }

    LaunchedEffect(Unit) {
        auth.currentUser?.let { user ->
            val profile = userRepository.getUser(user.uid)
            userProfile = profile
            profile?.let {
                name = it.name
                handle = it.handle
                city = it.city
                phone = it.phone
                email = it.email.ifBlank { user.email ?: "" }
                bio = it.bio
                photoUrl = it.photoUrl
            }
        }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Editar perfil", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), color = RedAccent, strokeWidth = 2.dp)
                    } else {
                        TextButton(
                            enabled = isHandleAvailable && !isCheckingHandle,
                            onClick = {
                            scope.launch {
                                isSaving = true
                                auth.currentUser?.let { user ->
                                    userRepository.updateFullProfile(
                                        userId = user.uid,
                                        name = name,
                                        handle = handle,
                                        city = city,
                                        phone = phone,
                                        email = email,
                                        bio = bio
                                    )
                                }
                                isSaving = false
                                navController.popBackStack()
                            }
                        }) {
                            Text("Guardar", color = RedAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Profile Image
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(90.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (photoUrl != null) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                        }
                        
                        if (isUploading) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Editar foto o avatar",
                    color = Color(0xFF3897F0), 
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { 
                        if (!isUploading) photoPickerLauncher.launch("image/*") 
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fields - All using the requested EditField style
                EditField(label = "Nombre", value = name, onValueChange = { name = it })
                
                EditField(
                    label = "Nombre de usuario", 
                    value = handle, 
                    onValueChange = { handle = it },
                    isError = !isHandleAvailable,
                    errorMessage = if (!isHandleAvailable) "Este nombre de usuario ya está en uso" else null,
                    isLoading = isCheckingHandle
                )

                EditField(
                    label = "Descripción", 
                    value = bio, 
                    onValueChange = { bio = it },
                    singleLine = false
                )

                EditField(label = "Ciudad", value = city, onValueChange = { city = it })
                EditField(label = "Teléfono", value = phone, onValueChange = { phone = it })
                EditField(label = "Email", value = email, onValueChange = { email = it })

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun EditField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    isLoading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .then(if (isError) Modifier.background(Color.White.copy(0f)) else Modifier) // Dummy to force refresh if needed
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (isError) RedAccent else Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color.Gray)
                }
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp, 
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            )
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = RedAccent,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
