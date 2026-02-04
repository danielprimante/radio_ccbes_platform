package com.radio.ccbes.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUser(userId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId IN (:userIds) ORDER BY timestamp DESC")
    fun getFeedPosts(userIds: List<String>): Flow<List<PostEntity>>

    // Search query
    @Query("SELECT * FROM posts WHERE content LIKE '%' || :query || '%' OR userName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchPosts(query: String): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)
    
    @Query("DELETE FROM posts")
    suspend fun clearAll()
}
