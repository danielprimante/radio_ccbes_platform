package com.radio.ccbes.ui.screens.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.repository.LikeRepository
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.screens.home.PostCard
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.TimeUtils
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    navController: NavController,
    postId: String
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val postRepository = remember { PostRepository(AppDatabase.getDatabase(context).postDao(), userRepository) }
    val likeRepository = remember { LikeRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    
    var post by remember { mutableStateOf<Post?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isLikedByCurrentUser by remember { mutableStateOf(false) }
    var showLikesModal by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf("") }
    
    LaunchedEffect(postId, currentUser?.uid) {
        isLoading = true
        val fetchedPost = postRepository.getPostById(postId)
        post = fetchedPost
        if (fetchedPost != null && currentUser != null) {
            isLikedByCurrentUser = likeRepository.hasUserLikedPost(fetchedPost.id, currentUser.uid)
        }
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = RedAccent)
                    }
                }
                post == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Publicación no encontrada",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { navController.navigateUp() }) {
                                Text("Volver")
                            }
                        }
                    }
                }
                else -> {
                    val currentPost = post!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        PostCard(
                            userName = currentPost.userName,
                            userHandle = currentPost.userHandle,
                            userPhotoUrl = currentPost.userPhotoUrl,
                            timeAgo = TimeUtils.formatTimeAgo(currentPost.timestamp),
                            content = currentPost.content,
                            imageUrl = currentPost.imageUrl,
                            likes = currentPost.likes,
                            comments = currentPost.comments,
                            postId = currentPost.id,
                            postUserId = currentPost.userId,
                            images = currentPost.images,
                            currentUserId = currentUser?.uid,
                            isLiked = isLikedByCurrentUser,
                            onLike = {
                                currentUser?.let { user ->
                                    val originalLikes = currentPost.likes
                                    val originalIsLiked = isLikedByCurrentUser

                                    // Actualización optimista: actualizar UI inmediatamente
                                    isLikedByCurrentUser = !originalIsLiked
                                    post = currentPost.copy(
                                        likes = if (isLikedByCurrentUser) originalLikes + 1 else (originalLikes - 1).coerceAtLeast(0)
                                    )

                                    // Llamada al backend en segundo plano
                                    scope.launch {
                                        val result = likeRepository.toggleLike(currentPost.id, user.uid)
                                        if (result.isFailure) {
                                            // Revertir si falla
                                            isLikedByCurrentUser = originalIsLiked
                                            post = currentPost.copy(likes = originalLikes)
                                        }
                                    }
                                }
                            },
                            onComment = {
                                navController.navigate(Screen.Comments.createRoute(currentPost.id))
                            },
                            onShare = { },
                            onReport = {
                                // Implementar reporte si es necesario
                            },
                            onEdit = {
                                navController.navigate(Screen.EditPost.createRoute(currentPost.id))
                            },
                            onDelete = {
                                scope.launch {
                                    val result = postRepository.deletePost(currentPost.id, currentUser?.uid ?: "")
                                    if (result.isSuccess) {
                                        navController.navigateUp()
                                    }
                                }
                            },
                            onProfileClick = { userId ->
                                navController.navigate(Screen.Profile.createRoute(userId))
                            },
                            onProfileImageClick = { userId ->
                                navController.navigate(Screen.Profile.createRoute(userId))
                            },
                            onLikesClick = {
                                selectedPostId = currentPost.id
                                showLikesModal = true
                            }
                        )
                    }
                }
            }
        }

        if (showLikesModal) {
            com.radio.ccbes.ui.components.LikesBottomSheet(
                postId = selectedPostId,
                onDismiss = { showLikesModal = false },
                onUserClick = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }
    }
}
