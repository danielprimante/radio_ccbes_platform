package com.radio.ccbes.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.radio.ccbes.data.model.Follow
import com.radio.ccbes.data.model.Notification
import com.radio.ccbes.data.model.NotificationType
import com.radio.ccbes.data.service.OneSignalService
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FollowRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val followsCollection = firestore.collection("follows")
    private val notificationRepository = NotificationRepository()
    private val userRepository = UserRepository()
    private val oneSignalService = OneSignalService()

    suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        if (followerId == followingId) return Result.failure(Exception("Cannot follow yourself"))
        
        return try {
            val existing = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .get()
                .await()

            if (existing.isEmpty) {
                val follow = Follow(
                    followerId = followerId,
                    followingId = followingId
                )
                followsCollection.add(follow).await()
                
                // Trigger notification for new follower
                triggerFollowNotification(followerId, followingId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun triggerFollowNotification(followerId: String, followingId: String) {
        try {
            val followerUser = userRepository.getUser(followerId)
            
            if (followerUser != null) {
                // Create notification in Firestore
                val notification = Notification(
                    type = NotificationType.FOLLOW.value,
                    fromUserId = followerId,
                    fromUserName = followerUser.name,
                    fromUserProfilePic = followerUser.photoUrl ?: "",
                    toUserId = followingId,
                    timestamp = Timestamp.now()
                )
                notificationRepository.createNotification(notification)
                
                // Send OneSignal push notification
                oneSignalService.sendFollowNotification(
                    toUserId = followingId,
                    fromUserName = followerUser.name
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FollowRepository", "Error sending follow notification", e)
            e.printStackTrace()
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            val query = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .get()
                .await()
            
            for (doc in query.documents) {
                followsCollection.document(doc.id).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        return try {
            val query = followsCollection
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .get()
                .await()
            !query.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowersCount(userId: String): Int {
        return try {
            val query = followsCollection
                .whereEqualTo("followingId", userId)
                .get()
                .await()
            query.size()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getFollowingCount(userId: String): Int {
        return try {
            val query = followsCollection
                .whereEqualTo("followerId", userId)
                .get()
                .await()
            query.size()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getFollowerIds(userId: String): List<String> {
        return try {
            val query = followsCollection
                .whereEqualTo("followingId", userId)
                .get()
                .await()
            query.documents.mapNotNull { it.getString("followerId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowingIds(userId: String): List<String> {
        return try {
            val query = followsCollection
                .whereEqualTo("followerId", userId)
                .get()
                .await()
            query.documents.mapNotNull { it.getString("followingId") }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
