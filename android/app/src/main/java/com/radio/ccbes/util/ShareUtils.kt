package com.radio.ccbes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    data class PostShareData(
        val userName: String,
        val userHandle: String,
        val content: String,
        val userPhotoUrl: String?,
        val imageUrl: String?, // Can be null if text-only post
        val postId: String
    )

    suspend fun generateShareImage(context: Context, data: PostShareData): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Configuración de dimensiones (estilo Instagram Story/Post Card)
                // Usamos un ancho fijo de alta calidad para compartir
                val width = 1080 
                // La altura será dinámica dependiento del contenido, pero iniciamos base
                // Header (150px) + Image (1080px) + Footer (variable)
                
                // Cargar imágenes
                val avatarBitmap = loadBitmap(context, data.userPhotoUrl)
                val postImageBitmap = loadBitmap(context, data.imageUrl)

                // Calcular altura de la imagen del post
                val postImageHeight = if (postImageBitmap != null) {
                   (width.toFloat() / postImageBitmap.width.toFloat() * postImageBitmap.height.toFloat()).toInt()
                } else {
                    // Si no hay imagen, un cuadro de texto (aspecto 1:1 o 4:5)
                    1080 
                }

                // Calcular altura del footer (texto)
                val footerPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }
                
                // Formato Footer: "nombre_usuario  contenido..."
                // Medimos el texto para saber cuánto ocupa
                val footerText = "${data.userName} ${data.content}"
                val footerStaticLayout = StaticLayout.Builder.obtain(
                    footerText, 0, footerText.length, footerPaint, width - 80
                ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .build()
                
                val footerHeight = footerStaticLayout.height + 120 // Padding extra

                // Altura Total
                val headerHeight = 180
                val totalHeight = headerHeight + postImageHeight + footerHeight

                // 2. Crear Bitmap y Canvas
                val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                
                // Fondo Blanco
                canvas.drawColor(Color.WHITE)

                // 3. Dibujar Header
                // Avatar
                val avatarSize = 100
                val avatarMargin = 40
                
                if (avatarBitmap != null) {
                    val scaledAvatar = getCircularBitmap(scaleBitmap(avatarBitmap, avatarSize, avatarSize))
                    canvas.drawBitmap(scaledAvatar, avatarMargin.toFloat(), (headerHeight - avatarSize) / 2f, null)
                } else {
                    // Placeholder avatar
                    val paint = Paint().apply { color = Color.LTGRAY; isAntiAlias = true }
                    canvas.drawCircle(avatarMargin.toFloat() + avatarSize/2, headerHeight/2f, avatarSize/2f, paint)
                }

                // Nombre
                val namePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 48f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                // Centrado verticalmente con el avatar
                val textX = avatarMargin + avatarSize + 30f
                val textY = headerHeight / 2f - (namePaint.descent() + namePaint.ascent()) / 2f
                canvas.drawText(data.userName, textX, textY, namePaint)

                // 4. Dibujar Contenido (Imagen Central)
                val contentTop = headerHeight.toFloat()
                
                if (postImageBitmap != null) {
                    val destRect = RectF(0f, contentTop, width.toFloat(), contentTop + postImageHeight)
                    canvas.drawBitmap(postImageBitmap, null, destRect, null)
                } else {
                    // Si es solo texto, dibujar un fondo bonito y el texto grande
                    val bgPaint = Paint().apply { color = Color.parseColor("#F0F0F0") } // Gris muy claro
                    canvas.drawRect(0f, contentTop, width.toFloat(), contentTop + postImageHeight, bgPaint)
                    
                    // Texto central grande (Quote style)
                    val quotePaint = TextPaint().apply {
                        color = Color.BLACK
                        textSize = 60f
                        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    
                    val quoteLayout = StaticLayout.Builder.obtain(
                        data.content, 0, data.content.length, quotePaint, width - 200
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                    
                    canvas.save()
                    canvas.translate(width/2f, contentTop + (postImageHeight - quoteLayout.height)/2f)
                    quoteLayout.draw(canvas)
                    canvas.restore()
                }

                // 5. Dibujar Footer
                val footerTop = contentTop + postImageHeight + 40 // +40 padding top
                canvas.save()
                canvas.translate(40f, footerTop) // 40px padding left
                
                // Dibujar Username y Caption
                canvas.drawText(data.userName, 0f, 0f, namePaint.apply { textSize = 42f })
                
                // Caption debajo
                val captionPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }
                
                val captionLayout = StaticLayout.Builder.obtain(
                    data.content, 0, data.content.length, captionPaint, width - 80
                ).build()
                 
                canvas.translate(0f, 60f) // Bajamos una linea
                captionLayout.draw(canvas)
                
                canvas.restore()

                // 6. Watermark / Logo Pequeño (Opcional, para branding)
                // ...

                // 7. Guardar imagen
                val imagesFolder = File(context.cacheDir, "images")
                imagesFolder.mkdirs()
                val file = File(imagesFolder, "shared_post_${System.currentTimeMillis()}.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                stream.flush()
                stream.close()

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun loadBitmap(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // Importante para Canvas
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}
