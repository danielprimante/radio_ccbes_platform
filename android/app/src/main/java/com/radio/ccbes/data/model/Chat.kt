package com.radio.ccbes.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

@Keep
data class Chat(
    @DocumentId
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val lastMessageSenderId: String = "",
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val participantHandles: Map<String, String> = emptyMap()
)
