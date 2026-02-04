package com.radio.ccbes.util

import android.content.Context
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.data.cache.LinkMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class LinkMetadata(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val domain: String? = null,
    val youtubeVideoId: String? = null
)

object LinkParser {
    // Caché en memoria para evitar consultas a Room durante el scroll
    private val memoryCache = mutableMapOf<String, LinkMetadata>()

    // Regex consistente con el resaltador para detectar URLs con o sin http
    private val urlRegex = Regex(
        "(https?://[^\\s]+|www\\.[^\\s]+)"
    )
    
    private val youtubeRegex = Regex(
        "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})"
    )

    fun extractFirstUrl(text: String): String? {
        return urlRegex.find(text)?.value
    }

    suspend fun fetchMetadata(context: Context, url: String): LinkMetadata? = withContext(Dispatchers.IO) {
        val finalUrl = if (!url.startsWith("http")) "https://$url" else url
        
        // 0. Intentar cargar desde caché en memoria (Ultra rápido para Scroll)
        memoryCache[finalUrl]?.let { return@withContext it }

        val dao = AppDatabase.getDatabase(context).linkMetadataDao()
        
        // 1. Intentar cargar desde caché de base de datos
        try {
            val cached = dao.getMetadata(finalUrl)
            if (cached != null) {
                // Verificar si el caché es reciente (30 días para evitar parpadeos)
                if (System.currentTimeMillis() - cached.timestamp < 30L * 24 * 60 * 60 * 1000) {
                    val metadata = LinkMetadata(
                        url = cached.url,
                        title = cached.title,
                        description = cached.description,
                        imageUrl = cached.imageUrl,
                        domain = cached.domain,
                        youtubeVideoId = cached.youtubeVideoId
                    )
                    memoryCache[finalUrl] = metadata
                    return@withContext metadata
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Detectar YouTube antes de Scraping (para info rápida)
        val youtubeMatch = youtubeRegex.find(finalUrl)
        val ytVideoId = youtubeMatch?.groupValues?.get(1)

        // 3. Scraping con Jsoup si no hay caché o expiró
        try {
            val doc = Jsoup.connect(finalUrl)
                .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36") 
                .timeout(8000)
                .followRedirects(true)
                .get()

            val title = doc.select("meta[property=og:title]").attr("content").ifBlank {
                doc.select("meta[name=twitter:title]").attr("content").ifBlank { doc.title() }
            }
            
            val description = doc.select("meta[property=og:description]").attr("content").ifBlank {
                doc.select("meta[name=twitter:description]").attr("content").ifBlank {
                    doc.select("meta[name=description]").attr("content")
                }
            }
            
            var imageUrl = doc.select("meta[property=og:image]").attr("content").ifBlank {
                doc.select("meta[name=twitter:image]").attr("content").ifBlank {
                    doc.select("link[rel=image_src]").attr("href")
                }
            }

            // Si es YouTube y no hay imagen OG, usar la miniatura oficial
            if (ytVideoId != null && imageUrl.isBlank()) {
                imageUrl = "https://img.youtube.com/vi/$ytVideoId/maxresdefault.jpg"
            }
            
            val domain = try { java.net.URL(finalUrl).host?.removePrefix("www.") } catch (e: Exception) { null }

            val result = LinkMetadata(
                url = finalUrl,
                title = title.takeIf { it.isNotBlank() },
                description = description.takeIf { it.isNotBlank() },
                imageUrl = imageUrl.takeIf { it.isNotBlank() },
                domain = domain,
                youtubeVideoId = ytVideoId
            )

            // 4. Guardar en caché para la próxima vez
            dao.insertMetadata(
                LinkMetadataEntity(
                    url = result.url,
                    title = result.title,
                    description = result.description,
                    imageUrl = result.imageUrl,
                    domain = result.domain,
                    youtubeVideoId = result.youtubeVideoId
                )
            )
            
            memoryCache[finalUrl] = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback robusto: Si el scraping falla, devolvemos lo mínimo necesario para que la UI muestre la tarjeta
            val domain = try { java.net.URL(finalUrl).host?.removePrefix("www.") } catch (ex: Exception) { null }
            
            val result = when {
                ytVideoId != null -> LinkMetadata(
                    url = finalUrl,
                    imageUrl = "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg",
                    youtubeVideoId = ytVideoId,
                    domain = "youtube.com"
                )
                domain != null -> {
                    val isInsta = domain.contains("instagram") || domain.contains("instagr.am")
                    val isFace = domain.contains("facebook") || domain.contains("fb.me") || domain.contains("fb.com")
                    
                    LinkMetadata(
                        url = finalUrl,
                        domain = domain,
                        title = if (isInsta) "Instagram" else if (isFace) "Facebook" else domain.replaceFirstChar { it.uppercase() }
                    )
                }
                else -> null
            }
            
            if (result != null) memoryCache[finalUrl] = result
            return@withContext result
        }
    }
}
