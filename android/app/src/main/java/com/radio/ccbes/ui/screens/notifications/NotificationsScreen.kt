package com.radio.ccbes.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.radio.ccbes.data.model.Notification
import com.radio.ccbes.data.model.NotificationType
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: NotificationsViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            title = { Text("Notificaciones", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.deleteAll() }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar todo",
                        tint = RedAccent
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedAccent)
            }
        } else if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tienes notificaciones aún", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            viewModel.markAsRead(notification.id)
                            notification.postId?.let { pid ->
                                navController.navigate(Screen.PostDetail.createRoute(pid))
                            }
                            notification.chatId?.let { cid ->
                                navController.navigate(Screen.Chat.createRoute(cid))
                            }
                            if (notification.type == NotificationType.FOLLOW.value) {
                                navController.navigate(Screen.Profile.createRoute(notification.fromUserId))
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    val isRead = notification.isRead
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isRead) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Profile Pic
        Box(modifier = Modifier.size(48.dp)) {
            if (notification.fromUserProfilePic.isNotEmpty()) {
                AsyncImage(
                    model = notification.fromUserProfilePic,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = Color.Gray)
                }
            }
            
            // Notification Type Icon Overlay
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            NotificationType.LIKE.value -> Color(0xFFE91E63)
                            NotificationType.COMMENT.value -> Color(0xFF2196F3)
                            NotificationType.MESSAGE.value -> Color(0xFF4CAF50)
                            NotificationType.FOLLOW.value -> Color(0xFF9C27B0) // Purple for follow
                            else -> Color.Gray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.LIKE.value -> Icons.Default.Favorite
                        NotificationType.COMMENT.value -> Icons.AutoMirrored.Filled.Comment
                        NotificationType.MESSAGE.value -> Icons.AutoMirrored.Filled.Chat
                        NotificationType.FOLLOW.value -> Icons.Default.Person
                        else -> Icons.Default.Person
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(notification.fromUserName)
                }
                append(" ")
                append(
                    when (notification.type) {
                        NotificationType.LIKE.value -> "le dio me gusta a tu publicación"
                        NotificationType.COMMENT.value -> "comentó tu publicación"
                        NotificationType.MESSAGE.value -> "te envió un mensaje"
                        NotificationType.FOLLOW.value -> "comenzó a seguirte"
                        else -> "interactuó contigo"
                    }
                )
                if (!notification.postContent.isNullOrBlank()) {
                    append(": \"")
                    withStyle(style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(notification.postContent)
                    }
                    append("\"")
                }
            }
            
            Text(
                text = annotatedText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = try { TimeUtils.formatTimeAgo(notification.timestamp) } catch (_: Exception) { "Recientemente" },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (!isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RedAccent)
            )
        }
    }
}
