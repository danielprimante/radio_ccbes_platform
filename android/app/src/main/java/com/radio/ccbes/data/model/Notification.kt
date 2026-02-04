package com.radio.ccbes.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

enum class NotificationType(val value: String) {
    LIKE("like"),
    COMMENT("comment"),
    FOLLOW("follow"),
    MESSAGE("message")
}

data class Notification(
    val id: String = "",
    val type: String = "", // "like", "comment", "follow", "message"
    val fromUserId: String = "",
    val fromUserName: String = "",
    val fromUserProfilePic: String = "",
    val toUserId: String = "",
    val postId: String? = null,
    val chatId: String? = null,
    val postContent: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    @get:PropertyName("isRead") val isRead: Boolean = false
)
