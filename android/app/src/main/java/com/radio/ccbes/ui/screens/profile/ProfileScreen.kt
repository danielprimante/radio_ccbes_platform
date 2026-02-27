package com.radio.ccbes.ui.screens.profile

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.radio.ccbes.R
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.ui.components.NetworkImage

import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.auth.AuthManager
import com.radio.ccbes.data.model.Report
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.repository.FollowRepository
import com.radio.ccbes.data.repository.LikeRepository
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.data.repository.ReportRepository
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.data.settings.SettingsManager
import com.radio.ccbes.ui.navigation.Screen
import com.radio.ccbes.ui.screens.home.PostCard
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.TimeUtils
import com.radio.ccbes.ui.screens.chat.ChatViewModel
import com.radio.ccbes.ui.components.LikesBottomSheet
import com.radio.ccbes.ui.components.CommentsBottomSheet
import com.radio.ccbes.ui.components.PostPlaceholder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController, 
    userId: String? = null,
    viewModel: ProfileViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }
    val postRepository = remember { PostRepository(postDao = AppDatabase.getDatabase(context).postDao(), userRepository = userRepository) }
    val followRepository = remember { FollowRepository() }
    val likeRepository = remember { LikeRepository() }
    val reportRepository = remember { ReportRepository() }
    val scope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsState()
    
    var showSettings by remember { mutableStateOf(false) }
    var isAuthLoading by remember { mutableStateOf(false) }
    
    val settingsManager = remember { SettingsManager(context) }
    val pushNews by settingsManager.pushNotificationsNews.collectAsState(initial = true)
    val pushSocial by settingsManager.pushNotificationsSocial.collectAsState(initial = true)
    val mediaNotif by settingsManager.mediaNotificationEnabled.collectAsState(initial = true)
    val autoPlay by settingsManager.autoPlayRadio.collectAsState(initial = false)
    
    val currentUser = auth.currentUser
    val targetUserId = userId ?: currentUser?.uid
    val sheetState = rememberModalBottomSheetState()
    val listSheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    var showFollowersList by remember { mutableStateOf(false) }
    var showFollowingList by remember { mutableStateOf(false) }
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }
    var isListLoading by remember { mutableStateOf(false) }
    
    // Likes Modal State
    var showLikesModal by remember { mutableStateOf(false) }
    var selectedPostId by remember { mutableStateOf("") }
    
    // Comments Modal State
    var showCommentsModal by remember { mutableStateOf(false) }
    var selectedPostIdForComments by remember { mutableStateOf("") }
    

    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(targetUserId) {
        viewModel.loadProfile(targetUserId)
    }






    // Check if banned
    if (uiState.userProfile?.isBanned == true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.account_banned_title), style = MaterialTheme.typography.headlineMedium, color = RedAccent, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.account_banned_msg), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { 
                    auth.signOut()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0)
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent)) {
                    Text(stringResource(R.string.logout))
                }
            }
        }
        return
    }

    // If not logged in, show Login UI
    if (currentUser == null && !uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (isAuthLoading) {
                CircularProgressIndicator(color = RedAccent)
            } else {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.welcome_to) + " " + stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val idToken = AuthManager.signInWithGoogle(context)
                                if (idToken != null) {
                                    isAuthLoading = true
                                    AuthManager.firebaseAuthWithGoogle(idToken) { success ->
                                        isAuthLoading = false
                                        if (success) {
                                            viewModel.loadProfile(null)
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.login_google))
                    }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
        TopAppBar(
            title = { 
                Text(
                    text = uiState.userProfile?.handle?.let { if (it.startsWith("@")) it else "@$it" } ?: stringResource(R.string.profile_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            scrollBehavior = scrollBehavior,
            actions = {
                if (targetUserId == currentUser?.uid) {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Profile Header Section (Instagram Layout)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = Color.LightGray.copy(alpha = 0.2f)
                        ) {
                            if (uiState.userProfile?.photoUrl != null) {
                                NetworkImage(
                                    url = uiState.userProfile?.photoUrl,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(16.dp),
                                    tint = Color.Gray
                                )
                            }
                        }

                        // Stats Row
                        Row(
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                count = if (uiState.isLoading) "-" else uiState.userPosts.size.toString(), 
                                label = stringResource(R.string.posts_unit)
                            )
                            StatItem(
                                count = if (uiState.isLoading) "-" else uiState.followerCount.toString(), 
                                label = stringResource(R.string.followers_unit),
                                onClick = {
                                    targetUserId?.let { uid ->
                                        scope.launch {
                                            showFollowersList = true
                                            isListLoading = true
                                            val ids = followRepository.getFollowerIds(uid)
                                            userList = userRepository.getUsersByIds(ids)
                                            isListLoading = false
                                        }
                                    }
                                }
                            )
                            StatItem(
                                count = if (uiState.isLoading) "-" else uiState.followingCount.toString(), 
                                label = stringResource(R.string.following_unit),
                                onClick = {
                                    targetUserId?.let { uid ->
                                        scope.launch {
                                            showFollowingList = true
                                            isListLoading = true
                                            val ids = followRepository.getFollowingIds(uid)
                                            userList = userRepository.getUsersByIds(ids)
                                            isListLoading = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Name & Bio display
                    Column {
                        Text(
                            text = uiState.userProfile?.name ?: stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (!uiState.userProfile?.bio.isNullOrBlank()) {
                            Text(
                                text = uiState.userProfile?.bio ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (targetUserId == currentUser?.uid) {
                            Button(
                                onClick = { navController.navigate(Screen.EditProfile.route) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.edit_profile), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { navController.navigate(Screen.ShareProfile.route) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text(stringResource(R.string.share_profile_button), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else if (!uiState.isLoading) {
                            Button(
                                onClick = { viewModel.toggleFollow() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isFollowing) MaterialTheme.colorScheme.surfaceVariant else RedAccent,
                                    contentColor = if (uiState.isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (uiState.isFollowing) stringResource(R.string.following_status) else stringResource(R.string.follow_status), fontWeight = FontWeight.SemiBold)
                            }
                            
                            Button(
                                onClick = { 
                                    targetUserId?.let { uid ->
                                        chatViewModel.startChatWithUser(uid) { chatId ->
                                            navController.navigate(Screen.Chat.createRoute(chatId))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.message), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)

                // User Posts Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (uiState.isPostsLoading && uiState.userPosts.isEmpty()) {
                        repeat(3) {
                            PostPlaceholder()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else if (uiState.userPosts.isEmpty() && !uiState.isPostsLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Text(stringResource(R.string.no_posts_yet), color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    } else {
                        // Obtener todos los likes del usuario de una vez (optimización)
                        var userLikedPosts by remember { mutableStateOf<Set<String>>(emptySet()) }
                        
                        LaunchedEffect(currentUser) {
                            currentUser?.let { user ->
                                userLikedPosts = likeRepository.getAllUserLikes(user.uid)
                            }
                        }

                        uiState.userPosts.forEach { post ->
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
                                            viewModel.deletePost(post.id)
                                        }
                                    }
                                },
                                onProfileClick = { userId ->
                                    if (userId != targetUserId) {
                                        navController.navigate(Screen.Profile.createRoute(userId))
                                    }
                                },
                                onProfileImageClick = { userId ->
                                    if (userId != targetUserId) {
                                        navController.navigate(Screen.Profile.createRoute(userId))
                                    }
                                },
                                onLikesClick = {
                                    selectedPostId = post.id
                                    showLikesModal = true
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 0.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
            }
        }

        // Central Spinner for initial load
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedAccent)
            }
        }
    }


    // Modal Bottom Sheets (Settings, Follow Lists, etc.)
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SettingsGroup(stringResource(R.string.settings_general)) {
                    SettingsToggleItem(
                        title = stringResource(R.string.notif_news), 
                        checked = pushNews,
                        onCheckedChange = { scope.launch { settingsManager.setPushNotificationsNews(it) } }
                    )
                    SettingsToggleItem(
                        title = stringResource(R.string.notif_social), 
                        checked = pushSocial,
                        onCheckedChange = { scope.launch { settingsManager.setPushNotificationsSocial(it) } }
                    )
                }

                SettingsGroup(stringResource(R.string.nav_radio)) {
                    SettingsToggleItem(
                        title = stringResource(R.string.media_control), 
                        checked = mediaNotif,
                        onCheckedChange = { scope.launch { settingsManager.setMediaNotificationEnabled(it) } }
                    )
                    SettingsToggleItem(
                        title = stringResource(R.string.auto_play), 
                        checked = autoPlay,
                        onCheckedChange = { scope.launch { settingsManager.setAutoPlayRadio(it) } }
                    )
                }

                SettingsGroup(stringResource(R.string.settings_account)) {
                    SettingsLinkItem(stringResource(R.string.nav_account_settings), onClick = { 
                        showSettings = false
                        navController.navigate(Screen.AccountSettings.route) 
                    })
                    SettingsLinkItem(stringResource(R.string.nav_about_us), onClick = { 
                        showSettings = false
                        navController.navigate(Screen.AboutUs.route) 
                    })
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showFollowersList || showFollowingList) {
        ModalBottomSheet(
            onDismissRequest = { 
                showFollowersList = false
                showFollowingList = false
                userList = emptyList()
            },
            sheetState = listSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .heightIn(max = 500.dp)
            ) {
                Text(
                    text = if (showFollowersList) stringResource(R.string.followers_unit).replaceFirstChar { it.uppercase() } else stringResource(R.string.following_unit).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isListLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp),
                        color = RedAccent
                    )
                } else if (userList.isEmpty()) {
                    Text(
                        stringResource(R.string.no_users_to_show),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn {
                        items(userList) { user ->
                            UserListItem(
                                user = user,
                                onProfileClick = {
                                    showFollowersList = false
                                    showFollowingList = false
                                    navController.navigate(Screen.Profile.createRoute(user.id))
                                }
                            )
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
fun UserListItem(user: User, onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = Color.LightGray.copy(alpha = 0.2f)
        ) {
            if (user.photoUrl?.isNotEmpty() == true) {
                NetworkImage(
                    url = user.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (user.handle.startsWith("@")) user.handle else "@${user.handle}",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StatItem(count: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(4.dp)
    ) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsToggleItem(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RedAccent
            )
        )
    }
}

@Composable
fun SettingsLinkItem(title: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 16.sp,
            color = if (isDestructive) RedAccent else MaterialTheme.colorScheme.onSurface
        )
        if (!isDestructive) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
