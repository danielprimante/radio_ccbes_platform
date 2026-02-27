package com.radio.ccbes.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.R
import com.radio.ccbes.data.model.Report
import com.radio.ccbes.data.repository.LikeRepository
import com.radio.ccbes.data.repository.NotificationRepository
import com.radio.ccbes.data.repository.ReportRepository
import com.radio.ccbes.ui.components.CommentsBottomSheet
import com.radio.ccbes.ui.components.LikesBottomSheet
import com.radio.ccbes.ui.components.PostPlaceholder
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: HomeViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val likeRepository = remember { LikeRepository() }
    val reportRepository = remember { ReportRepository() }
    val notificationRepository = remember { NotificationRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    var hasUnreadNotifications by remember { mutableStateOf(false) }
    var showLikesModal by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf("") }
    var showCommentsModal by remember { mutableStateOf(false) }
    var selectedPostIdForComments by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            notificationRepository.getNotificationsForUser(user.uid).collect { notifications ->
                hasUnreadNotifications = notifications.any { !it.isRead }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.radio_ccbes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                    BadgedBox(
                        badge = {
                            if (hasUnreadNotifications) {
                                Badge(containerColor = RedAccent)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (hasUnreadNotifications) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = { navController.navigate(Screen.ChatList.route) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = stringResource(R.string.nav_chat),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { navController.navigate(Screen.CreatePost.route) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.nav_post),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0) // Evitar que la TopAppBar añada más padding
        )

        // Obtener todos los likes del usuario de una vez (optimización)
        var userLikedPosts by remember { mutableStateOf<Set<String>>(emptySet()) }
        
        LaunchedEffect(currentUser) {
            currentUser?.let { user ->
                userLikedPosts = likeRepository.getAllUserLikes(user.uid)
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(posts, key = { it.id }) { post ->
                    val isLiked = userLikedPosts.contains(post.id)
                    
                    PostCard(
                        userName = post.userName,
                        userHandle = post.userHandle,
                        userPhotoUrl = post.userPhotoUrl,
                        timeAgo = TimeUtils.formatTimeAgo(post.timestamp),
                        content = post.content,
                        imageUrl = post.imageUrl,
                        likes = post.likes,
                        comments = post.comments,
                        isLiked = isLiked,
                        postId = post.id,
                        postUserId = post.userId,
                        images = post.images,
                        currentUserId = currentUser?.uid,
                        onLike = {
                            currentUser?.let { user ->
                                // Actualización optimista: actualizar UI primero
                                val wasLiked = userLikedPosts.contains(post.id)
                                userLikedPosts = if (wasLiked) {
                                    userLikedPosts - post.id
                                } else {
                                    userLikedPosts + post.id
                                }
                                
                                // Luego hacer la llamada al backend en segundo plano
                                scope.launch {
                                    val result = likeRepository.toggleLike(post.id, user.uid)
                                    // Si falla, revertir el cambio
                                    if (result.isFailure) {
                                        userLikedPosts = if (wasLiked) {
                                            userLikedPosts + post.id
                                        } else {
                                            userLikedPosts - post.id
                                        }
                                    }
                                }
                            }
                        },
                        onComment = {
                            selectedPostIdForComments = post.id
                            showCommentsModal = true
                        },
                        onShare = { },
                        onReport = { reason ->
                            currentUser?.let { user ->
                                scope.launch {
                                    val report = Report(
                                        postId = post.id,
                                        reportedBy = user.uid,
                                        reason = reason
                                    )
                                    reportRepository.submitReport(report)
                                }
                            }
                        },
                        onEdit = {
                            navController.navigate(Screen.EditPost.createRoute(post.id))
                        },
                        onDelete = {
                            currentUser?.let { user ->
                                scope.launch {
                                    viewModel.deletePostSuspend(post.id)
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
                            selectedPostId = post.id
                            showLikesModal = true
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }

                if (isLoading) {
                    items(5) {
                        PostPlaceholder()
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                } else if (posts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.no_posts),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.posts_will_appear),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }

    // Modals
    if (showLikesModal) {
        LikesBottomSheet(
            postId = selectedPostId,
            onDismiss = { showLikesModal = false },
            onUserClick = { userId ->
                navController.navigate(Screen.Profile.createRoute(userId))
            }
        )
    }

    if (showCommentsModal) {
        CommentsBottomSheet(
            postId = selectedPostIdForComments,
            onDismiss = { showCommentsModal = false }
        )
    }
}
