package com.crmind.agoratesting

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class ViewerActivity : AppCompatActivity() {
    private var mRtcEngine: RtcEngine? = null
    private var channelName: String? = null
    private var audioMuted = false
    private var isFullscreen = false

    // UI Elements
    private var remoteVideoContainer: FrameLayout? = null
    private var btnMuteAudio: Button? = null
    private var btnFullscreen: Button? = null
    private var btnLeaveChannel: Button? = null
    private var tvChannelInfo: TextView? = null
    private var tvConnectionStatus: TextView? = null
    private var tvViewerCount: TextView? = null
    private var tvStreamQuality: TextView? = null
    private var tvAudioIndicator: TextView? = null
    private var tvQualityIndicator: TextView? = null
    private var chatMessagesContainer: LinearLayout? = null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "✅ Joined channel as viewer - UID: $uid")
                tvConnectionStatus?.text = "Connected"
                tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_blue_light))
                Toast.makeText(this@ViewerActivity, "Connected! Waiting for stream...", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "🎥 Broadcaster connected: $uid")
                setupRemoteVideo(uid)
                tvConnectionStatus?.text = "🔴 LIVE"
                tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_red_light))
                addChatMessage("System", "Stream started! 🎉")
                Toast.makeText(this@ViewerActivity, "🔴 Stream is LIVE!", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.i(TAG, "📱 Broadcaster disconnected: $uid")
                remoteVideoContainer?.removeAllViews()
                tvConnectionStatus?.text = "Stream Ended"
                tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.darker_gray))
                addChatMessage("System", "Stream ended 👋")
                Toast.makeText(this@ViewerActivity, "Stream ended", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            runOnUiThread {
                when (state) {
                    Constants.REMOTE_AUDIO_STATE_STARTING -> {
                        tvAudioIndicator?.text = "🔊 Connecting..."
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_orange_light))
                    }
                    Constants.REMOTE_AUDIO_STATE_DECODING -> {
                        tvAudioIndicator?.text = "🔊 ON"
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_green_light))
                        addChatMessage("System", "Audio connected! 🎵")
                    }
                    Constants.REMOTE_AUDIO_STATE_STOPPED -> {
                        tvAudioIndicator?.text = "🔇 OFF"
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_red_light))
                    }
                    Constants.REMOTE_AUDIO_STATE_FROZEN -> {
                        tvAudioIndicator?.text = "🔊 Buffering..."
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_orange_light))
                    }
                }
            }
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            runOnUiThread {
                when (state) {
                    Constants.REMOTE_VIDEO_STATE_STARTING -> {
                        addChatMessage("System", "Video connecting... 📹")
                    }
//                    Constants.REMOTE_VIDEO_STATE_DECODING -> {
//                        addChatMessage("System", "Video connected! 📺")
//                    }
                    Constants.REMOTE_VIDEO_STATE_STOPPED -> {
                        addChatMessage("System", "Video stopped 📱")
                    }
                    Constants.REMOTE_VIDEO_STATE_FROZEN -> {
                        addChatMessage("System", "Video buffering... ⏳")
                    }
                }
            }
        }

        override fun onRtcStats(stats: RtcStats?) {
            runOnUiThread {
                stats?.let {
                    // Update quality indicator
                    val quality = when {
                        it.rxVideoKBitRate > 2000 -> "HD"
                        it.rxVideoKBitRate > 1000 -> "SD"
                        it.rxVideoKBitRate > 500 -> "LD"
                        else -> "Buffering"
                    }
                    tvStreamQuality?.text = "📶 $quality"
                    tvQualityIndicator?.text = quality

                    val color = when (quality) {
                        "HD" -> android.R.color.holo_green_light
                        "SD" -> android.R.color.holo_orange_light
                        else -> android.R.color.holo_red_light
                    }
                    tvQualityIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, color))
                }
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                Toast.makeText(this@ViewerActivity, "Left channel", Toast.LENGTH_SHORT).show()
            }
        }



        override fun onError(err: Int) {
            Log.e(TAG, "❌ Error: $err - ${RtcEngine.getErrorDescription(err)}")
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            runOnUiThread {
                when (state) {
                    Constants.CONNECTION_STATE_CONNECTING -> {
                        tvConnectionStatus?.text = "Connecting..."
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_orange_light))
                    }
                    Constants.CONNECTION_STATE_CONNECTED -> {
                        tvConnectionStatus?.text = "Connected"
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_blue_light))
                    }
                    Constants.CONNECTION_STATE_DISCONNECTED -> {
                        tvConnectionStatus?.text = "Disconnected"
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.darker_gray))
                    }
                    Constants.CONNECTION_STATE_FAILED -> {
                        tvConnectionStatus?.text = "Connection Failed"
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_red_light))
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Keep screen on during viewing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_viewer_layout)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        initViews()
        initializeAndJoinChannel()
        setupClickListeners()
        addWelcomeChatMessages()
    }

    private fun initViews() {
        remoteVideoContainer = findViewById(R.id.remote_video_view_container)
        btnMuteAudio = findViewById(R.id.btn_mute_audio)
        btnFullscreen = findViewById(R.id.btn_fullscreen)
        btnLeaveChannel = findViewById(R.id.btn_leave_channel)
        tvChannelInfo = findViewById(R.id.tv_channel_info_viewer)
        tvConnectionStatus = findViewById(R.id.tv_connection_status)
        tvViewerCount = findViewById(R.id.tv_viewer_count_viewer)
        tvStreamQuality = findViewById(R.id.tv_stream_quality_viewer)
        tvAudioIndicator = findViewById(R.id.tv_audio_indicator)
        tvQualityIndicator = findViewById(R.id.tv_quality_indicator)
        chatMessagesContainer = findViewById(R.id.chat_messages_container)

        tvChannelInfo?.text = channelName
        tvConnectionStatus?.text = "Connecting..."
        tvViewerCount?.text = "👁️ 1 viewer"
        tvStreamQuality?.text = "📶 HD"
        tvAudioIndicator?.text = "🔊 ON"
    }

    private fun initializeAndJoinChannel() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = baseContext
                mAppId = APP_ID
                mEventHandler = mRtcEventHandler
            }

            mRtcEngine = RtcEngine.create(config)

            mRtcEngine?.let { engine ->
                // Audio configuration
                engine.enableAudio()
                engine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO)
                engine.adjustPlaybackSignalVolume(400)
                engine.setDefaultAudioRoutetoSpeakerphone(true)
                engine.enableAudioVolumeIndication(200, 3, true)

                // Video configuration
                engine.enableVideo()

                // Channel configuration
                engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)

                // Join channel
                val options = ChannelMediaOptions().apply {
                    channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                    clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                    autoSubscribeAudio = true
                    autoSubscribeVideo = true
                }

                engine.joinChannel(null, channelName, 0, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Agora: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        remoteVideoContainer?.removeAllViews()
        remoteVideoContainer?.addView(surfaceView)

        val remoteVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        mRtcEngine?.setupRemoteVideo(remoteVideoCanvas)
    }

    private fun setupClickListeners() {
        btnMuteAudio?.setOnClickListener {
            audioMuted = !audioMuted
            mRtcEngine?.muteAllRemoteAudioStreams(audioMuted)
            btnMuteAudio?.text = if (audioMuted) "🔇 Unmute Audio" else "🔊 Mute Audio"
            tvAudioIndicator?.text = if (audioMuted) "🔇 MUTED" else "🔊 ON"

            val message = if (audioMuted) "Audio muted" else "Audio unmuted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        btnFullscreen?.setOnClickListener {
            // Toggle fullscreen (simplified version)
            isFullscreen = !isFullscreen
            if (isFullscreen) {
                // Hide system UI for fullscreen
                window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
                btnFullscreen?.text = "⛶ Exit Fullscreen"
            } else {
                // Show system UI
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                btnFullscreen?.text = "⛶ Fullscreen"
            }
        }

        btnLeaveChannel?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Leave Stream")
                .setMessage("Are you sure you want to leave this live stream?")
                .setPositiveButton("Leave") { _, _ ->
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun addWelcomeChatMessages() {
        addChatMessage("System", "Welcome to the live stream! 👋")
        addChatMessage("System", "Say hello in the comments! 💬")
    }

    private fun addChatMessage(username: String, message: String) {
        val chatMessage = TextView(this).apply {
            text = "👤 $username: $message"
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.white))
            setPadding(0, 4, 0, 4)
        }
        chatMessagesContainer?.addView(chatMessage)

        // Auto-scroll to bottom
        findViewById<android.widget.ScrollView>(R.id.chat_scroll_view)?.post {
            findViewById<android.widget.ScrollView>(R.id.chat_scroll_view)?.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRtcEngine?.let { engine ->
            engine.leaveChannel()
            RtcEngine.destroy()
        }
        mRtcEngine = null
    }

    companion object {
        private const val TAG = "ViewerActivity"
        // Replace with your actual Agora App ID
        private const val APP_ID = "981b297946924367814392114c9baed9"
    }
}