package com.radio.ccbes.data.repository

import androidx.annotation.Keep
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.radio.ccbes.util.Constants
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
