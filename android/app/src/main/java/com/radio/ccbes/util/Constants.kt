package com.radio.ccbes.util

import com.radio.ccbes.BuildConfig

object Constants {
    // URL for the radio stream
    const val STREAM_URL = "https://streaming01.shockmedia.com.ar:10586/stream"
    
    // OneSignal Configuration para las notificaciones push
    const val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID
    const val ONESIGNAL_REST_API_KEY = BuildConfig.ONESIGNAL_REST_API_KEY
}
