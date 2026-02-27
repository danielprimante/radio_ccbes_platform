package com.radio.ccbes.ui.screens.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.google.firebase.Timestamp
import com.radio.ccbes.R
import com.radio.ccbes.data.model.PostCategory
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.screens.home.PostCard
import com.radio.ccbes.ui.screens.search.SearchViewModel
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.ui.components.LikesBottomSheet
import com.radio.ccbes.ui.components.CommentsBottomSheet
import com.radio.ccbes.ui.components.PostPlaceholder
import com.radio.ccbes.data.repository.LikeRepository
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.model.Report
import com.radio.ccbes.data.repository.ReportRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.radio.ccbes.ui.components.NetworkImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    viewModel: SearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()

    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val userResults by viewModel.userResults.collectAsState()
    val followStatus by viewModel.followStatus.collectAsState()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val likeRepository = remember { LikeRepository() }
    val reportRepository = remember { ReportRepository() }
    val scope = rememberCoroutineScope()

    // Likes Modal State
    var showLikesModal by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf("") }

    // Comments Modal State
    var showCommentsModal by remember { mutableStateOf(false) }
    var selectedPostIdForComments by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onSearch = {},
            active = false,
            onActiveChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            windowInsets = WindowInsets(0, 0, 0, 0),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_hint),
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = Color.Gray
                        )
                    }
                }
            }
        ) {}

        // Pull to Refresh State
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        val pullRefreshState = rememberPullToRefreshState()
        LaunchedEffect(isRefreshing) {
            if (isRefreshing) pullRefreshState.animateToHidden()
        }

        // Results
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Content
            Column(modifier = Modifier.fillMaxSize()) {
                if (isSearching && searchResults.isEmpty() && userResults.isEmpty()) {
                    repeat(5) {
                        PostPlaceholder()
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }
                } else {
                        // Obtener todos los likes del usuario de una vez (optimización)
                        var userLikedPosts by remember { mutableStateOf<Set<String>>(emptySet()) }
                        
                        LaunchedEffect(currentUser) {
                            currentUser?.let { user ->
                                userLikedPosts = likeRepository.getAllUserLikes(user.uid)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(searchResults) { post ->
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
                                        viewModel.deletePost(post.id)
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
                            }

                            if (userResults.isNotEmpty()) {
                                item {
                                    Text(
                                        stringResource(R.string.users_section),
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                items(userResults) { user ->
                                    val isFollowing = followStatus[user.id] ?: false
                                    UserResultCard(
                                        user = user,
                                        isFollowing = isFollowing,
                                        onToggleFollow = { viewModel.toggleFollow(user.id) },
                                        onProfileClick = {
                                            navController.navigate(Screen.Profile.createRoute(user.id))
                                        }
                                    )
                                }
                            }

                            // Empty state
                            if (searchResults.isEmpty() && userResults.isEmpty() && !isRefreshing) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            if (searchQuery.isEmpty()) stringResource(R.string.search_hint) else stringResource(
                                                R.string.no_results
                                            ),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                stringResource(R.string.try_other_keywords),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        // Likes Modal
        if (showLikesModal) {
            LikesBottomSheet(
                postId = selectedPostId,
                onDismiss = { showLikesModal = false },
                onUserClick = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }

        // Comments Modal
        if (showCommentsModal) {
            CommentsBottomSheet(
                postId = selectedPostIdForComments,
                onDismiss = { showCommentsModal = false }
            )
        }
    }

@Composable
private fun UserResultCard(
    user: User,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onProfileClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (!user.photoUrl.isNullOrEmpty()) {
                NetworkImage(
                    url = user.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                } else {
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.padding(8.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val displayHandle =
                    if (user.handle.startsWith("@")) user.handle else "@${user.handle}"
                Text(
                    displayHandle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = onToggleFollow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else RedAccent
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    if (isFollowing) stringResource(R.string.following_status) else stringResource(
                        R.string.follow_status
                    ),
                    color = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}
