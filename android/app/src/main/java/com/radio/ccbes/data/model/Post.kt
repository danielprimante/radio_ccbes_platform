package com.radio.ccbes.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

@Keep
data class Post(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userHandle: String = "",
    val userPhotoUrl: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val images: List<String> = emptyList(),
    val likes: Int = 0,
    val comments: Int = 0,
    val timestamp: Timestamp = Timestamp.now(),
    @PropertyName("category")
    private val _category: String = "all"
) {
    // Computed property to convert string to enum
    val category: PostCategory
        get() = PostCategory.fromValue(_category)
}

@Keep
enum class PostCategory(val value: String, val displayName: String) {
    ALL("all", "Todo"),
    TRENDING("trending", "Tendencias"),
    NEWS("news", "Noticias"),
    REFLECTIONS("reflections", "Reflexiones");

    companion object {
        fun fromValue(value: String): PostCategory {
            return values().find { it.value == value } ?: ALL
        }
    }
}
