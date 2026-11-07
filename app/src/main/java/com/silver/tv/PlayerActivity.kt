package com.silver.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private const val DEFAULT_UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

@androidx.annotation.OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var index by mutableIntStateOf(0)
    private var status by mutableStateOf<String?>(null)
    private var buffering by mutableStateOf(true)

    private val list: List<Channel> get() = ChannelRepository.playlist

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (list.isEmpty()) {
            finish()
            return
        }
        index = (savedInstanceState?.getInt("index") ?: intent.getIntExtra("index", 0))
            .coerceIn(list.indices)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) status = null
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                        // Ao vivo: ficou para trás da transmissão, volta para o ponto atual.
                        seekToDefaultPosition()
                        prepare()
                        return
                    }
                    buffering = false
                    status = "Este canal não está respondendo agora."
                }
            })
        }
        playCurrent()

        setContent { SilverTheme { PlayerScreen() } }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("index", index)
    }

    private fun playCurrent() {
        val p = player ?: return
        val c = list[index]
        status = null
        buffering = true

        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setUserAgent(c.userAgent ?: DEFAULT_UA)
        c.referrer?.let { http.setDefaultRequestProperties(mapOf("Referer" to it)) }

        val item = MediaItem.Builder()
            .setUri(c.url)
            .apply { if (c.url.contains(".m3u8", ignoreCase = true)) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()

        val source = DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http))
            .createMediaSource(item)
        p.setMediaSource(source)
        p.prepare()
        p.playWhenReady = true
    }

    private fun step(delta: Int) {
        if (list.isEmpty()) return
        index = (index + delta).mod(list.size)
        playCurrent()
    }

    override fun onStart() {
        super.onStart()
        player?.play()
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    @Composable
    private fun PlayerScreen() {
        var showUi by remember { mutableStateOf(true) }
        LaunchedEffect(showUi, index) {
            if (showUi) {
                delay(4000)
                showUi = false
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        this.player = this@PlayerActivity.player
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Camada de gestos: toque mostra/esconde, deslizar troca de canal.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showUi = !showUi })
                    }
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if (total < -150f) step(1) else if (total > 150f) step(-1)
                                showUi = true
                            },
                        ) { _, amount -> total += amount }
                    }
            )

            if (buffering && status == null) {
                CircularProgressIndicator(
                    color = Silver.Mid,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            status?.let { msg ->
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(Silver.Card.copy(alpha = 0.92f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(msg, color = Silver.Light, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Alguns canais da lista ficam fora do ar ou bloqueados por região.",
                        color = Silver.Dim,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { playCurrent() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Silver.Bg, contentColor = Silver.Light,
                            ),
                        ) { Text("Tentar de novo") }
                        Button(
                            onClick = { step(1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Silver.Mid, contentColor = Silver.Bg,
                            ),
                        ) { Text("Próximo canal") }
                    }
                }
            }

            AnimatedVisibility(
                visible = showUi,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(Modifier.fillMaxSize()) {
                    val c = list.getOrNull(index)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent))
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Silver.Light)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                c?.name ?: "",
                                color = Silver.Light,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${index + 1} de ${list.size} · deslize para trocar de canal",
                                color = Silver.Mid,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    RoundButton(
                        onClick = { step(-1) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp),
                    ) { Icon(Icons.Filled.SkipPrevious, "Canal anterior", tint = Silver.Light) }
                    RoundButton(
                        onClick = { step(1) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp),
                    ) { Icon(Icons.Filled.SkipNext, "Próximo canal", tint = Silver.Light) }
                }
            }
        }
    }

    @Composable
    private fun RoundButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Box(
            modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick) { content() }
        }
    }
}
