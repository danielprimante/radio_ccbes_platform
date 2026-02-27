package com.radio.ccbes.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.Comment
import com.radio.ccbes.data.repository.CommentRepository
import com.radio.ccbes.data.repository.ImageUploadRepository
import com.radio.ccbes.ui.screens.comments.CommentItem
import com.radio.ccbes.ui.theme.RedAccent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()
    val repository = remember { CommentRepository() }
    val uploadRepository = remember { ImageUploadRepository() }
    val comments by repository.getCommentsForPost(postId).collectAsState(initial = emptyList())
    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    var commentText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var editingCommentId by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray)
                        .align(Alignment.CenterHorizontally)
                )
                
                // Header
                Text(
                    text = "Comentarios",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Comments List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay comentarios aún. ¡Sé el primero!",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        val isLiked by repository.isCommentLikedByUser(
                            comment.id,
                            currentUser?.uid ?: ""
                        ).collectAsState(initial = false)
                        
                        CommentItem(
                            modifier = Modifier.animateItem(),
                            comment = comment,
                            currentUserId = currentUser?.uid,
                            isLiked = isLiked,
                            onLikeClick = {
                                if (currentUser != null) {
                                    scope.launch {
                                        repository.toggleCommentLike(comment.id, currentUser.uid, postId)
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
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))

            // Input Section
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.imePadding()
            ) {
                Column {
                    // Image Preview
                    AnimatedVisibility(
                        visible = selectedImageUri != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(80.dp)
                            ) {
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
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, start = 8.dp, end = 8.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                if (editingCommentId == null) {
                                    IconButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = "Add Image",
                                            tint = Color.Gray
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            editingCommentId = null
                                            commentText = ""
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cancel Edit",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                                
                                TextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = {
                                        Text(
                                            if (editingCommentId != null) "Editando..." else "Escribe un comentario...",
                                            color = Color.Gray,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // Send Button with Animation
                        AnimatedVisibility(
                            visible = commentText.isNotBlank() && !isSending,
                            enter = fadeIn(animationSpec = tween(200)) + 
                                    scaleIn(initialScale = 0.8f),
                            exit = fadeOut(animationSpec = tween(150)) + 
                                   scaleOut(targetScale = 0.8f)
                        ) {
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
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(RedAccent.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = RedAccent
                                )
                            }
                        }
                        
                        // Loading Indicator
                        if (isSending) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = RedAccent
                            )
                        }
                    }
                }
            }
        }
        
        SnackbarHost(snackbarHostState)
    }
}
