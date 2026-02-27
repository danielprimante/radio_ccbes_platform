package com.radio.ccbes.ui.screens.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.R
import com.radio.ccbes.data.repository.LikeRepository
import com.radio.ccbes.data.repository.ReportRepository
import com.radio.ccbes.ui.components.CommentsBottomSheet
import com.radio.ccbes.ui.components.LikesBottomSheet
import com.radio.ccbes.ui.components.PostPlaceholder
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.screens.home.PostCard
import com.radio.ccbes.util.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: NewsViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val likeRepository = remember { LikeRepository() }
    val reportRepository = remember { ReportRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    var showLikesModal by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf("") }
    var showCommentsModal by remember { mutableStateOf(false) }
    var selectedPostIdForComments by remember { mutableStateOf("") }

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
                    text = stringResource(R.string.nav_news),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

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
                items(posts, key = { it.id }) { post ->
                    val isLiked = userLikedPosts.contains(post.id)
                    // Calcular likes localmente para actualizaciones optimistas
                    val currentLikes = if (isLiked) post.likes else post.likes
                    
                    PostCard(
                        userName = post.userName,
                        userHandle = post.userHandle,
                        userPhotoUrl = post.userPhotoUrl,
                        timeAgo = TimeUtils.formatTimeAgo(post.timestamp),
                        content = post.content,
                        imageUrl = post.imageUrl,
                        likes = currentLikes,
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
                            // Implement report logic if needed
                        },
                        onEdit = {
                            navController.navigate(Screen.EditPost.createRoute(post.id))
                        },
                        onDelete = {
                            // Implement delete logic if needed
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
                                "No hay noticias oficiales aún",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

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
