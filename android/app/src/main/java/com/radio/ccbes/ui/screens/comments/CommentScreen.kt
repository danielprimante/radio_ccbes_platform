package com.radio.ccbes.ui.screens.comments

import android.net.Uri
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.data.model.Comment
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.model.Report
import com.radio.ccbes.data.repository.CommentRepository
import com.radio.ccbes.data.repository.ImageUploadRepository
import com.radio.ccbes.data.repository.LikeRepository
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.data.repository.ReportRepository
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.screens.home.PostCard
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.TimeUtils
import kotlinx.coroutines.launch

private sealed class ReportTarget {
    data class Post(val id: String) : ReportTarget()
    data class Comment(val id: String) : ReportTarget()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    navController: NavController,
    postId: String
) {
    val repository = remember { CommentRepository() }
    val uploadRepository = remember { ImageUploadRepository() }
    val comments by repository.getCommentsForPost(postId).collectAsState(initial = emptyList())
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val postRepository = remember { PostRepository(AppDatabase.getDatabase(context).postDao(), userRepository) }
    val likeRepository = remember { LikeRepository() }
    val reportRepository = remember { ReportRepository() }

    var post by remember { mutableStateOf<Post?>(null) }
    var isLoadingPost by remember { mutableStateOf(true) }

    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }

    LaunchedEffect(postId) {
        isLoadingPost = true
        post = postRepository.getPostById(postId)
        isLoadingPost = false
    }

    var commentText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var showLikesModal by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    if (reportTarget != null) {
        com.radio.ccbes.ui.components.ReportDialog(
            onDismiss = { reportTarget = null },
            onReport = { reason ->
                val currentTarget = reportTarget
                reportTarget = null
                scope.launch {
                    val result = when (currentTarget) {
                        is ReportTarget.Post -> {
                            reportRepository.submitReport(Report(postId = currentTarget.id, reportedBy = currentUser?.uid ?: "", reason = reason))
                            Result.success(Unit)
                        }
                        is ReportTarget.Comment -> {
                            // Implementar lógica de reporte para comentarios
                            Result.success(Unit)
                        }
                        null -> Result.failure(Exception("No target"))
                    }

                    if (result.isSuccess) {
                        snackbarHostState.showSnackbar("Reporte enviado")
                    }
                }
            }
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Comentarios", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (comments.isEmpty() && !isLoadingPost) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay comentarios aún. ¡Sé el primero!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // Current Post at the top
                        item {
                            if (isLoadingPost) {
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = RedAccent, modifier = Modifier.size(32.dp))
                                }
                            } else if (post != null) {
                                val currentPost = post!!
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
                                    onLike = {
                                        currentUser?.let { user ->
                                            scope.launch {
                                                likeRepository.toggleLike(currentPost.id, user.uid)
                                                // Update local post state
                                                val postDoc = postRepository.getPostById(currentPost.id)
                                                if (postDoc != null) post = postDoc
                                            }
                                        }
                                    },
                                    onComment = {
                                        // Already in comments screen, maybe scroll to bottom?
                                    },
                                    onShare = { },
                                    onReport = {
                                        reportTarget = ReportTarget.Post(currentPost.id)
                                    },
                                    onEdit = {
                                        navController.navigate(Screen.EditPost.createRoute(currentPost.id))
                                    },
                                    onDelete = {
                                        scope.launch {
                                            val result = postRepository.deletePost(currentPost.id, currentUser?.uid ?: "")
                                            if (result.isSuccess) {
                                                navController.popBackStack()
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
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Text(
                                    "Comentarios",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        items(comments, key = { it.id }) { comment ->
                            // Estado optimista local para likes
                            val firestoreLiked by repository.isCommentLikedByUser(comment.id, currentUser?.uid ?: "").collectAsState(initial = null)
                            var localLikedState by remember(comment.id) { mutableStateOf<Boolean?>(null) }
                            
                            // Sincronizar estado local con Firestore
                            LaunchedEffect(firestoreLiked) {
                                if (firestoreLiked != null && localLikedState == null) {
                                    localLikedState = firestoreLiked
                                } else if (firestoreLiked != null && localLikedState != null) {
                                    // Solo actualizar si no hay acción optimista pendiente
                                    localLikedState = firestoreLiked
                                }
                            }
                            
                            val effectiveLiked = localLikedState ?: firestoreLiked ?: false

                            CommentItem(
                                comment = comment,
                                currentUserId = currentUser?.uid,
                                isLiked = effectiveLiked,
                                onLikeClick = {
                                    if (currentUser != null) {
                                        // Actualización optimista inmediata
                                        val newLikedState = !effectiveLiked
                                        localLikedState = newLikedState
                                        
                                        scope.launch {
                                            val result = repository.toggleCommentLike(comment.id, currentUser.uid, postId)
                                            if (result.isFailure) {
                                                // Revertir si falla
                                                localLikedState = !newLikedState
                                                snackbarHostState.showSnackbar("Error al dar like")
                                            }
                                        }
                                    }
                                },
                                onEditClick = {
                                    editingCommentId = comment.id
                                    commentText = comment.content
                                },
                                onDeleteClick = {
                                    scope.launch {
                                        val result = repository.deleteComment(comment.id, postId)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar("Comentario eliminado")
                                        } else {
                                            snackbarHostState.showSnackbar("Error al eliminar")
                                        }
                                    }
                                },
                                onReportClick = {
                                    reportTarget = ReportTarget.Comment(comment.id)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Bar Content
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column {
                    // Image Preview
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier
                            .padding(16.dp)
                            .size(80.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingCommentId == null) {
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Image, contentDescription = "Add Image", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            IconButton(onClick = {
                                editingCommentId = null
                                commentText = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        TextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text(if (editingCommentId != null) "Editando comentario..." else "Escribe un comentario...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = RedAccent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank() && currentUser != null && !isSending) {
                                    isSending = true
                                    scope.launch {
                                        if (editingCommentId != null) {
                                            val result = repository.updateComment(editingCommentId!!, commentText)
                                            if (result.isSuccess) {
                                                commentText = ""
                                                editingCommentId = null
                                            } else {
                                                snackbarHostState.showSnackbar("Error al editar")
                                            }
                                        } else {
                                            var imageUrl: String? = null
                                            if (selectedImageUri != null) {
                                                val uploadResult = uploadRepository.uploadImage(context, selectedImageUri!!)
                                                if (uploadResult.isSuccess) {
                                                    imageUrl = uploadResult.getOrNull()?.url
                                                } else {
                                                    snackbarHostState.showSnackbar("Error al subir imagen")
                                                    isSending = false
                                                    return@launch
                                                }
                                            }

                                            val newComment = Comment(
                                                postId = postId,
                                                userId = currentUser.uid,
                                                userName = currentUser.displayName ?: "Usuario",
                                                userPhotoUrl = currentUser.photoUrl?.toString(),
                                                content = commentText,
                                                imageUrl = imageUrl
                                            )
                                            val result = repository.addComment(newComment)
                                            if (result.isSuccess) {
                                                commentText = ""
                                                selectedImageUri = null
                                            } else {
                                                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                                                snackbarHostState.showSnackbar("Error: $error")
                                            }
                                        }
                                        isSending = false
                                    }
                                }
                            },
                            enabled = commentText.isNotBlank() && !isSending
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RedAccent)
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = RedAccent
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

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
