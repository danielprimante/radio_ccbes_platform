package com.radio.ccbes.data.service

import android.util.Log
import com.radio.ccbes.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OneSignalService {

    suspend fun sendNotification(
        toUserId: String,
        title: String,
        message: String,
        postId: String? = null,
        chatId: String? = null,
        userId: String? = null
    ) = withContext(Dispatchers.IO) {
        Log.d("OneSignalService", "Intentando enviar notificación a: $toUserId")
        Log.d("OneSignalService", "Título: $title, Mensaje: $message, PostId: $postId, ChatId: $chatId, UserId: $userId")
        
        if (Constants.ONESIGNAL_REST_API_KEY == "YOUR_REST_API_KEY_HERE") {
            Log.e("OneSignalService", "REST API Key no configurada. No se enviará la notificación.")
            return@withContext
        }

        try {
            val url = URL("https://onesignal.com/api/v1/notifications")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Key ${Constants.ONESIGNAL_REST_API_KEY}")
            connection.doOutput = true
            
            // Logs removed for security

            val jsonBody = JSONObject().apply {
                put("app_id", Constants.ONESIGNAL_APP_ID)
                put("include_external_user_ids", JSONArray(listOf(toUserId)))
                
                val contents = JSONObject().apply {
                    put("en", message)
                    put("es", message)
                }
                put("contents", contents)

                val headings = JSONObject().apply {
                    put("en", title)
                    put("es", title)
                }
                put("headings", headings)

                val data = JSONObject()
                if (postId != null) data.put("postId", postId)
                if (chatId != null) data.put("chatId", chatId)
                if (userId != null) data.put("userId", userId)
                
                if (data.length() > 0) {
                    put("data", data)
                }
            }

            Log.d("OneSignalService", "JSON Body: ${jsonBody.toString()}")

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().readText()
                Log.d("OneSignalService", "Notificación enviada exitosamente a $toUserId")
                Log.d("OneSignalService", "Respuesta: $responseBody")
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Sin detalles de error"
                Log.e("OneSignalService", "Error al enviar notificación: $responseCode - $errorStream")
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e("OneSignalService", "Error en OneSignalService", e)
        }
    }

    suspend fun sendLikeNotification(toUserId: String, fromUserName: String, postId: String) {
        sendNotification(
            toUserId = toUserId,
            title = "¡Nuevo Like!",
            message = "$fromUserName le ha dado like a tu publicación.",
            postId = postId
        )
    }

    suspend fun sendCommentNotification(toUserId: String, fromUserName: String, postId: String) {
        sendNotification(
            toUserId = toUserId,
            title = "Nuevo Comentario",
            message = "$fromUserName ha respondido a tu publicación.",
            postId = postId
        )
    }
    
    suspend fun sendFollowNotification(toUserId: String, fromUserName: String, fromUserId: String) {
        sendNotification(
            toUserId = toUserId,
            title = "Nuevo Seguidor",
            message = "$fromUserName comenzó a seguirte.",
            userId = fromUserId
        )
    }

    suspend fun sendMessageNotification(toUserId: String, fromUserName: String, chatId: String, messageText: String) {
        sendNotification(
            toUserId = toUserId,
            title = fromUserName,
            message = messageText,
            chatId = chatId
        )
    }
}
