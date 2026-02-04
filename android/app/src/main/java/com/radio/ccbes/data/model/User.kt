package com.radio.ccbes.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

@Keep
data class User(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val handle: String = "",
    val photoUrl: String? = null,
    val bio: String =  "",
    @get:PropertyName("isBanned") val isBanned: Boolean = false,
    val fcmToken: String? = null,
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val role: String = "user",
    val pronouns: String = "",
    val gender: String = "",
    val link: String = "",
    val category: String = "",
    val city: String = "",
    val phone: String = "",
    val email: String = ""
)
