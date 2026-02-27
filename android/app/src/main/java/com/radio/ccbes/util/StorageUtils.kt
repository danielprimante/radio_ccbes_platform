package com.radio.ccbes.util

import android.content.Context
import java.io.File
import java.util.Locale

object StorageUtils {

    fun getCacheSize(context: Context): String {
        var size: Long = 0
        size += getFolderSize(context.cacheDir)
        context.externalCacheDir?.let {
            size += getFolderSize(it)
        }
        return formatFileSize(size)
    }

    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        if (file.exists()) {
            val files = file.listFiles()
            if (files != null) {
                for (f in files) {
                    size += if (f.isDirectory) {
                        getFolderSize(f)
                    } else {
                        f.length()
                    }
                }
            }
        }
        return size
    }

    fun clearCache(context: Context) {
        deleteDir(context.cacheDir)
        context.externalCacheDir?.let {
            deleteDir(it)
        }
    }

    private fun deleteDir(file: File): Boolean {
        if (file.exists() && file.isDirectory) {
            val children = file.list()
            if (children != null) {
                for (child in children) {
                    val success = deleteDir(File(file, child))
                    if (!success) return false
                }
            }
            return file.delete()
        } else if (file.exists()) {
            return file.delete()
        }
        return false
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
