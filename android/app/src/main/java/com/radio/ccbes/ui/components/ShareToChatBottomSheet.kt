package com.radio.ccbes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.Chat
import com.radio.ccbes.data.model.User
import com.radio.ccbes.ui.screens.chat.ChatListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToChatBottomSheet(
    onDismiss: () -> Unit,
    onChatSelected: (Chat) -> Unit,
    onUserSelected: (User) -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Compartir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar") },
                leadingIcon = {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                                contentDescription = "Limpiar"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Gray,
                    unfocusedBorderColor = Color.Gray
                )
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.chats.isEmpty() && uiState.followers.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No tienes a quién enviar", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                ) {
                    // Filtrar chats según búsqueda
                    val filteredChats = if (searchQuery.isNotEmpty()) {
                        uiState.chats.filter { chat ->
                            val otherUserId = chat.participants.find { it != currentUserId } ?: ""
                            val name = chat.participantNames[otherUserId] ?: ""
                            val handle = chat.participantHandles[otherUserId] ?: ""
                            name.contains(searchQuery, ignoreCase = true) ||
                            handle.contains(searchQuery.removePrefix("@"), ignoreCase = true)
                        }
                    } else {
                        uiState.chats
                    }

                    // --- SECCIÓN DE CHATS RECIENTES ---
                    if (filteredChats.isNotEmpty()) {
                        item {
                            Text(
                                "Conversaciones recientes",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(filteredChats) { chat ->
                            val otherUserId = chat.participants.find { it != currentUserId } ?: ""
                            val name = chat.participantNames[otherUserId] ?: "Usuario"
                            val photoUrl = chat.participantPhotos[otherUserId]

                            ShareItemRow(
                                name = name,
                                photoUrl = photoUrl,
                                onClick = { onChatSelected(chat) }
                            )
                        }
                    }

                    // --- SECCIÓN DE SEGUIDORES ---
                    val followersNotInChats = uiState.followers.filter { follower ->
                        uiState.chats.none { chat -> chat.participants.contains(follower.id) }
                    }

                    // Filtrar seguidores según búsqueda
                    val filteredFollowers = if (searchQuery.isNotEmpty()) {
                        followersNotInChats.filter { follower ->
                            follower.name.contains(searchQuery, ignoreCase = true) ||
                            follower.handle.contains(searchQuery.removePrefix("@"), ignoreCase = true)
                        }
                    } else {
                        followersNotInChats
                    }

                    if (filteredFollowers.isNotEmpty()) {
                        item {
                            Text(
                                "Seguidores",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(filteredFollowers) { follower ->
                            ShareItemRow(
                                name = follower.name,
                                photoUrl = follower.photoUrl,
                                onClick = { onUserSelected(follower) }
                            )
                        }
                    }

                    // --- SECCIÓN DE RESULTADOS DE BÚSQUEDA GLOBAL ---
                    if (searchQuery.isNotEmpty() && uiState.searchResults.isNotEmpty()) {
                        // Filtrar usuarios de búsqueda global que no estén en chats ni en seguidores
                        val globalSearchResults = uiState.searchResults.filter { searchUser ->
                            // Excluir usuarios que ya tienen chat
                            val notInChats = uiState.chats.none { chat ->
                                chat.participants.contains(searchUser.id)
                            }
                            // Excluir usuarios que ya son seguidores
                            val notInFollowers = uiState.followers.none { follower ->
                                follower.id == searchUser.id
                            }
                            // Excluir al usuario actual
                            val notCurrentUser = searchUser.id != currentUserId

                            notInChats && notInFollowers && notCurrentUser
                        }

                        if (globalSearchResults.isNotEmpty()) {
                            item {
                                Text(
                                    "Otros usuarios",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            items(globalSearchResults) { user ->
                                ShareItemRow(
                                    name = user.name,
                                    photoUrl = user.photoUrl,
                                    onClick = { onUserSelected(user) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShareItemRow(
    name: String,
    photoUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.LightGray.copy(alpha = 0.2f)
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onClick() },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("Enviar", fontSize = 12.sp)
        }
    }
}
