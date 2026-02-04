package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.radio.ccbes.data.cache.PostDao
import com.radio.ccbes.data.cache.PostEntity
import com.radio.ccbes.data.cache.toDomain
import com.radio.ccbes.data.cache.toEntity
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.model.PostCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostRepository(private val postDao: PostDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Get posts by category with real-time updates and offline support
     */
    fun getPostsByCategory(category: PostCategory): Flow<List<Post>> {
        // Start real-time sync
        syncPostsByCategory(category)
        
        // Return local data observed
        return if (category == PostCategory.ALL) {
            postDao.getAllPosts().map { entities -> 
                entities.map { it.toDomain() }
            }
        } else {
             // For simplicity, we might filter in memory or fetch all and filter, 
             // but ideally we should have a query in DAO for filtering. 
             // Since our current DAO `getAllPosts` returns everything, we filter here for now
             // if we don't add specific queries.
             // BETTER: Let's assume we filter on client side for now or query all and filter.
             // OR: We create a specific DAO method. 
             // For this iteration, let's filter in the map transformation for simplicity as `getAllPosts` is ordered by time.
             // Note: This loads all posts from DB. For optimization, add DAO query `getPostsByCategory` if needed.
             // Given the requirements, I will just filter the stream from getAllPosts.
             postDao.getAllPosts().map { entities ->
                 entities
                     .map { it.toDomain() }
                     .filter { it.category == category }
             }
        }
    }

    private fun syncPostsByCategory(category: PostCategory) {
        val query = if (category == PostCategory.ALL) {
            postsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
        } else {
            postsCollection
                .whereEqualTo("category", category.value)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        }

        query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            
            scope.launch {
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }
                postDao.insertPosts(posts.map { it.toEntity() })
            }
        }
    }

    /**
     * Get feed posts with real-time sync
     */
    fun getFeedPosts(userIds: List<String>): Flow<List<Post>> {
        if (userIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())

        // Start Sync
        syncFeedPosts(userIds)

        // Return Local
        return postDao.getFeedPosts(userIds).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun syncFeedPosts(userIds: List<String>) {
         // Firestore limit 30 for 'whereIn'
         val idsToQuery = userIds.take(30)
         if (idsToQuery.isEmpty()) return

         val query = postsCollection
             .whereIn("userId", idsToQuery)
             .orderBy("timestamp", Query.Direction.DESCENDING)

         query.addSnapshotListener { snapshot, error ->
             if (error != null || snapshot == null) return@addSnapshotListener
             
             scope.launch {
                 val posts = snapshot.documents.mapNotNull { doc ->
                     doc.toObject(Post::class.java)?.copy(id = doc.id)
                 }
                 postDao.insertPosts(posts.map { it.toEntity() })
             }
         }
    }

    /**
     * Get user posts
     */
    fun getUserPosts(userId: String): Flow<List<Post>> {
        syncUserPosts(userId)
        return postDao.getPostsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun syncUserPosts(userId: String) {
        postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                scope.launch {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                    postDao.insertPosts(posts.map { it.toEntity() })
                }
            }
    }

    // Force refresh (Fetch from network manually if needed, though listeners handle it)
    suspend fun refreshFeed(userIds: List<String>) {
        // Since we have real-time listeners, 'refresh' mainly ensures we are connected 
        // or re-triggers the query. But for manual Pull-to-Refresh with Room+Listeners, 
        // usually we just rely on the listener. 
        // Use single fetch to force update if listener dropped or for UX feedback.
        try {
            val idsToQuery = userIds.take(30)
             if (idsToQuery.isEmpty()) return

             val snapshot = postsCollection
                 .whereIn("userId", idsToQuery)
                 .orderBy("timestamp", Query.Direction.DESCENDING)
                 .get().await()

             val posts = snapshot.documents.mapNotNull { doc ->
                 doc.toObject(Post::class.java)?.copy(id = doc.id)
             }
             postDao.insertPosts(posts.map { it.toEntity() })
        } catch (e: Exception) {
            // Log error or ignore in offline
        }
    }
    
    suspend fun refreshUserPosts(userId: String) {
        try {
            val snapshot = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
                
            val posts = snapshot.documents.mapNotNull { doc ->
                 doc.toObject(Post::class.java)?.copy(id = doc.id)
             }
             postDao.insertPosts(posts.map { it.toEntity() })
        } catch (e: Exception) { }
    }


    /**
     * Get a single post by ID (Network first, then Cache)
     */
    suspend fun getPostById(postId: String): Post? {
        return try {
            val doc = postsCollection.document(postId).get().await()
            val post = doc.toObject(Post::class.java)?.copy(id = doc.id)
            if (post != null) {
                // Cache it
                postDao.insertPost(post.toEntity())
            }
            post
        } catch (e: Exception) {
            // Fallback to cache? Need getPostById in DAO. 
            // For now return null or implement local fetch if needed.
            null
        }
    }

    /**
     * Search posts (Local Cache Search)
     */
    suspend fun searchPosts(query: String): List<Post> {
        // Search locally for instant results
        val localResults = postDao.searchPosts(query).map { it.toDomain() }
        
        // Optionally trigger network search to augment results (and cache them)
        // But for "Offline first", returning local is prioritary.
        // If we want hybrid:
        try {
            val snapshot = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
                
            val networkPosts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            // Insert all fetched into DB so next search finds them
            postDao.insertPosts(networkPosts.map { it.toEntity() })
            
            // Re-query local to include new ones? 
            // Or just return network filtered? 
            // Let's return local results after update to ensure consistency
            return postDao.searchPosts(query).map { it.toDomain() }
        } catch (e: Exception) {
            return localResults
        }
    }

    /**
     * Create a new post
     */
    suspend fun createPost(post: Post): Result<String> {
        return try {
            val docRef = postsCollection.add(post).await()
            // Optimistic update?
            postDao.insertPost(post.copy(id = docRef.id).toEntity())
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update post likes
     */
    suspend fun updateLikes(postId: String, likes: Int): Result<Unit> {
        return try {
            postsCollection.document(postId)
                .update("likes", likes)
                .await()
             // Local update will happen via listener, but we can optimistically update
             // postDao.updateLikes... (need to add to DAO)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update post contents
     */
    suspend fun updatePost(postId: String, currentUserId: String, content: String, imageUrl: String?, images: List<String>?): Result<Unit> {
        return try {
            // Verify ownership logic remains check only on server side or locally if we have the post
            val updates = mutableMapOf<String, Any>(
                "content" to content
            )
            if (imageUrl != null) updates["imageUrl"] = imageUrl
            if (images != null) updates["images"] = images
            
            postsCollection.document(postId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * Update post comments count
     */
    suspend fun updateComments(postId: String, comments: Int): Result<Unit> {
        return try {
            postsCollection.document(postId)
                .update("comments", comments)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a post
     */
    suspend fun deletePost(postId: String, currentUserId: String): Result<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            postDao.deletePost(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
