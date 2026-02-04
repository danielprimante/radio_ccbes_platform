package com.radio.ccbes.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

@Keep
data class Comment(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val likesCount: Int = 0,
    val timestamp: Timestamp = Timestamp.now()
)
