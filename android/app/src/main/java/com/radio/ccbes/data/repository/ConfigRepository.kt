package com.radio.ccbes.data.repository

import androidx.annotation.Keep
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.radio.ccbes.data.model.Program
import com.radio.ccbes.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ConfigRepository {
    private val db = Firebase.firestore
    private val settingsCollection = db.collection("settings")

    suspend fun getStreamUrl(): String {
        return try {
            val document = settingsCollection.document("radio").get().await()
            if (document.exists()) {
                document.getString("streamUrl") ?: Constants.STREAM_URL
            } else {
                Constants.STREAM_URL
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Constants.STREAM_URL
        }
    }

    suspend fun getRadioConfig(): RadioConfig {
        return try {
            val document = settingsCollection.document("radio").get().await()
            if (document.exists()) {
                RadioConfig(
                    streamUrl = document.getString("streamUrl") ?: Constants.STREAM_URL,
                    title = document.getString("title"),
                    subtitle = document.getString("subtitle")
                )
            } else {
                RadioConfig(streamUrl = Constants.STREAM_URL)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            RadioConfig(streamUrl = Constants.STREAM_URL)
        }
    }

    suspend fun getAboutConfig(): AboutConfig {
        return try {
            val document = settingsCollection.document("about").get().await()
            if (document.exists()) {
                document.toObject(AboutConfig::class.java) ?: AboutConfig()
            } else {
                AboutConfig()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AboutConfig()
        }
    }

    /**
     * Retorna el programa activo actualmente (isActive = true).
     * Si no hay ningún programa activo, retorna null (se usará el logo/nombre por defecto).
     */
    suspend fun getActiveProgram(): Program? {
        return try {
            val snapshot = db.collection("programs")
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(Program::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Observa en tiempo real el programa activo (isActive = true).
     * Emite un nuevo valor cada vez que Firestore detecta un cambio,
     * sin necesidad de recargar la UI.
     */
    fun observeActiveProgram(): Flow<Program?> = callbackFlow {
        val listenerRegistration = db.collection("programs")
            .whereEqualTo("isActive", true)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val program = snapshot?.documents
                    ?.firstOrNull()
                    ?.toObject(Program::class.java)
                trySend(program)
            }
        awaitClose { listenerRegistration.remove() }
    }
}

@Keep
data class RadioConfig(
    val streamUrl: String = Constants.STREAM_URL,
    val title: String? = null,
    val subtitle: String? = null
)

@Keep
data class AboutConfig(
    val logoUrl: String = "",
    val churchName: String = "",
    val subText: String = "",
    val location: String = "",
    val email: String = "",
    val phone: String = "",
    val facebookUrl: String = "",
    val instagramUrl: String = "",
    val youtubeUrl: String = ""
)
