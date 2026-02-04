package com.radio.ccbes.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.radio.ccbes.ui.theme.shimmerEffect
import com.radio.ccbes.util.LinkMetadata
import com.radio.ccbes.util.LinkParser

@Composable
fun LinkPreviewCard(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var metadata by remember(url) { mutableStateOf<LinkMetadata?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }

    LaunchedEffect(url) {
        isLoading = true
        metadata = LinkParser.fetchMetadata(context, url)
        isLoading = false
    }

    if (isLoading) {
        LinkPreviewSkeleton(modifier)
    } else if (metadata != null) {
        val domain = metadata?.domain?.lowercase() ?: ""
        val isYouTube = metadata?.youtubeVideoId != null || domain.contains("youtube") || domain.contains("youtu.be")
        val isInstagram = domain.contains("instagram") || domain.contains("instagr.am")
        val isFacebook = domain.contains("facebook") || domain.contains("fb.me") || domain.contains("fb.com")

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(metadata!!.url))
                    context.startActivity(intent)
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Column {
                if (isYouTube && metadata!!.imageUrl != null) {
                    // YouTube Premium View with Play Button Overlay
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        AsyncImage(
                            model = metadata!!.imageUrl,
                            contentDescription = "YouTube Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Play button overlay
                        Surface(
                            modifier = Modifier.align(Alignment.Center).size(56.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.padding(12.dp).size(32.dp)
                            )
                        }
                        // Youtube Label
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color.Red,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "YOUTUBE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else if (metadata!!.imageUrl != null) {
                    AsyncImage(
                        model = metadata!!.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback for Missing Images (Social Branded)
                    val backgroundBrush = when {
                        isInstagram -> Brush.verticalGradient(
                            colors = listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                        )
                        isFacebook -> Brush.verticalGradient(
                            colors = listOf(Color(0xFF1877F2), Color(0xFF0C59CF))
                        )
                        else -> Brush.verticalGradient(
                            colors = listOf(Color(0xFFF0F0F0), Color(0xFFE0E0E0))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(backgroundBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                null,
                                tint = if (isInstagram || isFacebook) Color.White else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            if (isInstagram || isFacebook) {
                                Text(
                                    if (isInstagram) "VER EN INSTAGRAM" else "VER EN FACEBOOK",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(12.dp)) {
                    if (metadata!!.domain != null) {
                        Text(
                            text = metadata!!.domain!!.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isYouTube) Color.Red else Color.Gray,
                            letterSpacing = 1.sp,
                            fontWeight = if (isYouTube) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    Text(
                        text = metadata!!.title ?: (if (isYouTube) "Video de YouTube" else metadata!!.url),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black
                    )
                    
                    if (metadata!!.description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metadata!!.description!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LinkPreviewSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .shimmerEffect()
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}
