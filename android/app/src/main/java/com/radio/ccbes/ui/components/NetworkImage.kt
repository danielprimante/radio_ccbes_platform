package com.radio.ccbes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.radio.ccbes.ui.theme.shimmerEffect
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    crossfade: Boolean = true,
    aspectRatio: Float? = null,
    minHeight: androidx.compose.ui.unit.Dp = 200.dp
) {
    val finalModifier = if (aspectRatio != null) {
        modifier.aspectRatio(aspectRatio)
    } else {
        modifier.heightIn(min = minHeight) // Asegura que haya un espacio mínimo para que el shimmer se vea
    }

    if (url.isNullOrBlank()) {
        PlaceholderImage(finalModifier, contentDescription)
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(crossfade)
                .size(Size.ORIGINAL) 
                .build(),
            contentDescription = contentDescription,
            modifier = finalModifier,
            contentScale = contentScale,
            error = {
                PlaceholderImage(Modifier.fillMaxSize(), contentDescription)
            },
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
            }
        )
    }
}

@Composable
private fun PlaceholderImage(
    modifier: Modifier = Modifier,
    contentDescription: String?
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = contentDescription,
            tint = Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
    }
}
