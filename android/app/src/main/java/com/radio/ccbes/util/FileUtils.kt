package com.radio.ccbes.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    fun createImageUri(context: Context): Uri {
        val directory = File(context.cacheDir, "images")
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, "temp_image_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
