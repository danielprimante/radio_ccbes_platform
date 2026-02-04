package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.radio.ccbes.data.model.Notification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationsCollection = firestore.collection("notifications")

    /**
     * Get notifications for a specific user with real-time updates
     */
    fun getNotificationsForUser(userId: String): Flow<List<Notification>> = callbackFlow {
        val subscription = notificationsCollection
            .whereEqualTo("toUserId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    }
                    trySend(notifications)
                }
            }

        awaitClose { subscription.remove() }
    }

    /**
     * Create a new notification
     */
    suspend fun createNotification(notification: Notification) {
        try {
            // Don't notify if the user is acting on their own content
            if (notification.fromUserId == notification.toUserId) return
            
            notificationsCollection.add(notification).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String) {
        try {
            notificationsCollection.document(notificationId).update("isRead", true).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String) {
        try {
            val unread = notificationsCollection
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            val batch = firestore.batch()
            unread.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Delete all notifications for a user
     */
    suspend fun deleteAllNotifications(userId: String) {
        try {
            val allNotifications = notificationsCollection
                .whereEqualTo("toUserId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            allNotifications.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
