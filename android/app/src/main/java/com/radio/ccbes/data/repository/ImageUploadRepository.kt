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

class ImageUploadRepository {
    private val apiKey = "d7d8c8028977e32ee9fab67b251c6697"

    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
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
                val directUrl = json.getJSONObject("data").getString("url")
                Result.success(directUrl)
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
