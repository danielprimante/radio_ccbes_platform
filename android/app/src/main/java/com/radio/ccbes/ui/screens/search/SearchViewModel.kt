package com.radio.ccbes.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.model.PostCategory
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.data.repository.FollowRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository
    private val userRepository = UserRepository()
    private val followRepository = FollowRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<Post>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getPostsByCategory(PostCategory.ALL)
            } else {
                flow { emit(repository.searchPosts(query)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _userResults = MutableStateFlow<List<com.radio.ccbes.data.model.User>>(emptyList())
    val userResults: StateFlow<List<com.radio.ccbes.data.model.User>> = _userResults.asStateFlow()

    private val _followStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followStatus: StateFlow<Map<String, Boolean>> = _followStatus.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PostRepository(database.postDao())
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        performSearch(query)
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // If query is blank, we are showing default feed (ALL posts). 
            // In that case, we can refresh via repository or just re-fetch.
            // Repository `getPostsByCategory` returns a Flow observing DB. 
            // So we might need to trigger a sync manually if needed, or just let it be.
            // For explicit refresh, let's re-run performSearch which might trigger network fetch if we add logic for it.
            // Or since `performSearch` calls `repository.searchPosts` (network + local), it's good.
            performSearch(_searchQuery.value)
            _isRefreshing.value = false
        }
    }

    fun deletePost(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            repository.deletePost(postId, userId)
        }
    }
    
    private fun performSearch(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Also search for users (this part remains manual as it's not a Flow in repository yet)
                val users = userRepository.searchUsers(query)
                _userResults.value = users
                
                // Check follow status for results
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != null) {
                    val statusMap = mutableMapOf<String, Boolean>()
                    for (user in users) {
                        statusMap[user.id] = followRepository.isFollowing(currentUserId, user.id)
                    }
                    _followStatus.value = statusMap
                }
            } catch (_: Exception) {
                _userResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val isFollowing = _followStatus.value[targetUserId] ?: false
            if (isFollowing) {
                followRepository.unfollowUser(currentUserId, targetUserId)
            } else {
                followRepository.followUser(currentUserId, targetUserId)
            }
            // Update local status
            _followStatus.value = _followStatus.value.toMutableMap().apply {
                put(targetUserId, !isFollowing)
            }
        }
    }
}
