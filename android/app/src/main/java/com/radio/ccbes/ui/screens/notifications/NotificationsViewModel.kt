package com.radio.ccbes.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.radio.ccbes.data.model.Notification
import com.radio.ccbes.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.getNotificationsForUser(userId).collect {
                _notifications.value = it
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.markAllAsRead(userId)
        }
    }

    fun deleteAll() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.deleteAllNotifications(userId)
            // Optional: refresh list immediately or rely on flow collection
             loadNotifications()
        }
    }
}
