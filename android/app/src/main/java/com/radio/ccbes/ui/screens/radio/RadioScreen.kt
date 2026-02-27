package com.radio.ccbes.ui.screens.radio

import android.content.ComponentName
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import com.radio.ccbes.R
import com.radio.ccbes.data.service.RadioService
import com.radio.ccbes.ui.theme.RedAccent
import com.radio.ccbes.util.ScreenSizeUtils

@OptIn(UnstableApi::class)
@Composable
fun RadioScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: RadioViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val radioConfig by viewModel.radioConfig.collectAsState()
    val logoUrl by viewModel.logoUrl.collectAsState()
    val activeProgram by viewModel.activeProgram.collectAsState()

    val isTablet = ScreenSizeUtils.isTablet(windowSizeClass)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Determinar el nombre y la imagen a mostrar según si hay un programa activo
    val displayTitle = activeProgram?.name ?: radioConfig.title
    val displayImageUrl = activeProgram?.imageUrl?.takeIf { it.isNotEmpty() } ?: logoUrl

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, RadioService::class.java))
        val controllerFuture: ListenableFuture<MediaController> = MediaController.Builder(context, sessionToken).buildAsync()

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
        }

        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                isPlaying = controller.isPlaying
                controller.addListener(listener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            mediaController?.removeListener(listener)
            MediaController.releaseFuture(controllerFuture)
        }
    }

    if (isTablet && isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LogoImage(
                imageUrl = displayImageUrl,
                modifier = Modifier
                    .weight(0.5f)
                    .aspectRatio(1f)
                    .padding(end = 32.dp)
            )

            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Crossfade(
                    targetState = displayTitle,
                    animationSpec = tween(durationMillis = 500),
                    label = "titleCrossfade"
                ) { title ->
                    Text(
                        title ?: stringResource(R.string.radio_tagline),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    radioConfig.subtitle ?: stringResource(R.string.radio_live),
                    color = RedAccent,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(48.dp))
                PlayPauseButton(
                    isPlaying = isPlaying,
                    size = 80.dp,
                    iconSize = 40.dp,
                    onClick = {
                        mediaController?.let { controller ->
                            if (controller.isPlaying) controller.pause() else controller.play()
                        }
                    }
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(if (isTablet) 32.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            val logoSize = if (isTablet) 400.dp else 300.dp
            LogoImage(
                imageUrl = displayImageUrl,
                modifier = Modifier
                    .size(logoSize)
                    .background(Color.Transparent)
            )

            Spacer(modifier = Modifier.weight(1f))
            Crossfade(
                targetState = displayTitle,
                animationSpec = tween(durationMillis = 500),
                label = "titleCrossfadePortrait"
            ) { title ->
                Text(
                    title ?: stringResource(R.string.radio_tagline),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                radioConfig.subtitle ?: stringResource(R.string.radio_live),
                color = RedAccent,
                style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayPauseButton(
                    isPlaying = isPlaying,
                    size = if (isTablet) 80.dp else 64.dp,
                    iconSize = if (isTablet) 40.dp else 32.dp,
                    onClick = {
                        mediaController?.let { controller ->
                            if (controller.isPlaying) controller.pause() else controller.play()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

/**
 * Componente unificado para el logo.
 * Siempre usa AsyncImage pero con el drawable local como placeholder.
 * Si la URL está vacía, el placeholder queda visible (sin parpadeo).
 * Si hay una URL, carga la imagen remota con crossfade sobre el placeholder.
 */
@Composable
private fun LogoImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Crossfade(
        targetState = imageUrl,
        animationSpec = tween(durationMillis = 600),
        label = "logoCrossfade"
    ) { url ->
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url.ifEmpty { R.drawable.ic_logo_redondo })
                .crossfade(true)
                .build(),
            placeholder = painterResource(id = R.drawable.ic_logo_redondo),
            error = painterResource(id = R.drawable.ic_logo_redondo),
            fallback = painterResource(id = R.drawable.ic_logo_redondo),
            contentDescription = stringResource(R.string.logo_radio_desc),
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(RedAccent)
    ) {
        Icon(
            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            stringResource(R.string.play_pause),
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}