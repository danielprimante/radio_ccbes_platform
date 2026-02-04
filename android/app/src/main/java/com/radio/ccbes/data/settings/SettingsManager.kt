package com.radio.ccbes.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val PUSH_NOTIFICATIONS_NEWS = booleanPreferencesKey("push_notifications_news")
        val PUSH_NOTIFICATIONS_SOCIAL = booleanPreferencesKey("push_notifications_social")
        val MEDIA_NOTIFICATION_ENABLED = booleanPreferencesKey("media_notification_enabled")
        val AUTO_PLAY_RADIO = booleanPreferencesKey("auto_play_radio")
        val PROFILE_BACKGROUND_GRADIENT_INDEX = intPreferencesKey("profile_background_gradient_index")
    }

    val pushNotificationsNews: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PUSH_NOTIFICATIONS_NEWS] ?: true
    }

    val pushNotificationsSocial: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PUSH_NOTIFICATIONS_SOCIAL] ?: true
    }

    val mediaNotificationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MEDIA_NOTIFICATION_ENABLED] ?: true
    }

    val autoPlayRadio: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PLAY_RADIO] ?: false
    }

    val profileBackgroundGradientIndex: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PROFILE_BACKGROUND_GRADIENT_INDEX] ?: 0
    }

    suspend fun setPushNotificationsNews(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PUSH_NOTIFICATIONS_NEWS] = enabled
        }
    }

    suspend fun setPushNotificationsSocial(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PUSH_NOTIFICATIONS_SOCIAL] = enabled
        }
    }

    suspend fun setMediaNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MEDIA_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setAutoPlayRadio(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_RADIO] = enabled
        }
    }

    suspend fun setProfileBackgroundGradientIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_BACKGROUND_GRADIENT_INDEX] = index
        }
    }
}
