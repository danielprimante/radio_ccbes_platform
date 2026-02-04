package com.radio.ccbes.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.radio.ccbes.data.model.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val userHandle: String,
    val userPhotoUrl: String?,
    val content: String,
    val imageUrl: String?,
    val images: List<String>,
    val likes: Int,
    val comments: Int,
    val timestamp: Timestamp,
    val category: String
)

fun PostEntity.toDomain(): Post {
    // Post has a constructor which takes `_category` as private val, so we might need to use `copy` or standard constructor if accessible.
    // Post data class:
    // data class Post(
    //     ...
    //     @PropertyName("category")
    //     private val _category: String = "all"
    // )
    // We can just pass it to the constructor.
    return Post(
        id = id,
        userId = userId,
        userName = userName,
        userHandle = userHandle,
        userPhotoUrl = userPhotoUrl,
        content = content,
        imageUrl = imageUrl,
        images = images,
        likes = likes,
        comments = comments,
        timestamp = timestamp,
        _category = category
    )
}

fun Post.toEntity(): PostEntity {
    return PostEntity(
        id = id,
        userId = userId,
        userName = userName,
        userHandle = userHandle,
        userPhotoUrl = userPhotoUrl,
        content = content,
        imageUrl = imageUrl,
        images = images,
        likes = likes,
        comments = comments,
        timestamp = timestamp,
        category = category.value
    )
}
