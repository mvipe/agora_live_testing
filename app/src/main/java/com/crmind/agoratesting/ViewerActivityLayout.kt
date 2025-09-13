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
                Log.i(TAG, "✅ ==== JOINED CHANNEL SUCCESS ====")
                Log.i(TAG, "✅ Channel: $channel")
                Log.i(TAG, "✅ My UID: $uid")
                Log.i(TAG, "✅ Elapsed: $elapsed ms")
                Log.i(TAG, "✅ Role: AUDIENCE")
                Log.i(TAG, "✅ Container exists: ${remoteVideoContainer != null}")
                Log.i(TAG, "✅ Container child count: ${remoteVideoContainer?.childCount}")
                Log.i(TAG, "✅ ==== END JOIN SUCCESS ====")

                tvConnectionStatus?.text = "Connected"
                tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_blue_light))
                Toast.makeText(this@ViewerActivity, "Connected! Waiting for stream...", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "🎥 ==== USER JOINED ====")
                Log.i(TAG, "🎥 Broadcaster UID: $uid")
                Log.i(TAG, "🎥 Elapsed: $elapsed ms")
                Log.i(TAG, "🎥 Expected UID: 1")
                Log.i(TAG, "🎥 UID Match: ${uid == 1}")
                Log.i(TAG, "🎥 Container before setup: ${remoteVideoContainer?.childCount} children")
                Log.i(TAG, "🎥 About to call setupRemoteVideo...")

                setupRemoteVideo(uid)

                Log.i(TAG, "🎥 Container after setup: ${remoteVideoContainer?.childCount} children")
                Log.i(TAG, "🎥 ==== END USER JOINED ====")

                tvConnectionStatus?.text = "🔴 LIVE"
                tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_red_light))
                addChatMessage("System", "Stream started! 🎉")
                Toast.makeText(this@ViewerActivity, "🔴 Stream is LIVE!", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.i(TAG, "📱 ==== USER OFFLINE ====")
                Log.i(TAG, "📱 Broadcaster UID: $uid")
                Log.i(TAG, "📱 Reason: $reason")
                Log.i(TAG, "📱 Container children before clear: ${remoteVideoContainer?.childCount}")

                remoteVideoContainer?.removeAllViews()

                Log.i(TAG, "📱 Container children after clear: ${remoteVideoContainer?.childCount}")
                Log.i(TAG, "📱 ==== END USER OFFLINE ====")

                tvConnectionStatus?.text = "Stream Ended"
                tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.darker_gray))
                addChatMessage("System", "Stream ended 👋")
                Toast.makeText(this@ViewerActivity, "Stream ended", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "🔊 ==== REMOTE AUDIO STATE CHANGED ====")
                Log.i(TAG, "🔊 UID: $uid")
                Log.i(TAG, "🔊 State: $state")
                Log.i(TAG, "🔊 Reason: $reason")
                Log.i(TAG, "🔊 Elapsed: $elapsed")

                when (state) {
                    Constants.REMOTE_AUDIO_STATE_STARTING -> {
                        Log.i(TAG, "🔊 State: STARTING")
                        tvAudioIndicator?.text = "🔊 Connecting..."
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_orange_light))
                    }
                    Constants.REMOTE_AUDIO_STATE_DECODING -> {
                        Log.i(TAG, "🔊 State: DECODING (Audio Working!)")
                        tvAudioIndicator?.text = "🔊 ON"
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_green_light))
                        addChatMessage("System", "Audio connected! 🎵")
                    }
                    Constants.REMOTE_AUDIO_STATE_STOPPED -> {
                        Log.i(TAG, "🔊 State: STOPPED")
                        tvAudioIndicator?.text = "🔇 OFF"
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_red_light))
                    }
                    Constants.REMOTE_AUDIO_STATE_FROZEN -> {
                        Log.i(TAG, "🔊 State: FROZEN")
                        tvAudioIndicator?.text = "🔊 Buffering..."
                        tvAudioIndicator?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_orange_light))
                    }
                }
                Log.i(TAG, "🔊 ==== END REMOTE AUDIO STATE ====")
            }
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "📹 ==== REMOTE VIDEO STATE CHANGED ====")
                Log.i(TAG, "📹 UID: $uid")
                Log.i(TAG, "📹 State: $state")
                Log.i(TAG, "📹 Reason: $reason")
                Log.i(TAG, "📹 Elapsed: $elapsed")

                when (state) {
                    Constants.REMOTE_VIDEO_STATE_STOPPED -> {
                        Log.i(TAG, "📹 State: STOPPED")
                        addChatMessage("System", "Video stopped 📱")
                    }
                    Constants.REMOTE_VIDEO_STATE_STARTING -> {
                        Log.i(TAG, "📹 State: STARTING")
                        addChatMessage("System", "Video connecting... 📹")
                    }
//                    Constants.REMOTE_VIDEO_STATE_DECODING -> {
//                        Log.i(TAG, "📹 State: DECODING (Video Working!)")
//                        addChatMessage("System", "Video connected! 📺")
//                    }
                    Constants.REMOTE_VIDEO_STATE_FROZEN -> {
                        Log.i(TAG, "📹 State: FROZEN")
                        addChatMessage("System", "Video buffering... ⏳")
                    }
                    Constants.REMOTE_VIDEO_STATE_FAILED -> {
                        Log.e(TAG, "📹 State: FAILED")
                        addChatMessage("System", "Video failed! ❌")
                    }
                }
                Log.i(TAG, "📹 ==== END REMOTE VIDEO STATE ====")
            }
        }

        override fun onRtcStats(stats: RtcStats?) {
            runOnUiThread {
                stats?.let {
                    Log.d(TAG, "📊 RTC Stats - RX Video: ${it.rxVideoKBitRate} kbps, RX Audio: ${it.rxAudioKBitRate} kbps")

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
                Log.i(TAG, "👋 Left channel successfully")
                Toast.makeText(this@ViewerActivity, "Left channel", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onError(err: Int) {
            Log.e(TAG, "❌ ==== AGORA ERROR ====")
            Log.e(TAG, "❌ Error Code: $err")
            Log.e(TAG, "❌ Error Description: ${RtcEngine.getErrorDescription(err)}")
            Log.e(TAG, "❌ ==== END ERROR ====")
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            runOnUiThread {
                Log.i(TAG, "🔗 ==== CONNECTION STATE CHANGED ====")
                Log.i(TAG, "🔗 State: $state")
                Log.i(TAG, "🔗 Reason: $reason")

                when (state) {
                    Constants.CONNECTION_STATE_CONNECTING -> {
                        Log.i(TAG, "🔗 State: CONNECTING")
                        tvConnectionStatus?.text = "Connecting..."
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_orange_light))
                    }
                    Constants.CONNECTION_STATE_CONNECTED -> {
                        Log.i(TAG, "🔗 State: CONNECTED")
                        tvConnectionStatus?.text = "Connected"
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_blue_light))
                    }
                    Constants.CONNECTION_STATE_DISCONNECTED -> {
                        Log.i(TAG, "🔗 State: DISCONNECTED")
                        tvConnectionStatus?.text = "Disconnected"
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.darker_gray))
                    }
                    Constants.CONNECTION_STATE_FAILED -> {
                        Log.e(TAG, "🔗 State: FAILED")
                        tvConnectionStatus?.text = "Connection Failed"
                        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this@ViewerActivity, android.R.color.holo_red_light))
                    }
                }
                Log.i(TAG, "🔗 ==== END CONNECTION STATE ====")
            }
        }

        override fun onLocalAudioStats(stats: LocalAudioStats?) {
            Log.d(TAG, "🎤 Local Audio Stats: $stats")
        }

        override fun onRemoteAudioStats(stats: RemoteAudioStats?) {
            Log.d(TAG, "🔊 Remote Audio Stats: $stats")
        }

        override fun onLocalVideoStats(source: Constants.VideoSourceType?, stats: LocalVideoStats?) {
            Log.d(TAG, "📹 Local Video Stats: $stats")
        }

        override fun onRemoteVideoStats(stats: RemoteVideoStats?) {
            Log.d(TAG, "📺 Remote Video Stats: UID=${stats?.uid}, Width=${stats?.width}, Height=${stats?.height}, RX=${stats?.receivedBitrate}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "🚀 ==== ONCREATE START ====")

        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Keep screen on during viewing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_viewer_layout)
        Log.i(TAG, "🚀 Layout set successfully")

        channelName = CHANNEL_NAME
        Log.i(TAG, "🚀 Channel name: $channelName")

        initViews()
        initializeAndJoinChannel()
        setupClickListeners()
        addWelcomeChatMessages()

        Log.i(TAG, "🚀 ==== ONCREATE END ====")
    }

    private fun initViews() {
        Log.i(TAG, "🎨 ==== INIT VIEWS START ====")

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

        Log.i(TAG, "🎨 Remote video container found: ${remoteVideoContainer != null}")
        Log.i(TAG, "🎨 Remote video container size: ${remoteVideoContainer?.width}x${remoteVideoContainer?.height}")
        Log.i(TAG, "🎨 Remote video container visibility: ${remoteVideoContainer?.visibility}")

        tvChannelInfo?.text = channelName
        tvConnectionStatus?.text = "Connecting..."
        tvViewerCount?.text = "👁️ 1 viewer"
        tvStreamQuality?.text = "📶 HD"
        tvAudioIndicator?.text = "🔊 ON"

        Log.i(TAG, "🎨 ==== INIT VIEWS END ====")
    }

    private fun initializeAndJoinChannel() {
        Log.i(TAG, "⚙️ ==== INITIALIZE AND JOIN START ====")

        try {
            val config = RtcEngineConfig().apply {
                mContext = baseContext
                mAppId = APP_ID
                mEventHandler = mRtcEventHandler
            }
            Log.i(TAG, "⚙️ RtcEngineConfig created with App ID: $APP_ID")

            mRtcEngine = RtcEngine.create(config)
            Log.i(TAG, "⚙️ RtcEngine created successfully: ${mRtcEngine != null}")

            mRtcEngine?.let { engine ->
                Log.i(TAG, "⚙️ Configuring engine...")

                // Basic audio configuration - simplified
                val audioResult = engine.enableAudio()
                Log.i(TAG, "⚙️ Enable audio result: $audioResult")

                // Use default audio profile first
                val audioProfileResult = engine.setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT)
                Log.i(TAG, "⚙️ Audio profile result: $audioProfileResult")

                // Normal volume (100%)
                val volumeResult = engine.adjustPlaybackSignalVolume(100)
                Log.i(TAG, "⚙️ Volume adjustment result: $volumeResult")

                val speakerResult = engine.setDefaultAudioRoutetoSpeakerphone(true)
                Log.i(TAG, "⚙️ Speaker route result: $speakerResult")

                val volumeIndicationResult = engine.enableAudioVolumeIndication(200, 3, true)
                Log.i(TAG, "⚙️ Volume indication result: $volumeIndicationResult")

                // Video configuration
                val videoResult = engine.enableVideo()
                Log.i(TAG, "⚙️ Enable video result: $videoResult")

                // Channel configuration
                val channelProfileResult = engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                Log.i(TAG, "⚙️ Channel profile result: $channelProfileResult")

                val clientRoleResult = engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
                Log.i(TAG, "⚙️ Client role result: $clientRoleResult")


                val options = ChannelMediaOptions().apply {
                    channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                    clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                    autoSubscribeAudio = true
                    autoSubscribeVideo = true
                    publishMicrophoneTrack = false  // Explicit for audience
                    publishCameraTrack = false      // Explicit for audience
                }
                Log.i(TAG, "⚙️ Channel options created - autoSubscribeAudio: ${options.autoSubscribeAudio}, autoSubscribeVideo: ${options.autoSubscribeVideo}")

                Log.i(TAG, "⚙️ About to join channel: $channelName with token: $TOKEN")
                // Use UID 0 to let Agora assign a unique UID
                val joinResult = engine.joinChannel(TOKEN, channelName, 1, options)
                Log.i(TAG, "⚙️ Join channel result: $joinResult")

            } ?: run {
                Log.e(TAG, "⚙️ Failed to create RtcEngine!")
            }

            Log.i(TAG, "⚙️ ==== INITIALIZE AND JOIN END ====")

        } catch (e: Exception) {
            Log.e(TAG, "⚙️ ==== EXCEPTION IN INITIALIZE ====")
            Log.e(TAG, "⚙️ Exception: ${e.message}")
            Log.e(TAG, "⚙️ Stack trace: ${e.stackTrace.contentToString()}")
            Log.e(TAG, "⚙️ ==== END EXCEPTION ====")

            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        Log.i(TAG, "📺 ==== SETUP REMOTE VIDEO START ====")
        Log.i(TAG, "📺 Setting up video for UID: $uid")
        Log.i(TAG, "📺 Container exists: ${remoteVideoContainer != null}")
        Log.i(TAG, "📺 Container current children: ${remoteVideoContainer?.childCount}")

        try {
            val surfaceView = RtcEngine.CreateRendererView(baseContext)
            Log.i(TAG, "📺 SurfaceView created: ${surfaceView != null}")
            Log.i(TAG, "📺 SurfaceView class: ${surfaceView?.javaClass?.name}")

            remoteVideoContainer?.let { container ->
                Log.i(TAG, "📺 Removing all existing views...")
                container.removeAllViews()
                Log.i(TAG, "📺 Children after removal: ${container.childCount}")

                Log.i(TAG, "📺 Adding SurfaceView to container...")
                container.addView(surfaceView)
                Log.i(TAG, "📺 Children after adding: ${container.childCount}")

                Log.i(TAG, "📺 Container dimensions: ${container.width}x${container.height}")
                Log.i(TAG, "📺 Container visibility: ${container.visibility}")

            } ?: run {
                Log.e(TAG, "📺 Container is null!")
            }

            val remoteVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
            Log.i(TAG, "📺 VideoCanvas created - UID: ${remoteVideoCanvas.uid}, RenderMode: ${remoteVideoCanvas.renderMode}")

            val setupResult = mRtcEngine?.setupRemoteVideo(remoteVideoCanvas)
            Log.i(TAG, "📺 setupRemoteVideo result: $setupResult")

            // Additional surface view checks
            surfaceView?.let { sv ->
                Log.i(TAG, "📺 SurfaceView holder: ${sv.holder}")
                Log.i(TAG, "📺 SurfaceView visibility: ${sv.visibility}")
                Log.i(TAG, "📺 SurfaceView layout params: ${sv.layoutParams}")

                sv.post {
                    Log.i(TAG, "📺 SurfaceView final dimensions: ${sv.width}x${sv.height}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "📺 ==== EXCEPTION IN SETUP REMOTE VIDEO ====")
            Log.e(TAG, "📺 Exception: ${e.message}")
            Log.e(TAG, "📺 Stack trace: ${e.stackTrace.contentToString()}")
            Log.e(TAG, "📺 ==== END EXCEPTION ====")
        }

        Log.i(TAG, "📺 ==== SETUP REMOTE VIDEO END ====")
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
        Log.i(TAG, "💀 ==== ONDESTROY START ====")
        super.onDestroy()
        mRtcEngine?.let { engine ->
            Log.i(TAG, "💀 Leaving channel...")
            engine.leaveChannel()
            Log.i(TAG, "💀 Destroying engine...")
            RtcEngine.destroy()
        }
        mRtcEngine = null
        Log.i(TAG, "💀 ==== ONDESTROY END ====")
    }

    companion object {
        private const val TAG = "ViewerActivity"
        private const val CHANNEL_NAME = "class10"
        // Replace with your actual Agora App ID
        private const val TOKEN = "006d9de868cb3af4d3993740451364ff302IAClKfE+2Ldhr5HTH7IMVyc4UD9gGS58910C5qygTLD+O7/27zO379yDEABr+oQ8LnLGaAEAAQC+LsVo"
        private const val APP_ID = "d9de868cb3af4d3993740451364ff302"
    }
}