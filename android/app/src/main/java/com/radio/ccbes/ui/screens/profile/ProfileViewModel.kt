package com.radio.ccbes.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.repository.FollowRepository
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userProfile: User? = null,
    val userPosts: List<Post> = emptyList(),
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val isPostsLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository()
    private val postRepository: PostRepository
    private val followRepository = FollowRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        val database = AppDatabase.getDatabase(application)
        postRepository = PostRepository(database.postDao(), userRepository)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            // Trigger refresh. 
            // If loadProfile is called, it observes Flow, so we might just want to 
            // force a sync in repository or re-fetch profile data.
            currentUserId?.let { uid ->
                // Reload profile data (not realtime usually)
                val profile = userRepository.getUser(uid)
                val followers = followRepository.getFollowersCount(uid)
                val following = followRepository.getFollowingCount(uid)
                val followingStatus = auth.currentUser?.uid?.let { 
                    followRepository.isFollowing(it, uid) 
                } ?: false

                 _uiState.value = _uiState.value.copy(
                    userProfile = profile ?: _uiState.value.userProfile,
                    followerCount = followers,
                    followingCount = following,
                    isFollowing = followingStatus
                )
                
                // Force posts refresh
                postRepository.refreshUserPosts(uid)
            }
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }
    
    fun deletePost(postId: String) {
        val currentUid = auth.currentUser?.uid ?: return
         viewModelScope.launch {
             postRepository.deletePost(postId, currentUid)
             // Flow should update automatically
         }
    }

    fun loadProfile(userId: String?) {
        val targetUid = userId ?: auth.currentUser?.uid ?: return
        
        if (currentUserId == targetUid) return
        currentUserId = targetUid

        _uiState.value = _uiState.value.copy(isLoading = true, isPostsLoading = true)

        viewModelScope.launch {
            val profile = userRepository.getUser(targetUid)
            val followers = followRepository.getFollowersCount(targetUid)
            val following = followRepository.getFollowingCount(targetUid)
            val followingStatus = auth.currentUser?.uid?.let { 
                followRepository.isFollowing(it, targetUid) 
            } ?: false

            _uiState.value = _uiState.value.copy(
                userProfile = profile,
                followerCount = followers,
                followingCount = following,
                isFollowing = followingStatus,
                isLoading = false
            )

            // Start Sync
            postRepository.syncUserPosts(targetUid)
                .catch { e -> e.printStackTrace() }
                .launchIn(this)

            postRepository.getUserPosts(targetUid).collectLatest { posts ->
                _uiState.value = _uiState.value.copy(
                    userPosts = posts,
                    isPostsLoading = false
                )
            }
        }
    }

    fun toggleFollow() {
        val targetId = currentUserId ?: return
        val currentUid = auth.currentUser?.uid ?: return
        val isCurrentlyFollowing = _uiState.value.isFollowing

        viewModelScope.launch {
            if (isCurrentlyFollowing) {
                followRepository.unfollowUser(currentUid, targetId)
                _uiState.value = _uiState.value.copy(
                    isFollowing = false,
                    followerCount = (_uiState.value.followerCount - 1).coerceAtLeast(0)
                )
            } else {
                followRepository.followUser(currentUid, targetId)
                _uiState.value = _uiState.value.copy(
                    isFollowing = true,
                    followerCount = _uiState.value.followerCount + 1
                )
            }
        }
    }
}
