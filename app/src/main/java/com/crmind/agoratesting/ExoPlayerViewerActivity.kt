package com.crmind.agoratesting

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util


class ExoPlayerViewerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var channelName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player_viewer)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        playerView = findViewById(R.id.player_view)

        initializePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                playerView?.player = exoPlayer

                // Your stream URLs - try different formats
                val streamUrls = listOf(
                    "rtmp://your-rtmp-server.com/live/$channelName",           // RTMP
                    "https://your-server.com/live/$channelName.m3u8",         // HLS
                    "https://your-server.com/live/$channelName.flv",          // HTTP-FLV
                    "https://your-server.com/live/$channelName/index.m3u8"    // Alternative HLS
                )

                // Try each URL until one works
                tryStreamUrls(exoPlayer, streamUrls, 0)
            }
    }

    private fun tryStreamUrls(player: ExoPlayer, urls: List<String>, index: Int) {
        if (index >= urls.size) {
            Toast.makeText(this, "No working stream found", Toast.LENGTH_LONG).show()
            return
        }

        val url = urls[index]
        Log.d(TAG, "Trying stream URL: $url")

        try {
            val mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(url)

            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Stream error for $url: ${error.message}")
                    // Try next URL
                    tryStreamUrls(player, urls, index + 1)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            Log.d(TAG, "Buffering stream...")
                            Toast.makeText(this@ExoPlayerViewerActivity, "Connecting to stream...", Toast.LENGTH_SHORT).show()
                        }
                        Player.STATE_READY -> {
                            Log.d(TAG, "Stream ready!")
                            Toast.makeText(this@ExoPlayerViewerActivity, "🔴 Stream is LIVE!", Toast.LENGTH_SHORT).show()
                        }
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Stream ended")
                            Toast.makeText(this@ExoPlayerViewerActivity, "Stream ended", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

        } catch (e: Exception) {
            Log.e(TAG, "Error with URL $url: ${e.message}")
            tryStreamUrls(player, urls, index + 1)
        }
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        player?.run {
            release()
        }
        player = null
    }

    companion object {
        private const val TAG = "ExoPlayerViewer"
    }
}