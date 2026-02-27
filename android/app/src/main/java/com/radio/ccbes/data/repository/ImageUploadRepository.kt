package com.radio.ccbes.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import android.media.ExifInterface
import android.graphics.Matrix
import com.radio.ccbes.data.model.ImgBBData

class ImageUploadRepository {
    private val apiKey = "d7d8c8028977e32ee9fab67b251c6697"

    suspend fun uploadImage(context: Context, imageUri: Uri): Result<ImgBBData> = withContext(Dispatchers.IO) {
        try {
            // 1. Get image and handle orientation
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return@withContext Result.failure(Exception("Failed to decode bitmap"))

            // Handle Rotation
            val rotatedBitmap = handleRotation(context, imageUri, originalBitmap)

            val outputStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

            // 2. Prepare request
            val url = URL("https://api.imgbb.com/1/upload?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = "image=" + URLEncoder.encode(base64Image, "UTF-8")
            conn.outputStream.use { it.write(postData.toByteArray()) }

            // 3. Handle response
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.getJSONObject("data")
                val directUrl = data.getString("url")
                val imageId = data.getString("id")
                val deleteUrl = data.getString("delete_url")
                
                Result.success(ImgBBData(id = imageId, url = directUrl, deleteUrl = deleteUrl))
            } else {
                val error = conn.errorStream.bufferedReader().use { it.readText() }
                Log.e("Upload", "Error: $error")
                Result.failure(Exception("Upload failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e("Upload", "Exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteImage(deleteUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Parse deleteUrl: https://ibb.co/ID/HASH
            val regex = Regex("https://ibb\\.co/([a-zA-Z0-9]+)/([a-zA-Z0-9]+)")
            val match = regex.find(deleteUrl)
            
            if (match == null) {
                Log.e("DeleteImage", "Invalid delete URL format: $deleteUrl")
                // Just return success to not block other deletions if the URL is weird
                return@withContext Result.success(Unit) 
            }

            val (imageId, imageHash) = match.destructured

            val url = URL("https://ibb.co/json")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            // Implementation of multipart/form-data manually is complex, but the endpoint might accept url-encoded
            // based on the stackoverflow post which says "data encoded as multipart/form-data".
            // Let's try to construct a simple multipart body.
            
            val boundary = "Boundary-" + System.currentTimeMillis()
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val outputStream = conn.outputStream
            val writer = outputStream.writer()

            fun addPart(name: String, value: String) {
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                writer.append("$value\r\n")
            }

            addPart("action", "delete")
            addPart("delete", "image")
            addPart("from", "resource")
            addPart("deleting[id]", imageId)
            addPart("deleting[hash]", imageHash)
            
            writer.append("--$boundary--\r\n")
            writer.flush()
            writer.close()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                // Check if response indicates success if needed, but 200 is usually good
                Result.success(Unit)
            } else {
                val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e("DeleteImage", "Error deleting image: $error code: ${conn.responseCode}")
                Result.failure(Exception("Delete failed: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e("DeleteImage", "Exception during deletion", e)
            Result.failure(e)
        }
    }

    private fun handleRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
