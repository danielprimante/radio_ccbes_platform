package com.radio.ccbes.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
data class Program(
    @DocumentId val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val isActive: Boolean = false
)
