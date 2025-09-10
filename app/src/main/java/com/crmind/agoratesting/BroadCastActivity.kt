package com.crmind.agoratesting

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
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

class BroadcastActivity : AppCompatActivity() {
    private var mRtcEngine: RtcEngine? = null
    private var channelName: String? = null
    private var muted = false
    private var cameraOn = true
    private var frontCamera = true

    // UI Elements
    private var localVideoContainer: FrameLayout? = null
    private var btnMute: Button? = null
    private var btnCamera: Button? = null
    private var btnSwitchCamera: Button? = null
    private var btnEndBroadcast: Button? = null
    private var tvChannelInfo: TextView? = null
    private var tvViewerCount: TextView? = null
    private var tvStreamDuration: TextView? = null
    private var tvAudioStatus: TextView? = null
    private var tvVideoStatus: TextView? = null
    private var tvStreamQuality: TextView? = null

    // Stream timing
    private var streamStartTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateStreamDuration()
            handler.postDelayed(this, 1000)
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "✅ Broadcast started - UID: $uid")
                Toast.makeText(this@BroadcastActivity, "🔴 LIVE - Broadcasting!", Toast.LENGTH_SHORT).show()

                streamStartTime = System.currentTimeMillis()
                handler.post(timeUpdateRunnable)
                updateStreamStatus()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "👤 Viewer joined: $uid")
                updateViewerCount(1) // In real app, track actual count
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.i(TAG, "👤 Viewer left: $uid")
                updateViewerCount(0) // In real app, track actual count
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                Log.i(TAG, "Stream ended")
                handler.removeCallbacks(timeUpdateRunnable)
            }
        }

        override fun onLocalAudioStateChanged(state: Int, error: Int) {
            runOnUiThread {
                when (state) {
                    Constants.LOCAL_AUDIO_STREAM_STATE_RECORDING -> {
                        tvAudioStatus?.text = "🎤 ON"
                        tvAudioStatus?.setTextColor(ContextCompat.getColor(this@BroadcastActivity, android.R.color.holo_green_light))
                    }
                    Constants.LOCAL_AUDIO_STREAM_STATE_STOPPED -> {
                        tvAudioStatus?.text = "🎤 OFF"
                        tvAudioStatus?.setTextColor(ContextCompat.getColor(this@BroadcastActivity, android.R.color.holo_red_light))
                    }
                }
            }
        }

