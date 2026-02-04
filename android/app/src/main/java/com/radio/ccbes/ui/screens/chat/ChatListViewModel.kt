package com.radio.ccbes.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.Chat
import com.radio.ccbes.data.model.User
import com.radio.ccbes.data.repository.ChatRepository
import com.radio.ccbes.data.repository.FollowRepository
import com.radio.ccbes.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val followers: List<User> = emptyList(),
    val searchResults: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val isSearching: Boolean = false
)

class ChatListViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            // Cargar chats en tiempo real
            launch {
                chatRepository.getChats(currentUserId).collect { chats ->
                    _uiState.value = _uiState.value.copy(chats = chats, isLoading = false)
                }
            }
            
            // Cargar seguidores (lista estática inicial)
            launch {
                try {
                    val followerIds = FollowRepository().getFollowerIds(currentUserId)
                    val followerUsers = UserRepository().getUsersByIds(followerIds)
                    _uiState.value = _uiState.value.copy(followers = followerUsers)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            try {
                val results = UserRepository().searchUsers(query)
                _uiState.value = _uiState.value.copy(
                    searchResults = results,
                    isSearching = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    searchResults = emptyList(),
                    isSearching = false
                )
            }
        }
    }
}
