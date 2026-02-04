package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.radio.ccbes.data.model.Comment
import com.radio.ccbes.data.model.Notification
import com.radio.ccbes.data.model.NotificationType
import com.radio.ccbes.data.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import android.util.Log
import com.radio.ccbes.data.service.OneSignalService

class CommentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val commentsCollection = firestore.collection("comments")
    private val postsCollection = firestore.collection("posts")
    private val notificationRepository = NotificationRepository()
    private val userRepository = UserRepository()
    private val oneSignalService = OneSignalService()

    fun getCommentsForPost(postId: String): Flow<List<Comment>> {
        return commentsCollection
            .whereEqualTo("postId", postId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Comment::class.java)
            }
    }

    suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val postRef = postsCollection.document(comment.postId)
                val postSnapshot = transaction.get(postRef)
                
                if (!postSnapshot.exists()) {
                    throw Exception("La publicación no existe")
                }
                
                val currentComments = postSnapshot.getLong("comments") ?: 0
                val commentRef = commentsCollection.document()
                transaction.set(commentRef, comment.copy(id = commentRef.id))
                transaction.update(postRef, "comments", currentComments + 1)
            }.await()
            
            // Trigger Notification
            triggerCommentNotification(comment)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun triggerCommentNotification(comment: Comment) {
        Log.d("CommentRepository", "triggerCommentNotification iniciado - postId: ${comment.postId}, userId: ${comment.userId}")
        try {
            val postDoc = postsCollection.document(comment.postId).get().await()
            val post = postDoc.toObject(Post::class.java)
            Log.d("CommentRepository", "Post obtenido: ${post?.id}, userId: ${post?.userId}")
            
            val fromUser = userRepository.getUser(comment.userId)
            Log.d("CommentRepository", "Usuario obtenido: ${fromUser?.name}")
            
            if (post != null && fromUser != null && post.userId != comment.userId) {
                Log.d("CommentRepository", "Condiciones cumplidas, creando notificación...")
                
                val notification = Notification(
                    type = NotificationType.COMMENT.value,
                    fromUserId = comment.userId,
                    fromUserName = fromUser.name,
                    fromUserProfilePic = fromUser.photoUrl ?: "",
                    toUserId = post.userId,
                    postId = comment.postId,
                    postContent = post.content.take(50),
                    timestamp = Timestamp.now()
                )
                notificationRepository.createNotification(notification)
                Log.d("CommentRepository", "Notificación guardada en Firestore")
                
                // OneSignal Push Notification
                Log.d("CommentRepository", "Llamando a OneSignalService...")
                oneSignalService.sendCommentNotification(
                    toUserId = post.userId,
                    fromUserName = fromUser.name,
                    postId = comment.postId
                )
                Log.d("CommentRepository", "OneSignalService.sendCommentNotification completado")
            } else {
                Log.w("CommentRepository", "Condiciones NO cumplidas - post: ${post != null}, fromUser: ${fromUser != null}, diferenteUsuario: ${post?.userId != comment.userId}")
            }
        } catch (e: Exception) {
            Log.e("CommentRepository", "Error en triggerCommentNotification", e)
            e.printStackTrace()
        }
    }

    suspend fun deleteComment(commentId: String, postId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val postRef = postsCollection.document(postId)
                val postSnapshot = transaction.get(postRef)
                val commentRef = commentsCollection.document(commentId)
                transaction.delete(commentRef)
                val currentComments = postSnapshot.getLong("comments") ?: 0
                if (currentComments > 0) {
                    transaction.update(postRef, "comments", currentComments - 1)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCommentLike(commentId: String, userId: String, postId: String): Result<Unit> {
        return try {
            val likeRef = firestore.collection("comment_likes")
                .document("${userId}_${commentId}")
            
            val likeDoc = likeRef.get().await()
            
            firestore.runTransaction { transaction ->
                val commentRef = commentsCollection.document(commentId)
                val commentSnapshot = transaction.get(commentRef)
                val currentLikes = commentSnapshot.getLong("likesCount") ?: 0
                
                if (likeDoc.exists()) {
                    transaction.delete(likeRef)
                    transaction.update(commentRef, "likesCount", (currentLikes - 1).coerceAtLeast(0))
                } else {
                    val likeData = mapOf(
                        "userId" to userId,
                        "commentId" to commentId,
                        "postId" to postId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                    transaction.set(likeRef, likeData)
                    transaction.update(commentRef, "likesCount", currentLikes + 1)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isCommentLikedByUser(commentId: String, userId: String): Flow<Boolean> {
        return firestore.collection("comment_likes")
            .document("${userId}_${commentId}")
            .snapshots()
            .map { it.exists() }
    }

    suspend fun updateComment(commentId: String, newContent: String): Result<Unit> {
        return try {
            commentsCollection.document(commentId)
                .update("content", newContent)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
