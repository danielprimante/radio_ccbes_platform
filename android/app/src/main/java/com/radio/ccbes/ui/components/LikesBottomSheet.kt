package com.radio.ccbes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.repository.LikeRepository
import com.radio.ccbes.data.repository.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesBottomSheet(
    postId: String,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val likeRepository = remember { LikeRepository() }
    val userRepository = remember { UserRepository() }
    
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    
    LaunchedEffect(postId) {
        isLoading = true
        android.util.Log.d("LikesBottomSheet", "Iniciando carga de likes para postId: $postId")
        try {
            val userIds = likeRepository.getUsersWhoLiked(postId)
            android.util.Log.d("LikesBottomSheet", "UserIds obtenidos: ${userIds.size} usuarios - $userIds")
            
            if (userIds.isNotEmpty()) {
                val loadedUsers = userRepository.getUsersByIds(userIds)
                android.util.Log.d("LikesBottomSheet", "Total usuarios cargados: ${loadedUsers.size}")
                loadedUsers.forEach { u -> android.util.Log.d("LikesBottomSheet", "Usuario: ${u.name} (id: ${u.id})") }
                users = loadedUsers
            } else {
                android.util.Log.d("LikesBottomSheet", "No se encontraron IDs de usuarios para este post")
                users = emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("LikesBottomSheet", "Error al cargar likes para postId: $postId", e)
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Me gusta",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay likes aún",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(users) { user ->
                        UserLikeItem(
                            user = user,
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                    onUserClick(user.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserLikeItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (!user.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.LightGray.copy(alpha = 0.3f)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val displayHandle = if (user.handle.startsWith("@")) user.handle else "@${user.handle}"
            Text(
                text = displayHandle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}
