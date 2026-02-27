package com.radio.ccbes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.radio.ccbes.util.Constants.ONESIGNAL_APP_ID
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RadioApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Usa el 25% de la memoria disponible para la caché
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(512 * 1024 * 1024) // 512 MB de caché en disco
                    .build()
            }
            .respectCacheHeaders(false) // Forzar el uso de caché incluso si el servidor dice lo contrario
            .crossfade(true) // Transición suave entre el placeholder y la imagen final
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Crear canal de notificaciones (para compatibilidad)
        createNotificationChannel()

        // OneSignal Initialization
        OneSignal.Debug.logLevel = LogLevel.WARN
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
        
        // Request Permission for Android 13+
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)
        }

        // Manejador de clics en notificaciones de OneSignal (v5)
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data = event.notification.additionalData
                val postId = data?.optString("postId")
                val chatId = data?.optString("chatId")
                
                if (!postId.isNullOrEmpty() || !chatId.isNullOrEmpty()) {
                    val intent = Intent(this@RadioApplication, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        if (!postId.isNullOrEmpty()) putExtra("postId", postId)
                        if (!chatId.isNullOrEmpty()) putExtra("chatId", chatId)
                    }
                    startActivity(intent)
                }
            }
        })

        // Vincular usuario si ya está logueado
        FirebaseAuth.getInstance().currentUser?.let { user ->
            OneSignal.login(user.uid)
        }
    }

    /**
     * Crea un canal de notificaciones para Android O y versiones posteriores.
     * Esto es necesario para que las notificaciones de Firebase y OneSignal se agrupen correctamente.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "fcm_default_channel"
            val channelName = getString(R.string.notif_channel_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
