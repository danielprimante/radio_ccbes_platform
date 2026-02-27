package com.radio.ccbes.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.radio.ccbes.R
import com.radio.ccbes.ui.components.ReportDialog
import com.radio.ccbes.ui.components.highlightPostContent
import com.radio.ccbes.ui.components.LinkPreviewCard
import com.radio.ccbes.ui.components.NetworkImage
import com.radio.ccbes.util.LinkParser
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.model.Chat
import com.radio.ccbes.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import com.radio.ccbes.ui.theme.RedAccent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    userName: String,
    userHandle: String,
    timeAgo: String,
    content: String,
    userPhotoUrl: String? = null,
    imageUrl: String? = null,
    likes: Int = 0,
    comments: Int = 0,
    isLiked: Boolean = false,
    postId: String = "",
    postUserId: String = "",
    images: List<String> = emptyList(),
    currentUserId: String? = null,
    onLike: () -> Unit = {},
    onComment: () -> Unit = {},
    onShare: () -> Unit = {},
    onReport: (String) -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onProfileImageClick: (String) -> Unit = {},
    onLikesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var liked by remember { mutableStateOf(isLiked) }
    var showMenu by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Sincronizar el estado local con el parámetro isLiked cuando cambie
    LaunchedEffect(isLiked) {
        liked = isLiked
    }

    val isOwner = currentUserId == postUserId

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                onReport(reason)
                showReportDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_post_title)) },
            text = { Text(stringResource(R.string.delete_post_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedAccent)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Reducido de 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { // Reducido de 16.dp
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // User Avatar
                if (!userPhotoUrl.isNullOrBlank()) {
                    NetworkImage(
                        url = userPhotoUrl,
                        contentDescription = stringResource(R.string.profile_photo_desc),
                        modifier = Modifier
                            .size(36.dp) // Reducido de 40.dp
                            .clip(CircleShape)
                            .clickable { onProfileImageClick(postUserId) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onProfileImageClick(postUserId) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProfileClick(postUserId) }
                ) {
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp // Ajustado levemente
                    )
                    val displayHandle = if (userHandle.startsWith("@")) userHandle else "@$userHandle"
                    Text(
                        text = "$displayHandle • $timeAgo",
                        color = Color.Gray,
                        fontSize = 11.sp // Reducido de 12.sp
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp) // Más compacto
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.options),
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit)) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete), color = RedAccent) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.report)) },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Reducido de 12.dp

            // Caption
            if (content.isNotEmpty()) {
                val annotatedString = content.highlightPostContent()
                val firstUrl = remember(content) { LinkParser.extractFirstUrl(content) }

                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        fontSize = 14.sp
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                    }
                )

                if (firstUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinkPreviewCard(url = firstUrl)
                }

                Spacer(modifier = Modifier.height(8.dp)) // Reducido de 12.dp
            }

            // Carousel or Single Image
            val allImages = if (images.isNotEmpty()) images else listOfNotNull(imageUrl)

            if (allImages.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { allImages.size })

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { index ->
                        val url = allImages[index]
                        NetworkImage(
                            url = url,
                            contentDescription = "Post image $index",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(context, com.radio.ccbes.ui.screens.post.FullScreenImageActivity::class.java).apply {
                                        putExtra("IMAGE_URL", url)
                                    }
                                    context.startActivity(intent)
                                },
                            contentScale = ContentScale.FillWidth // Vuelve a adaptarse al ancho sin recortar
                        )
                    }

                    // Page Indicator (e.g., 1/3) elegante
                    if (allImages.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${allImages.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconToggleButton(
                        checked = liked,
                        onCheckedChange = {
                            liked = it
                            onLike()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (liked) RedAccent else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "$likes",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            if (likes > 0) onLikesClick()
                        }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(onClick = onComment, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(text = "$comments", color = Color.Gray, fontSize = 12.sp)
                }

                IconButton(
                    onClick = { showShareSheet = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.share_post),
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (showShareSheet) {
            val sendPostToChat: (String) -> Unit = { chatId ->
                val senderId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (senderId.isNotEmpty()) {
                    coroutineScope.launch(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Enviando...", android.widget.Toast.LENGTH_SHORT).show()

                        val shareData = com.radio.ccbes.util.ShareUtils.PostShareData(
                            userName = userName,
                            userHandle = if (userHandle.startsWith("@")) userHandle else "@$userHandle",
                            content = content,
                            userPhotoUrl = userPhotoUrl,
                            imageUrl = if (images.isNotEmpty()) images.first() else imageUrl,
                            postId = postId
                        )

                        val uri = com.radio.ccbes.util.ShareUtils.generateShareImage(context, shareData)
                        var sent = false

                            if (uri != null) {
                                val uploadResult = com.radio.ccbes.data.repository.ImageUploadRepository().uploadImage(context, uri)
                                val imageData = uploadResult.getOrNull()

                                if (imageData != null) {
                                    try {
                                        ChatRepository().sendMessage(
                                            chatId = chatId,
                                            senderId = senderId,
                                            content = imageData.url,
                                            type = "image",
                                            postId = postId,
                                            deleteUrl = imageData.deleteUrl
                                        )
                                    sent = true
                                    android.widget.Toast.makeText(context, "Publicación enviada", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        if (!sent) {
                            try {
                                val messageContent = "He compartido una publicación de @$userHandle en Radio CCBES"
                                ChatRepository().sendMessage(
                                    chatId = chatId,
                                    senderId = senderId,
                                    content = messageContent
                                )
                                android.widget.Toast.makeText(context, "Publicación compartida", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Error al compartir", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            com.radio.ccbes.ui.components.ShareToChatBottomSheet(
                onDismiss = { showShareSheet = false },
                onChatSelected = { chat ->
                    showShareSheet = false
                    sendPostToChat(chat.id)
                },
                onUserSelected = { user ->
                    showShareSheet = false
                    coroutineScope.launch {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        if (currentUserId.isNotEmpty()) {
                            val chatId = ChatRepository().getOrCreateChat(currentUserId, user.id)
                            sendPostToChat(chatId)
                        }
                    }
                }
            )
        }
    }
}
