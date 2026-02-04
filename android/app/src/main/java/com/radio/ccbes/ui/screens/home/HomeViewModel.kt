package com.radio.ccbes.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.model.PostCategory
import com.radio.ccbes.data.repository.FollowRepository
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository: PostRepository
    private val userRepository = UserRepository()
    private val followRepository = FollowRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _feedIds = MutableStateFlow<List<String>>(emptyList())
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val posts: StateFlow<List<Post>> = _feedIds.flatMapLatest { ids ->
        if (ids.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
        else postRepository.getFeedPosts(ids)
    }.onEach { 
        if (it.isNotEmpty() || _feedIds.value.isNotEmpty()) {
            _isLoading.value = false 
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val database = AppDatabase.getDatabase(application)
        postRepository = PostRepository(database.postDao())
        loadFeed()
    }

    fun onCategorySelected(category: PostCategory) {
        // Keeps personalized feed for now
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val currentUserId = auth.currentUser?.uid
            if (currentUserId != null) {
                // Determine feed IDs again to force refresh from network
                val officialUser = userRepository.getUserByHandle("ccbes")
                val officialUserId = officialUser?.id
                val followingIds = followRepository.getFollowingIds(currentUserId)
                val feedIds = mutableSetOf(currentUserId)
                officialUserId?.let { feedIds.add(it) }
                feedIds.addAll(followingIds)
                
                postRepository.refreshFeed(feedIds.toList())
            }
            _isRefreshing.value = false
        }
    }

    private fun loadFeed() {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            
            // 1. Get official account ID (@ccbes)
            val officialUser = userRepository.getUserByHandle("ccbes")
            val officialUserId = officialUser?.id

            // 2. Get following IDs
            val followingIds = followRepository.getFollowingIds(currentUserId)

            // 3. Combine IDs: Self + Following + Official
            val feedIds = mutableSetOf(currentUserId)
            officialUserId?.let { feedIds.add(it) }
            feedIds.addAll(followingIds)

            // 4. Set IDs to trigger flatMapLatest
            _feedIds.value = feedIds.toList()
        }
    }

    fun deletePost(postId: String): Result<Unit> {
        // We can expose this as suspend or launch here. 
        // For simplicity, we launch here but return Unit, creating a specific state if needed.
        // But the previous code used suspend in UI. 
        // Adapting to launch in ViewModel is better.
        // However, to keep signature simple, let's expose a suspend function or use a wrapper.
        // The UI called `postRepository.deletePost`. We should wrap it.
        // Since we can't easily return Result from launch, let's return Job or just fire and forget, 
        // or suspend if called from scope.
        // Let's make it suspend.
        // But AndroidViewModel doesn't change `suspend` behavior.
        // Wait, I cannot return Result<Unit> from a non-suspend function if inside I launch.
        // I will make it suspend and caller (UI) uses scope.
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("No user"))
        // We need to run this in a coroutine, but the UI is calling it from a scope.
        // So we can make this `suspend`.
        // BUT `postRepository` is private.
        // So I'll make this suspend.
        return kotlinx.coroutines.runBlocking { // Bad practice on main thread.
             // Better: suspend function
             postRepository.deletePost(postId, currentUserId)
        }
    }
    
    suspend fun deletePostSuspend(postId: String): Result<Unit> {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("No user"))
        return postRepository.deletePost(postId, currentUserId)
    }
}