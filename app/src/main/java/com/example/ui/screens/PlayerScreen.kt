package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.PlayerRequest
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    request: PlayerRequest,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isLive by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableLongStateOf(request.startPositionMs) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }

    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "متناسب",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "پرکردن صفحه",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "کشیده",
    )

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(request.streamUrl) {
        playbackError = null
        isBuffering = true
        val mediaItem = MediaItem.fromUri(request.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        if (request.startPositionMs > 0L) {
            exoPlayer.seekTo(request.startPositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isLive = exoPlayer.isCurrentMediaItemLive
                if (exoPlayer.duration > 0L) {
                    durationMs = exoPlayer.duration
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = "پخش این لینک ممکن نشد (${error.errorCodeName}). ممکن است لینک منقضی شده یا نیاز به باز کردن در مرورگر داشته باشد."
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            val finalPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val finalDur = exoPlayer.duration.coerceAtLeast(0L)
            onSaveProgress(finalPos, finalDur)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
                onSaveProgress(exoPlayer.currentPosition.coerceAtLeast(0L), exoPlayer.duration.coerceAtLeast(0L))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Update system bars & orientation on fullscreen toggle
    DisposableEffect(isFullscreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Periodic progress ticker
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (exoPlayer.duration > 0L) durationMs = exoPlayer.duration
            isLive = exoPlayer.isCurrentMediaItemLive
            delay(500L)
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                showControls = !showControls
            }
            .testTag("player_screen"),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = resizeModes[resizeModeIndex].first
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeModes[resizeModeIndex].first
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = CinemaGold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (playbackError != null) {
            Surface(
                color = ObsidianSurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "خطا در پخش ویدیو",
                        style = MaterialTheme.typography.titleMedium,
                        color = LiveCrimson,
                    )
                    Text(
                        text = playbackError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onClose) { Text("بستن") }
                        Button(
                            onClick = {
                                playbackError = null
                                isBuffering = true
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CinemaGold,
                                contentColor = ObsidianBg,
                            ),
                        ) {
                            Text("تلاش مجدد")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .systemBarsPadding()
                    .padding(14.dp),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = request.sourceName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isLive) {
                                Surface(
                                    color = LiveCrimson,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = "پخش زنده",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_favorite_button"),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "علاقه‌مندی",
                            tint = if (isFavorite) LiveCrimson else Color.White,
                        )
                    }
                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_fullscreen_button"),
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                            contentDescription = "تمام صفحه",
                            tint = Color.White,
                        )
                    }
                }

                // Center Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "۱۰ ثانیه قبل",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CinemaGold)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "توقف" else "پخش",
                            tint = ObsidianBg,
                            modifier = Modifier.size(38.dp),
                        )
                    }

                    IconButton(
                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "۱۰ ثانیه بعد",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                // Bottom Seekbar & Playback Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    if (!isLive && durationMs > 0L) {
                        Slider(
                            value = positionMs.coerceIn(0L, durationMs).toFloat(),
                            onValueChange = { newPos ->
                                positionMs = newPos.toLong()
                                exoPlayer.seekTo(positionMs)
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = CinemaGold,
                                activeTrackColor = CinemaGold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (isLive) "LIVE" else "${formatTime(positionMs)} / ${formatTime(durationMs)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = OutfitFontFamily),
                            color = Color.White,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
                                    val next = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
                                    playbackSpeed = next
                                    exoPlayer.playbackParameters = PlaybackParameters(next)
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.Speed, contentDescription = null, tint = CinemaGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${playbackSpeed}x", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                            }
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.AspectRatio, contentDescription = null, tint = CinemaGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(resizeModes[resizeModeIndex].second, style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
