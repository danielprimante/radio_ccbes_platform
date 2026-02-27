package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.radio.ccbes.data.model.Notification
import com.radio.ccbes.data.model.NotificationType
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.service.OneSignalService
import kotlinx.coroutines.tasks.await

data class Like(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

class LikeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val likesCollection = firestore.collection("likes")
    private val postsCollection = firestore.collection("posts")
    private val notificationRepository = NotificationRepository()
    private val userRepository = UserRepository()
    private val oneSignalService = OneSignalService()

    suspend fun hasUserLikedPost(postId: String, userId: String): Boolean {
        return try {
            // 1. Intentar con el ID compuesto moderno
            val likeId = "${postId}_${userId}"
            if (likesCollection.document(likeId).get().await().exists()) return true
            
            // 2. Si no, buscar por campos (maneja formato legado b/c e IDs autogenerados)
            val snapshot = likesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("postId", postId)
                .limit(1)
                .get()
                .await()
            if (!snapshot.isEmpty) return true

            // 3. Buscar por campos legados
            val snapshotLegacy = likesCollection
                .whereEqualTo("c", userId)
                .whereEqualTo("b", postId)
                .limit(1)
                .get()
                .await()
            
            !snapshotLegacy.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return try {
            // Usar un ID compuesto predecible para evitar duplicados
            val likeId = "${postId}_${userId}"
            val likeRef = likesCollection.document(likeId)
            val postRef = postsCollection.document(postId)
            
            // Usar transacción para garantizar atomicidad
            val isLiked = firestore.runTransaction { transaction ->
                val likeSnapshot = transaction.get(likeRef)
                val postSnapshot = transaction.get(postRef)
                
                val currentLikes = postSnapshot.getLong("likes") ?: 0
                
                if (likeSnapshot.exists()) {
                    // Ya existe el like, eliminarlo
                    transaction.delete(likeRef)
                    transaction.update(postRef, "likes", (currentLikes - 1).coerceAtLeast(0))
                    false
                } else {
                    // No existe el like, crearlo
                    val like = Like(
                        id = likeId,
                        postId = postId,
                        userId = userId,
                        timestamp = Timestamp.now()
                    )
                    transaction.set(likeRef, like)
                    transaction.update(postRef, "likes", currentLikes + 1)
                    true
                }
            }.await()
            
            // Trigger notification solo si se agregó el like
            if (isLiked) {
                triggerLikeNotification(postId, userId)
            }
            
            Result.success(isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun triggerLikeNotification(postId: String, fromUserId: String) {

        try {
            val postDoc = postsCollection.document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)

            
            val fromUser = userRepository.getUser(fromUserId)

            
            if (post != null && fromUser != null && post.userId != fromUserId) {

                
                val notification = Notification(
                    type = NotificationType.LIKE.value,
                    fromUserId = fromUserId,
                    fromUserName = fromUser.name,
                    fromUserProfilePic = fromUser.photoUrl ?: "",
                    toUserId = post.userId,
                    postId = postId,
                    postContent = post.content.take(50),
                    timestamp = Timestamp.now()
                )
                notificationRepository.createNotification(notification)

                
                // OneSignal Push Notification

                oneSignalService.sendLikeNotification(
                    toUserId = post.userId,
                    fromUserName = fromUser.name,
                    postId = postId
                )

            } else {

            }
        } catch (e: Exception) {
            android.util.Log.e("LikeRepository", "Error en triggerLikeNotification", e)
            e.printStackTrace()
        }
    }

    suspend fun getUsersWhoLiked(postId: String): List<String> {
        return try {
            android.util.Log.d("LikeRepository", "getUsersWhoLiked para postId: $postId")
            
            // Consulta 1: Formato estándar (postId)
            val snapshotStandard = likesCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
            
            // Consulta 2: Formato legado (b = postId)
            val snapshotLegacy = likesCollection
                .whereEqualTo("b", postId)
                .get()
                .await()

            val userIds = mutableSetOf<String>()
            
            // Extraer de formato estándar
            snapshotStandard.documents.forEach { doc ->
                doc.getString("userId")?.let { userIds.add(it) }
                // También chequear 'c' por si acaso está mezclado
                doc.getString("c")?.let { userIds.add(it) }
            }
            
            // Extraer de formato legado
            snapshotLegacy.documents.forEach { doc ->
                doc.getString("userId")?.let { userIds.add(it) }
                doc.getString("c")?.let { userIds.add(it) }
            }

            android.util.Log.d("LikeRepository", "Total IDs únicos encontrados: ${userIds.size}")
            userIds.toList()
        } catch (e: Exception) {
            android.util.Log.e("LikeRepository", "Error en getUsersWhoLiked", e)
            emptyList()
        }
    }

    /**
     * Obtiene todos los IDs de publicaciones que el usuario ha dado like
     * Optimización: Maneja formatos estándar y legados
     */
    suspend fun getAllUserLikes(userId: String): Set<String> {
        return try {
            // Consulta 1: Formato estándar (userId)
            val snapshotStandard = likesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Consulta 2: Formato legado (c = userId)
            val snapshotLegacy = likesCollection
                .whereEqualTo("c", userId)
                .get()
                .await()
            
            val postIds = mutableSetOf<String>()
            
            snapshotStandard.documents.forEach { doc ->
                doc.getString("postId")?.let { postIds.add(it) }
                doc.getString("b")?.let { postIds.add(it) }
            }
            
            snapshotLegacy.documents.forEach { doc ->
                doc.getString("postId")?.let { postIds.add(it) }
                doc.getString("b")?.let { postIds.add(it) }
            }

            postIds
        } catch (e: Exception) {
            android.util.Log.e("LikeRepository", "Error en getAllUserLikes", e)
            emptySet()
        }
    }

}
