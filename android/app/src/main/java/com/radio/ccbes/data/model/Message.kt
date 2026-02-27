package com.radio.ccbes.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

@Keep
data class Message(
    @DocumentId
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val type: String = "text", // text, image, etc.
    val deleteUrl: String? = null,
    val readBy: List<String> = emptyList(),
    val isEdited: Boolean = false,
    val postId: String? = null
)
