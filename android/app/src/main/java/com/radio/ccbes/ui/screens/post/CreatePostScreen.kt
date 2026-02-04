package com.radio.ccbes.ui.screens.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.radio.ccbes.data.cache.AppDatabase
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.repository.ImageUploadRepository
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.ui.theme.RedAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(navController: NavController, postId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postRepository =
        remember { PostRepository(postDao = AppDatabase.getDatabase(context).postDao()) }
    val imageUploadRepository = remember { ImageUploadRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser

    var content by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingImageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingPost by remember { mutableStateOf(postId != null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isEditing = postId != null

    // Fetch existing post if editing
    LaunchedEffect(postId) {
        if (postId != null) {
            try {
                val post = postRepository.getPostById(postId)
                if (post != null) {
                    if (post.userId != currentUser?.uid) {
                        snackbarHostState.showSnackbar("No tienes permiso para editar esta publicación")
                        navController.popBackStack()
                        return@LaunchedEffect
                    }
                    content = post.content
                    existingImageUrls = post.images.ifEmpty { listOfNotNull(post.imageUrl) }
                } else {
                    snackbarHostState.showSnackbar("No se encontró la publicación")
                    navController.popBackStack()
                }

            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error al cargar el post: ${e.message}")
            } finally {
                isLoadingPost = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImageUris = (selectedImageUris + uris).take(5)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Editar Publicación" else "Nueva Publicación",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    TextButton(
                        onClick = {
                            if ((content.isNotBlank() || selectedImageUris.isNotEmpty() || existingImageUrls.isNotEmpty()) && !isLoading) {
                                isLoading = true
                                scope.launch {
                                    try {
                                        val finalImageUrls = mutableListOf<String>()
                                        finalImageUrls.addAll(existingImageUrls)

                                        // 1. Upload new images
                                        selectedImageUris.forEach { uri ->
                                            val uploadResult =
                                                imageUploadRepository.uploadImage(context, uri)
                                            if (uploadResult.isSuccess) {
                                                uploadResult.getOrNull()
                                                    ?.let { finalImageUrls.add(it) }
                                            } else {
                                                throw Exception("Error al subir una imagen")
                                            }
                                        }

                                        if (postId != null) {
                                            // 3. Update existing post
                                            val result = postRepository.updatePost(
                                                postId,
                                                currentUser?.uid ?: "",
                                                content,
                                                finalImageUrls.firstOrNull(),
                                                finalImageUrls
                                            )

                                            if (result.isSuccess) {
                                                navController.popBackStack()
                                            } else {
                                                throw Exception(
                                                    result.exceptionOrNull()?.message
                                                        ?: "Error al actualizar"
                                                )
                                            }
                                        } else {
                                            // 3. Create new post
                                            val newPost = Post(
                                                userId = currentUser?.uid ?: "anonymous",
                                                userName = currentUser?.displayName ?: "Usuario",
                                                userHandle = "@${
                                                    currentUser?.displayName?.replace(
                                                        " ",
                                                        ""
                                                    )?.lowercase() ?: "usuario"
                                                }",
                                                userPhotoUrl = currentUser?.photoUrl?.toString(),
                                                content = content,
                                                imageUrl = finalImageUrls.firstOrNull(),
                                                images = finalImageUrls,
                                                _category = "all"
                                            )

                                            val result = postRepository.createPost(newPost)
                                            if (result.isSuccess) {
                                                navController.popBackStack()
                                            } else {
                                                throw Exception(
                                                    result.exceptionOrNull()?.message
                                                        ?: "Error al publicar"
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = RedAccent
                            )

                        } else {
                            Text(
                                if (isEditing) "Actualizar" else "Publicar",
                                color = RedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )

            if (isLoadingPost) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RedAccent)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Content Input Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        TextField(
                            value = content,
                            onValueChange = { content = it },
                            placeholder = {
                                Text(
                                    "¿Qué está pasando?",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                            visualTransformation = com.radio.ccbes.ui.components.HashtagVisualTransformation(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = RedAccent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media Preview
                    val allPreviewImages = selectedImageUris + existingImageUrls

                    if (allPreviewImages.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allPreviewImages.size) { index ->
                                val item = allPreviewImages[index]
                                Box(modifier = Modifier
                                    .width(150.dp)
                                    .fillMaxHeight()) {
                                    AsyncImage(
                                        model = item,
                                        contentDescription = "Preview",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            if (item is Uri) {
                                                selectedImageUris =
                                                    selectedImageUris.filter { it != item }
                                            } else {
                                                existingImageUrls =
                                                    existingImageUrls.filter { it != item }
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                    ) {
                                        Surface(
                                            color = Color.White.copy(alpha = 0.7f),
                                            shape = CircleShape
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Quitar",
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                if (allPreviewImages.size < 5) {
                                    OutlinedButton(
                                        onClick = { imagePicker.launch("image/*") },
                                        modifier = Modifier
                                            .width(100.dp)
                                            .fillMaxHeight(),
                                        border = BorderStroke(1.dp, RedAccent),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            tint = RedAccent
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, RedAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAccent)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar Imágenes", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