//        override fun onLocalVideoStateChanged(source: Int, state: Int, error: Int) {
//            runOnUiThread {
//                when (state) {
//                    Constants.LOCAL_VIDEO_STREAM_STATE_CAPTURING -> {
//                        tvVideoStatus?.text = "📹 ON"
//                        tvVideoStatus?.setTextColor(ContextCompat.getColor(this@BroadcastActivity, android.R.color.holo_green_light))
//                    }
//                    Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED -> {
//                        tvVideoStatus?.text = "📹 OFF"
//                        tvVideoStatus?.setTextColor(ContextCompat.getColor(this@BroadcastActivity, android.R.color.holo_red_light))
//                    }
//                }
//            }
//        }

        override fun onRtcStats(stats: RtcStats?) {
            runOnUiThread {
                stats?.let {
                    // Update quality indicator based on stats
                    val quality = when {
                        it.txVideoKBitRate > 2000 -> "HD"
                        it.txVideoKBitRate > 1000 -> "SD"
                        else -> "LD"
                    }
                    tvStreamQuality?.text = quality
                }
            }
        }



        override fun onError(err: Int) {
            Log.e(TAG, "❌ Error: $err - ${RtcEngine.getErrorDescription(err)}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Keep screen on during broadcast
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_broad_cast)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        initViews()
        initializeAndJoinChannel()
        setupClickListeners()
    }

    private fun initViews() {
        localVideoContainer = findViewById(R.id.local_video_view_container)
        btnMute = findViewById(R.id.btn_mute)
        btnCamera = findViewById(R.id.btn_camera)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera)
        btnEndBroadcast = findViewById(R.id.btn_end_broadcast)
        tvChannelInfo = findViewById(R.id.tv_channel_info)
        tvViewerCount = findViewById(R.id.tv_viewer_count)
        tvStreamDuration = findViewById(R.id.tv_stream_duration)
        tvAudioStatus = findViewById(R.id.tv_audio_status)
        tvVideoStatus = findViewById(R.id.tv_video_status)
        tvStreamQuality = findViewById(R.id.tv_stream_quality)

        tvChannelInfo?.text = channelName
        tvViewerCount?.text = "👁️ 0 viewers"
        tvStreamDuration?.text = "⏱️ 00:00"
        tvStreamQuality?.text = "HD"
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
                //engine.setAudioScenario(Constants.AUDIO_SCENARIO_BROADCASTING)
                engine.enableLocalAudio(true)
                engine.adjustRecordingSignalVolume(400)
                engine.setDefaultAudioRoutetoSpeakerphone(true)

                // Video configuration for portrait streaming
                engine.enableVideo()
                engine.enableLocalVideo(true)

                // Set video encoder configuration for portrait
                val videoConfig = io.agora.rtc2.video.VideoEncoderConfiguration().apply {
                    dimensions = io.agora.rtc2.video.VideoEncoderConfiguration.VideoDimensions(720, 1280) // Portrait HD
                    frameRate = io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30.value
                    bitrate = 2000 // 2Mbps for good quality
                    orientationMode = io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
                }
                engine.setVideoEncoderConfiguration(videoConfig)

                // Channel configuration
                engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

                // Setup local video
                setupLocalVideo()

                // Join channel
                val options = ChannelMediaOptions().apply {
                    channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    autoSubscribeAudio = true
                    autoSubscribeVideo = true
                    publishCameraTrack = true
                    publishMicrophoneTrack = true
                    publishCustomAudioTrack = true
                }

                engine.joinChannel(null, channelName, 0, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Agora: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupLocalVideo() {
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        localVideoContainer?.addView(surfaceView)

        val localVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        mRtcEngine?.let { engine ->
            engine.setupLocalVideo(localVideoCanvas)
            engine.startPreview()
        }
    }

    private fun setupClickListeners() {
        btnMute?.setOnClickListener {
            muted = !muted
            mRtcEngine?.muteLocalAudioStream(muted)
            btnMute?.text = if (muted) "🔇 Unmute" else "🎤 Mute"
            updateStreamStatus()
        }

        btnCamera?.setOnClickListener {
            cameraOn = !cameraOn
            mRtcEngine?.muteLocalVideoStream(!cameraOn)
            btnCamera?.text = if (cameraOn) "📹 Camera Off" else "📷 Camera On"
            updateStreamStatus()
        }

        btnSwitchCamera?.setOnClickListener {
            mRtcEngine?.switchCamera()
            frontCamera = !frontCamera
            Toast.makeText(this, "Switched to ${if (frontCamera) "front" else "back"} camera", Toast.LENGTH_SHORT).show()
        }

        btnEndBroadcast?.setOnClickListener {
            showEndStreamDialog()
        }
    }

    private fun showEndStreamDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("End Stream")
            .setMessage("Are you sure you want to end your live stream?")
            .setPositiveButton("End Stream") { _, _ ->
                Toast.makeText(this, "Stream ended", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStreamDuration() {
        if (streamStartTime > 0) {
            val duration = (System.currentTimeMillis() - streamStartTime) / 1000
            val minutes = duration / 60
            val seconds = duration % 60
            tvStreamDuration?.text = "⏱️ ${String.format("%02d:%02d", minutes, seconds)}"
        }
    }

    private fun updateViewerCount(count: Int) {
        tvViewerCount?.text = "👁️ $count viewer${if (count != 1) "s" else ""}"
    }

    private fun updateStreamStatus() {
        tvAudioStatus?.text = if (muted) "🔇 MUTED" else "🎤 ON"
        tvAudioStatus?.setTextColor(
            ContextCompat.getColor(
                this,
                if (muted) android.R.color.holo_red_light else android.R.color.holo_green_light
            )
        )

        tvVideoStatus?.text = if (cameraOn) "📹 ON" else "📷 OFF"
        tvVideoStatus?.setTextColor(
            ContextCompat.getColor(
                this,
                if (cameraOn) android.R.color.holo_green_light else android.R.color.holo_red_light
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeUpdateRunnable)
        mRtcEngine?.let { engine ->
            engine.leaveChannel()
            RtcEngine.destroy()
        }
        mRtcEngine = null
    }

    companion object {
        private const val TAG = "BroadcastActivity"
        // Replace with your actual Agora App ID
        private const val APP_ID = "981b297946924367814392114c9baed9"
    }
}