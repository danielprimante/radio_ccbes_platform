package com.radio.ccbes.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.radio.ccbes.data.model.Chat
import com.radio.ccbes.data.model.Message
import com.radio.ccbes.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection("chats")
    private val usersCollection = firestore.collection("users")
    private val notificationRepository = NotificationRepository()
    private val oneSignalService = com.radio.ccbes.data.service.OneSignalService()

    fun getChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val subscription = chatsCollection
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.toObjects(Chat::class.java).mapIndexed { index, chat ->
                        chat.copy(id = snapshot.documents[index].id)
                    }
                    trySend(chats)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val subscription = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(Message::class.java).mapIndexed { index, msg ->
                        msg.copy(id = snapshot.documents[index].id)
                    }
                    trySend(messages)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(chatId: String, senderId: String, content: String, type: String = "text", postId: String? = null) {
        val message = Message(
            senderId = senderId,
            content = content,
            type = type,
            timestamp = Timestamp.now(),
            postId = postId
        )
        
        // Add message to subcollection
        chatsCollection.document(chatId).collection("messages").add(message).await()
        
        // Update last message in chat document
        chatsCollection.document(chatId).update(
            mapOf(
                "lastMessage" to content,
                "lastMessageTimestamp" to message.timestamp,
                "lastMessageSenderId" to senderId
            )
        ).await()

        // Trigger notifications
        try {
            val chatDoc = chatsCollection.document(chatId).get().await()
            val chat = chatDoc.toObject(Chat::class.java)
            val recipientId = chat?.participants?.find { it != senderId }
            
            if (recipientId != null) {
                val senderName = chat.participantNames[senderId] ?: "Usuario"
                val senderPhoto = chat.participantPhotos[senderId] ?: ""
                
                val notification = com.radio.ccbes.data.model.Notification(
                    type = com.radio.ccbes.data.model.NotificationType.MESSAGE.value,
                    fromUserId = senderId,
                    fromUserName = senderName,
                    fromUserProfilePic = senderPhoto,
                    toUserId = recipientId,
                    chatId = chatId,
                    postContent = if (type == "image") "Sent an image" else content,
                    timestamp = message.timestamp ?: Timestamp.now()
                )
                
                notificationRepository.createNotification(notification)
                
                oneSignalService.sendMessageNotification(
                    toUserId = recipientId,
                    fromUserName = senderName,
                    chatId = chatId,
                    messageText = if (type == "image") "📷 Imagen" else content
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): String {
        // Try to find existing chat
        val existingChat = chatsCollection
            .whereArrayContains("participants", currentUserId)
            .get()
            .await()
            .documents
            .find { doc ->
                val participants = doc.get("participants") as? List<*>
                participants?.contains(otherUserId) == true
            }

        if (existingChat != null) {
            return existingChat.id
        }

        // Create new chat
        val currentUser = usersCollection.document(currentUserId).get().await().toObject(User::class.java)
        val otherUser = usersCollection.document(otherUserId).get().await().toObject(User::class.java)

        val newChat = hashMapOf(
            "participants" to listOf(currentUserId, otherUserId),
            "participantNames" to mapOf(
                currentUserId to (currentUser?.name ?: ""),
                otherUserId to (otherUser?.name ?: "")
            ),
            "participantPhotos" to mapOf(
                currentUserId to (currentUser?.photoUrl ?: ""),
                otherUserId to (otherUser?.photoUrl ?: "")
            ),
            "participantHandles" to mapOf(
                currentUserId to (currentUser?.handle ?: ""),
                otherUserId to (otherUser?.handle ?: "")
            ),
            "lastMessage" to "",
            "lastMessageTimestamp" to Timestamp.now(),
            "lastMessageSenderId" to ""
        )

        val docRef = chatsCollection.add(newChat).await()
        return docRef.id
    }

    suspend fun deleteChat(chatId: String) {
        // Delete all messages first
        val messages = chatsCollection.document(chatId).collection("messages").get().await()
        val batch = firestore.batch()
        messages.documents.forEach { batch.delete(it.reference) }
        batch.delete(chatsCollection.document(chatId))
        batch.commit().await()
    }

    suspend fun reportUser(senderId: String, reportedUserId: String, reason: String) {
        val report = hashMapOf(
            "reportedBy" to senderId,
            "reportedUser" to reportedUserId,
            "reason" to reason,
            "timestamp" to Timestamp.now()
        )
        firestore.collection("user_reports").add(report).await()
    }

    suspend fun blockUser(currentUserId: String, blockedUserId: String) {
        firestore.collection("users").document(currentUserId)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(blockedUserId))
            .await()
    }
    
    suspend fun editMessage(chatId: String, messageId: String, newContent: String) {
        try {
            val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
            
            // Update message content and mark as edited
            messageRef.update(
                mapOf(
                    "content" to newContent,
                    "isEdited" to true
                )
            ).await()
            
            // Check if this is the last message and update chat preview
            val chatDoc = chatsCollection.document(chatId).get().await()
            val chat = chatDoc.toObject(Chat::class.java)
            
            // Get all messages to find the last one
            val messages = chatsCollection.document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (!messages.isEmpty) {
                val lastMessage = messages.documents[0]
                if (lastMessage.id == messageId) {
                    // Update last message in chat document
                    chatsCollection.document(chatId).update(
                        "lastMessage", newContent
                    ).await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error editing message", e)
            throw e
        }
    }
    
    suspend fun deleteMessage(chatId: String, messageId: String) {
        try {
            val messageRef = chatsCollection.document(chatId).collection("messages").document(messageId)
            
            // Get message before deleting to check if it's the last one
            val messageDoc = messageRef.get().await()
            val message = messageDoc.toObject(Message::class.java)
            
            // Delete the message
            messageRef.delete().await()
            
            // Update last message in chat if this was the last message
            val messages = chatsCollection.document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (!messages.isEmpty) {
                val lastMessage = messages.documents[0].toObject(Message::class.java)
                if (lastMessage != null) {
                    chatsCollection.document(chatId).update(
                        mapOf(
                            "lastMessage" to if (lastMessage.type == "image") "📷 Imagen" else lastMessage.content,
                            "lastMessageTimestamp" to lastMessage.timestamp,
                            "lastMessageSenderId" to lastMessage.senderId
                        )
                    ).await()
                }
            } else {
                // No messages left, clear last message
                chatsCollection.document(chatId).update(
                    mapOf(
                        "lastMessage" to "",
                        "lastMessageTimestamp" to Timestamp.now(),
                        "lastMessageSenderId" to ""
                    )
                ).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error deleting message", e)
            throw e
        }
    }
}
