package com.crmind.agoratesting

import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private var localVideoContainer: FrameLayout? = null
    private var btnMute: Button? = null
    private var btnCamera: Button? = null
    private var btnEndBroadcast: Button? = null
    private var tvChannelInfo: TextView? = null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "Join channel success, uid: $uid")
                Toast.makeText(
                    this@BroadcastActivity,
                    "Broadcasting started! 🔴 LIVE",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                Log.i(TAG, "Leave channel")
                Toast.makeText(
                    this@BroadcastActivity,
                    "Broadcast ended",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            // Debug: Log audio levels
            if (totalVolume > 5) {
                Log.d(TAG, "Broadcasting audio - volume: $totalVolume")
            }
        }


        override fun onError(err: Int) {
            Log.e(TAG, "onError code $err message ${RtcEngine.getErrorDescription(err)}")
            runOnUiThread {
                Toast.makeText(this@BroadcastActivity, "Error: $err", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        btnEndBroadcast = findViewById(R.id.btn_end_broadcast)
        tvChannelInfo = findViewById(R.id.tv_channel_info)

        tvChannelInfo?.text = "🔴 Broadcasting: $channelName"
    }

    private fun initializeAndJoinChannel() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = baseContext
                mAppId = APP_ID
                mEventHandler = mRtcEventHandler
            }

            Log.d(TAG, "Creating Agora engine...")
            mRtcEngine = RtcEngine.create(config)

            mRtcEngine?.let { engine ->
                Log.d(TAG, "Configuring audio and video...")

                // Audio configuration
                engine.enableAudio()
                engine.enableLocalAudio(true)
                engine.setDefaultAudioRoutetoSpeakerphone(true)
                engine.enableAudioVolumeIndication(500, 3, true)

                // Video configuration
                engine.enableVideo()
                engine.enableLocalVideo(true)

                // Set channel profile and role
                engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

                // Setup local video preview
                setupLocalVideo()

                // Join channel with explicit options
                val options = ChannelMediaOptions().apply {
                    channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    autoSubscribeAudio = true
                    autoSubscribeVideo = true
                    publishCameraTrack = true
                    publishMicrophoneTrack = true
                    publishCustomAudioTrack = true
                }

                Log.d(TAG, "Joining channel: $channelName")
                val result = engine.joinChannel(null, channelName, 0, options)
                Log.d(TAG, "Join channel result: $result")

                if (result == 0) {
                    Toast.makeText(this, "Starting broadcast...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to join channel: $result", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Agora: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupLocalVideo() {
        val surfaceView: SurfaceView = RtcEngine.CreateRendererView(baseContext)
        surfaceView.setZOrderMediaOverlay(true)
        localVideoContainer?.addView(surfaceView)

        val localVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        mRtcEngine?.let { engine ->
            engine.setupLocalVideo(localVideoCanvas)
            engine.startPreview()
            Log.d(TAG, "Local video preview started")
        }
    }

    private fun setupClickListeners() {
        btnMute?.setOnClickListener {
            muted = !muted
            mRtcEngine?.let { engine ->
                engine.muteLocalAudioStream(muted)
                engine.enableLocalAudio(!muted)
            }
            btnMute?.text = if (muted) "🔇 Unmute" else "🎤 Mute"
            Log.d(TAG, "Audio muted: $muted")

            val message = if (muted) "Microphone muted" else "Microphone active"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        btnCamera?.setOnClickListener {
            cameraOn = !cameraOn
            mRtcEngine?.muteLocalVideoStream(!cameraOn)
            btnCamera?.text = if (cameraOn) "📹 Camera Off" else "📷 Camera On"
            Log.d(TAG, "Camera on: $cameraOn")
        }

        btnEndBroadcast?.setOnClickListener {
            Toast.makeText(this, "Ending broadcast...", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying broadcast activity")
        mRtcEngine?.let { engine ->
            engine.leaveChannel()
            RtcEngine.destroy()
        }
        mRtcEngine = null
    }

    companion object {
        private const val TAG = "BroadcastActivity"
        // Replace with your actual Agora App ID
        private val APP_ID = com.crmind.agoratesting.Constants().AGora_APP_ID
    }
}