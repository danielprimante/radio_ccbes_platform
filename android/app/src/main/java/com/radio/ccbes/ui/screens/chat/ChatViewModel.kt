package com.radio.ccbes.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.radio.ccbes.data.model.Chat
import com.radio.ccbes.data.model.Message
import com.radio.ccbes.data.repository.ChatRepository
import com.radio.ccbes.data.repository.ImageUploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = true,
    val otherUserName: String = "",
    val otherUserPhoto: String = "",
    val otherUserHandle: String = "",
    val otherUserId: String = "",
    val isUploading: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val imageRepository = ImageUploadRepository()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentChatId: String? = null

    fun initChat(chatId: String) {
        if (currentChatId == chatId) return
        currentChatId = chatId
        
        viewModelScope.launch {
            // Get chat metadata first
            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            val chat = chatDoc.toObject(Chat::class.java)
            val currentUid = auth.currentUser?.uid ?: ""
            val otherUid = chat?.participants?.find { it != currentUid } ?: ""
            
            _uiState.value = _uiState.value.copy(
                otherUserName = chat?.participantNames?.get(otherUid) ?: "Usuario",
                otherUserPhoto = chat?.participantPhotos?.get(otherUid) ?: "",
                otherUserHandle = chat?.participantHandles?.get(otherUid) ?: "",
                otherUserId = otherUid
            )

            chatRepository.getMessages(chatId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages, isLoading = false)
            }
        }
    }

    fun sendMessage(content: String, type: String = "text") {
        val chatId = currentChatId ?: return
        val senderId = auth.currentUser?.uid ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            chatRepository.sendMessage(chatId, senderId, content)
        }
    }

    fun sendImage(context: Context, uri: Uri) {
        val chatId = currentChatId ?: return
        val senderId = auth.currentUser?.uid ?: return
        
        _uiState.value = _uiState.value.copy(isUploading = true)
        
        viewModelScope.launch {
            val result = imageRepository.uploadImage(context, uri)
            result.onSuccess { url ->
                chatRepository.sendMessage(chatId, senderId, url, type = "image")
            }
            _uiState.value = _uiState.value.copy(isUploading = false)
        }
    }

    fun startChatWithUser(otherUserId: String, onChatIdReady: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val chatId = chatRepository.getOrCreateChat(currentUserId, otherUserId)
            onChatIdReady(chatId)
        }
    }

    fun deleteChat(onDeleted: () -> Unit) {
        val chatId = currentChatId ?: return
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
            onDeleted()
        }
    }

    fun reportUser(reason: String) {
        val senderId = auth.currentUser?.uid ?: return
        val reportedUserId = _uiState.value.otherUserId
        if (reportedUserId.isEmpty()) return
        
        viewModelScope.launch {
            chatRepository.reportUser(senderId, reportedUserId, reason)
        }
    }

    fun blockUser() {
        val currentUserId = auth.currentUser?.uid ?: return
        val blockedUserId = _uiState.value.otherUserId
        if (blockedUserId.isEmpty()) return

        viewModelScope.launch {
            chatRepository.blockUser(currentUserId, blockedUserId)
        }
    }
    
    fun editMessage(messageId: String, newContent: String) {
        val chatId = currentChatId ?: return
        if (newContent.isBlank()) return
        
        viewModelScope.launch {
            try {
                chatRepository.editMessage(chatId, messageId, newContent)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error editing message", e)
            }
        }
    }
    
    fun deleteMessage(messageId: String) {
        val chatId = currentChatId ?: return
        
        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(chatId, messageId)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error deleting message", e)
            }
        }
    }
}
