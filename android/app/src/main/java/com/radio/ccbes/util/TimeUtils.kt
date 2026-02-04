package com.radio.ccbes.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

object TimeUtils {
    fun formatTimeAgo(timestamp: Timestamp): String {
        val now: Long = System.currentTimeMillis()
        val time: Long = timestamp.toDate().time
        val diff: Long = now - time
        
        val seconds = diff / 1000L
        val minutes = seconds / 60L
        val hours = minutes / 60L
        val days = hours / 24L
        
        return when {
            days >= 1L -> "Hace ${days}d"
            hours >= 1L -> "Hace ${hours}h"
            minutes >= 1L -> "Hace ${minutes}m"
            else -> "Ahora"
        }
    }

    fun formatToTime(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}
