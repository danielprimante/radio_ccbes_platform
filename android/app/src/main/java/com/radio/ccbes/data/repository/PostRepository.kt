package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentChange
import com.radio.ccbes.data.cache.PostDao
import com.radio.ccbes.data.cache.PostEntity
import com.radio.ccbes.data.cache.toDomain
import com.radio.ccbes.data.cache.toEntity
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.model.PostCategory
import com.radio.ccbes.data.repository.ImageUploadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.ListenerRegistration

class PostRepository(
    private val postDao: PostDao,
    private val userRepository: UserRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Get posts by category with real-time updates and offline support
     */
    fun getPostsByCategory(category: PostCategory): Flow<List<Post>> {
        return if (category == PostCategory.ALL) {
            postDao.getAllPosts().map { entities -> 
                entities.map { it.toDomain() }
            }
        } else {
             postDao.getPostsByCategory(category.value).map { entities ->
                 entities.map { it.toDomain() }
             }
        }
    }

    fun syncPostsByCategory(category: PostCategory): Flow<Unit> = callbackFlow {
        val query = if (category == PostCategory.ALL) {
            postsCollection.orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
        } else {
            postsCollection
                .whereEqualTo("category", category.value)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            
            scope.launch {
                val changedDocs = snapshot.documentChanges
                for (change in changedDocs) {
                    when (change.type) {
                        DocumentChange.Type.REMOVED -> {
                            postDao.deletePost(change.document.id)
                        }
                        else -> {
                            val post = change.document.toObject(Post::class.java).copy(id = change.document.id)
                            val user = userRepository.getUser(post.userId)
                            val updatedPost = if (user != null) {
                                post.copy(
                                    userName = user.name,
                                    userHandle = user.handle,
                                    userPhotoUrl = user.photoUrl
                                )
                            } else {
                                post
                            }
                            postDao.insertPost(updatedPost.toEntity())
                        }
                    }
                }
            }
        }
        
        awaitClose { 
             registration.remove()
        }
    }

    /**
     * Get feed posts with real-time sync
     */
    fun getFeedPosts(userIds: List<String>): Flow<List<Post>> {
        if (userIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())

        return postDao.getFeedPosts(userIds).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun syncFeedPosts(userIds: List<String>): Flow<Unit> = callbackFlow {
         // Firestore limit 30 for 'whereIn'
         val idsToQuery = userIds.take(30)
         if (idsToQuery.isEmpty()) {
             close()
             return@callbackFlow
         }

         val query = postsCollection
             .whereIn("userId", idsToQuery)
             .orderBy("timestamp", Query.Direction.DESCENDING)
             .limit(50)

         val registration = query.addSnapshotListener { snapshot, error ->
             if (error != null || snapshot == null) return@addSnapshotListener
             
             scope.launch {
                 val changedDocs = snapshot.documentChanges
                 for (change in changedDocs) {
                     when (change.type) {
                         DocumentChange.Type.REMOVED -> {
                             postDao.deletePost(change.document.id)
                         }
                         else -> {
                             val post = change.document.toObject(Post::class.java).copy(id = change.document.id)
                             val user = userRepository.getUser(post.userId)
                             val updatedPost = if (user != null) {
                                 post.copy(
                                     userName = user.name,
                                     userHandle = user.handle,
                                     userPhotoUrl = user.photoUrl
                                 )
                             } else {
                                 post
                             }
                             postDao.insertPost(updatedPost.toEntity())
                         }
                     }
                 }
             }
         }
         
         awaitClose { registration.remove() }
    }

    /**
     * Get user posts
     */
    fun getUserPosts(userId: String): Flow<List<Post>> {
        return postDao.getPostsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun syncUserPosts(userId: String): Flow<Unit> = callbackFlow {
        val registration = postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                scope.launch {
                    val changedDocs = snapshot.documentChanges
                    for (change in changedDocs) {
                        when (change.type) {
                            DocumentChange.Type.REMOVED -> {
                                postDao.deletePost(change.document.id)
                            }
                            else -> {
                                val post = change.document.toObject(Post::class.java).copy(id = change.document.id)
                                // Even for a single user, fetch fresh data to ensure profile is up to date
                                val user = userRepository.getUser(userId)
                                
                                val updatedPost = if (user != null) {
                                    post.copy(
                                        userName = user.name,
                                        userHandle = user.handle,
                                        userPhotoUrl = user.photoUrl
                                    )
                                } else {
                                    post
                                }
                                
                                postDao.insertPost(updatedPost.toEntity())
                            }
                        }
                    }
                }
            }
            
        awaitClose { registration.remove() }
    }

    // Manual refresh (still useful for pull-to-refresh)
    suspend fun refreshFeed(userIds: List<String>) {
        try {
            val idsToQuery = userIds.take(30)
             if (idsToQuery.isEmpty()) return

             val snapshot = postsCollection
                 .whereIn("userId", idsToQuery)
                 .orderBy("timestamp", Query.Direction.DESCENDING)
                 .limit(50)
                 .get().await()

             val posts = snapshot.documents.mapNotNull { doc ->
                 doc.toObject(Post::class.java)?.copy(id = doc.id)
             }
             
             val postIds = posts.map { it.id }
             
            // Fetch fresh user data to update profile images
            val authorIds = posts.map { it.userId }.distinct()
            val users = userRepository.getUsersByIds(authorIds)
            val userMap = users.associateBy { it.id }
            
            val updatedPosts = posts.map { post ->
                val user = userMap[post.userId]
                if (user != null) {
                    post.copy(
                        userName = user.name,
                        userHandle = user.handle,
                        userPhotoUrl = user.photoUrl
                    )
                } else {
                    post
                }
            }

             postDao.insertPosts(updatedPosts.map { it.toEntity() })
             
             postDao.insertPosts(updatedPosts.map { it.toEntity() })
             
             // Reconciliation: 
             // Instead of deleting posts not in the list (which kills offline cache for older posts),
             // we just keep the cache size manageable.
             postDao.keepMaxPosts(500)
             
        } catch (e: Exception) {
            // Log error or ignore
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
             
            val postIds = posts.map { it.id }
            val user = userRepository.getUser(userId)
            val updatedPosts = if (user != null) {
                posts.map { post ->
                    post.copy(
                        userName = user.name,
                        userHandle = user.handle,
                        userPhotoUrl = user.photoUrl
                    )
                }
            } else {
                posts
            }

             postDao.insertPosts(updatedPosts.map { it.toEntity() })
             
             postDao.insertPosts(updatedPosts.map { it.toEntity() })
             
             // No deletePostsByUserNotInList here either, to preserve history.
             // If a post is actually deleted, the real-time listener or a specific check should handle it.
        } catch (e: Exception) { }
    }


    /**
     * Get a single post by ID (Network first, then Cache)
     */
    suspend fun getPostById(postId: String): Post? {
        return try {
            val doc = postsCollection.document(postId).get().await()
            var post = doc.toObject(Post::class.java)?.copy(id = doc.id)
            if (post != null) {
                // Fetch user to update
                val user = userRepository.getUser(post.userId)
                if (user != null) {
                    post = post.copy(
                        userName = user.name,
                        userHandle = user.handle,
                        userPhotoUrl = user.photoUrl
                    )
                }
                
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
            
            // Sync users for these posts too?
            // Yes, strict consistency.
            val authorIds = networkPosts.map { it.userId }.distinct()
            val users = userRepository.getUsersByIds(authorIds)
            val userMap = users.associateBy { it.id }
            
            val updatedPosts = networkPosts.map { post ->
                val user = userMap[post.userId]
                if (user != null) {
                    post.copy(
                        userName = user.name,
                        userHandle = user.handle,
                        userPhotoUrl = user.photoUrl
                    )
                } else {
                    post
                }
            }

            // Insert all fetched into DB so next search finds them
            postDao.insertPosts(updatedPosts.map { it.toEntity() })
            
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
    private val imageUploadRepository = ImageUploadRepository()

    /**
     * Update post contents
     */
    suspend fun updatePost(
        postId: String, 
        currentUserId: String, 
        content: String, 
        imageUrl: String?, 
        imageDeleteUrl: String?,
        images: List<String>?,
        imagesDeleteUrls: List<String>?
    ): Result<Unit> {
        return try {
            // Verify ownership logic remains check only on server side or locally if we have the post
            val updates = mutableMapOf<String, Any>(
                "content" to content
            )
            if (imageUrl != null) updates["imageUrl"] = imageUrl
            if (imageDeleteUrl != null) updates["imageDeleteUrl"] = imageDeleteUrl
            if (images != null) updates["images"] = images
            if (imagesDeleteUrls != null) updates["imagesDeleteUrls"] = imagesDeleteUrls
            
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
            // 1. Fetch post to get image URLs
            val postSnapshot = postsCollection.document(postId).get().await()
            val post = postSnapshot.toObject(Post::class.java)

            if (post != null) {
                // 2. Delete images from ImgBB
                post.imageDeleteUrl?.let { imageUploadRepository.deleteImage(it) }
                post.imagesDeleteUrls.forEach { imageUploadRepository.deleteImage(it) }
            }

            // 3. Delete post from Firestore
            postsCollection.document(postId).delete().await()
            
            // 4. Delete from local cache
            postDao.deletePost(postId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
