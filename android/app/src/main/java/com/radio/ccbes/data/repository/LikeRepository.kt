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
            val likeId = "${postId}_${userId}"
            val snapshot = likesCollection.document(likeId).get().await()
            snapshot.exists()
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
        android.util.Log.d("LikeRepository", "triggerLikeNotification iniciado - postId: $postId, fromUserId: $fromUserId")
        try {
            val postDoc = postsCollection.document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)
            android.util.Log.d("LikeRepository", "Post obtenido: ${post?.id}, userId: ${post?.userId}")
            
            val fromUser = userRepository.getUser(fromUserId)
            android.util.Log.d("LikeRepository", "Usuario obtenido: ${fromUser?.name}")
            
            if (post != null && fromUser != null && post.userId != fromUserId) {
                android.util.Log.d("LikeRepository", "Condiciones cumplidas, creando notificación...")
                
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
                android.util.Log.d("LikeRepository", "Notificación guardada en Firestore")
                
                // OneSignal Push Notification
                android.util.Log.d("LikeRepository", "Llamando a OneSignalService...")
                oneSignalService.sendLikeNotification(
                    toUserId = post.userId,
                    fromUserName = fromUser.name,
                    postId = postId
                )
                android.util.Log.d("LikeRepository", "OneSignalService.sendLikeNotification completado")
            } else {
                android.util.Log.w("LikeRepository", "Condiciones NO cumplidas - post: ${post != null}, fromUser: ${fromUser != null}, diferenteUsuario: ${post?.userId != fromUserId}")
            }
        } catch (e: Exception) {
            android.util.Log.e("LikeRepository", "Error en triggerLikeNotification", e)
            e.printStackTrace()
        }
    }

    suspend fun getUsersWhoLiked(postId: String): List<String> {
        return try {
            android.util.Log.d("LikeRepository", "getUsersWhoLiked para postId: $postId")
            val snapshot = likesCollection
                .whereEqualTo("postId", postId)
                .get()
                .await()
            android.util.Log.d("LikeRepository", "Documentos encontrados: ${snapshot.documents.size}")
            snapshot.documents.forEach { doc ->
                android.util.Log.d("LikeRepository", "Doc: ${doc.id}, data: ${doc.data}")
            }
            val userIds = snapshot.documents.mapNotNull { it.getString("userId") }
            android.util.Log.d("LikeRepository", "UserIds extraídos: $userIds")
            userIds
        } catch (e: Exception) {
            android.util.Log.e("LikeRepository", "Error en getUsersWhoLiked", e)
            emptyList()
        }
    }

    /**
     * Obtiene todos los IDs de publicaciones que el usuario ha dado like
     * Optimización: 1 consulta en lugar de N consultas individuales
     * @param userId ID del usuario
     * @return Set con los IDs de las publicaciones con like
     */
    suspend fun getAllUserLikes(userId: String): Set<String> {
        return try {
            val snapshot = likesCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { 
                it.getString("postId") 
            }.toSet()
        } catch (e: Exception) {
            android.util.Log.e("LikeRepository", "Error en getAllUserLikes", e)
            emptySet()
        }
    }

}
