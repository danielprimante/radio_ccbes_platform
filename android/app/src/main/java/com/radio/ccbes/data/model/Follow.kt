package com.radio.ccbes.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Follow(
    @DocumentId
    val id: String = "",
    val followerId: String = "",
    val followingId: String = "",
    val timestamp: Timestamp = Timestamp.now()
)