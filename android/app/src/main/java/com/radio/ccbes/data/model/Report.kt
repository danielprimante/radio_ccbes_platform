package com.radio.ccbes.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Report(
    @DocumentId
    val id: String = "",
    val postId: String? = null,
    val commentId: String? = null,
    val reportedBy: String = "",
    val reason: String = "",
    val status: String = "pending", // pending, reviewed, dismissed
    val timestamp: Timestamp = Timestamp.now()
)
