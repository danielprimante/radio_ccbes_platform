package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.radio.ccbes.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun getUser(userId: String): User? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.toObject(User::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByHandle(handle: String): User? {
        return try {
            // Try exact match first
            val query = usersCollection
                .whereEqualTo("handle", handle)
                .limit(1)
                .get()
                .await()
            
            var doc = query.documents.firstOrNull()
            
            // If not found, try with @ prefix just in case some handles are stored that way
            if (doc == null) {
                val alternativeHandle = if (handle.startsWith("@")) handle.substring(1) else "@$handle"
                val altQuery = usersCollection
                    .whereEqualTo("handle", alternativeHandle)
                    .limit(1)
                    .get()
                    .await()
                doc = altQuery.documents.firstOrNull()
            }

            doc?.toObject(User::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateProfile(userId: String, bio: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("bio", bio)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePhotoUrl(userId: String, photoUrl: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("photoUrl", photoUrl)
                .await()
            
            // Cascade update to posts, comments, chats and notifications
            cascadeUserUpdate(userId, newPhotoUrl = photoUrl)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFullProfile(
        userId: String,
        name: String,
        handle: String,
        city: String,
        phone: String,
        email: String,
        bio: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "name" to name,
                "handle" to handle,
                "city" to city,
                "phone" to phone,
                "email" to email,
                "bio" to bio
            )
            usersCollection.document(userId).update(updates).await()
            
            // Cascade update to posts, comments, chats and notifications
            cascadeUserUpdate(userId, newName = name, newHandle = handle)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isHandleAvailable(handle: String, currentUserId: String): Boolean {
        return try {
            val query = usersCollection
                .whereEqualTo("handle", handle)
                .get()
                .await()
            
            // Available if no one has it, OR if only the current user has it
            query.documents.all { it.id == currentUserId }
        } catch (e: Exception) {
            false // Error assumes unavailable for safety
        }
    }

    suspend fun searchUsers(query: String): List<User> {
        return try {
            val normalizedQuery = query.trim().lowercase()
            if (normalizedQuery.isEmpty()) return emptyList()

            // Optimización: Limitamos a 20 resultados y filtramos eficientemente.
            // Para una búsqueda real en producción se recomienda Algolia o Firestore Search.
            val querySnapshot = usersCollection
                .orderBy("name")
                .limit(40) // Tomamos un set razonable para filtrar en memoria
                .get()
                .await()
            
            querySnapshot.toObjects(User::class.java)
                .mapIndexed { index, user -> user.copy(id = querySnapshot.documents[index].id) }
                .filter { 
                    it.name.lowercase().contains(normalizedQuery) || 
                    it.handle.lowercase().contains(normalizedQuery)
                }
                .take(15) // Entregamos solo los 15 más relevantes
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun acceptTerms(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("termsAccepted", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptPrivacy(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("privacyAccepted", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateUser(user: User): Result<Unit> {
        return try {
            val existingUser = getUser(user.id)
            if (existingUser == null) {
                usersCollection.document(user.id).set(user).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "name" to user.name,
                    "handle" to user.handle
                )
                // Mantener estados de aceptación si ya están en true localmente o en el objeto que llega
                if (user.termsAccepted) updates["termsAccepted"] = true
                if (user.privacyAccepted) updates["privacyAccepted"] = true
                
                // Solo actualizamos la foto si el usuario existente no tiene una.
                // Esto permite que el usuario mantenga su foto de ImgBB aunque inicie sesión con Google.
                if (existingUser.photoUrl.isNullOrBlank() && !user.photoUrl.isNullOrBlank()) {
                    updates["photoUrl"] = user.photoUrl
                }
                usersCollection.document(user.id).update(updates).await()
                
                // Cascade update in case name or handle changed
                cascadeUserUpdate(
                    userId = user.id, 
                    newName = user.name, 
                    newHandle = user.handle,
                    newPhotoUrl = if (updates.containsKey("photoUrl")) user.photoUrl else null
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) return emptyList()
        return try {
            // Firestore 'in' query has a limit of 10 items. For larger lists, we need to batch.
            val users = mutableListOf<User>()
            val chunks = userIds.chunked(10)
            for (chunk in chunks) {
                val query = usersCollection.whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get().await()
                users.addAll(query.toObjects(User::class.java).mapIndexed { index, user -> 
                    user.copy(id = query.documents[index].id)
                })
            }
            users
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun deleteUserCompletely(userId: String): Result<Unit> {
        return try {
            // 1. Delete Posts
            val posts = firestore.collection("posts").whereEqualTo("userId", userId).get().await()
            if (!posts.isEmpty) {
                val batch = firestore.batch()
                posts.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }

            // 2. Delete Comments
            val comments = firestore.collection("comments").whereEqualTo("userId", userId).get().await()
            if (!comments.isEmpty) {
                val batch = firestore.batch()
                comments.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }

            // 3. Delete Notifications (sent by the user and received by the user)
            val sentNotifications = firestore.collection("notifications").whereEqualTo("fromUserId", userId).get().await()
            if (!sentNotifications.isEmpty) {
                val batch = firestore.batch()
                sentNotifications.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            
            val receivedNotifications = firestore.collection("notifications").whereEqualTo("userId", userId).get().await()
            if (!receivedNotifications.isEmpty) {
                val batch = firestore.batch()
                receivedNotifications.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }

            // 4. Delete Likes (this is more complex because it affects the post's count)
            val likes = firestore.collection("likes").whereEqualTo("userId", userId).get().await()
            if (!likes.isEmpty) {
                likes.documents.forEach { likeDoc ->
                    val postId = likeDoc.getString("postId")
                    if (postId != null) {
                        // Atomic decrement if possible, or just ignore since the user is gone
                        // For simplicity in a batch-like operation, we'll just delete the like
                        firestore.collection("posts").document(postId)
                            .update("likes", com.google.firebase.firestore.FieldValue.increment(-1))
                    }
                    likeDoc.reference.delete()
                }
            }
            
            // 5. Delete User Profile
            usersCollection.document(userId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cascadeUserUpdate(
        userId: String,
        newName: String? = null,
        newPhotoUrl: String? = null,
        newHandle: String? = null
    ) {
        try {
            val postUpdates = mutableMapOf<String, Any>()
            val commentUpdates = mutableMapOf<String, Any>()
            val notificationUpdates = mutableMapOf<String, Any>()
            val chatUpdates = mutableMapOf<String, Any>()

            newName?.let { 
                postUpdates["userName"] = it
                commentUpdates["userName"] = it
                notificationUpdates["fromUserName"] = it
                chatUpdates["participantNames.$userId"] = it
            }
            newPhotoUrl?.let { 
                postUpdates["userPhotoUrl"] = it
                commentUpdates["userPhotoUrl"] = it
                notificationUpdates["fromUserProfilePic"] = it
                chatUpdates["participantPhotos.$userId"] = it
            }
            newHandle?.let { 
                postUpdates["userHandle"] = it
                chatUpdates["participantHandles.$userId"] = it
            }

            if (postUpdates.isEmpty() && commentUpdates.isEmpty() && notificationUpdates.isEmpty() && chatUpdates.isEmpty()) return

            // Update Posts in batches
            if (postUpdates.isNotEmpty()) {
                val posts = firestore.collection("posts").whereEqualTo("userId", userId).get().await()
                if (!posts.isEmpty) {
                    posts.documents.chunked(500).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { batch.update(it.reference, postUpdates) }
                        batch.commit().await()
                    }
                }
            }

            // Update Comments in batches
            if (commentUpdates.isNotEmpty()) {
                val comments = firestore.collection("comments").whereEqualTo("userId", userId).get().await()
                if (!comments.isEmpty) {
                    comments.documents.chunked(500).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { batch.update(it.reference, commentUpdates) }
                        batch.commit().await()
                    }
                }
            }

            // Update Notifications in batches
            if (notificationUpdates.isNotEmpty()) {
                val notifications = firestore.collection("notifications").whereEqualTo("fromUserId", userId).get().await()
                if (!notifications.isEmpty) {
                    notifications.documents.chunked(500).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { batch.update(it.reference, notificationUpdates) }
                        batch.commit().await()
                    }
                }
            }

            // Update Chats in batches (using dot notation for Map fields)
            if (chatUpdates.isNotEmpty()) {
                val chats = firestore.collection("chats").whereArrayContains("participants", userId).get().await()
                if (!chats.isEmpty) {
                    chats.documents.chunked(500).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { batch.update(it.reference, chatUpdates) }
                        batch.commit().await()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
