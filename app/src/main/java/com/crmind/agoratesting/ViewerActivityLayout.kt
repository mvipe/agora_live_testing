package com.crmind.agoratesting

import android.media.AudioManager
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

class ViewerActivity : AppCompatActivity() {
    private var mRtcEngine: RtcEngine? = null
    private var channelName: String? = null
    private var audioMuted = false

    private var remoteVideoContainer: FrameLayout? = null
    private var btnMuteAudio: Button? = null
    private var btnLeaveChannel: Button? = null
    private var tvChannelInfo: TextView? = null
    private var tvConnectionStatus: TextView? = null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "✅ Join channel success, uid: $uid")
                tvConnectionStatus?.text = "Connected"
                Toast.makeText(this@ViewerActivity, "Connected! Waiting for broadcaster...", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.i(TAG, "👤 User joined: $uid")
                setupRemoteVideo(uid)
                tvConnectionStatus?.text = "🔴 LIVE"
                Toast.makeText(this@ViewerActivity, "Broadcaster connected!", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.i(TAG, "👤 User offline: $uid, reason: $reason")
                remoteVideoContainer?.removeAllViews()
                tvConnectionStatus?.text = "Stream ended"
                Toast.makeText(this@ViewerActivity, "Broadcaster left", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            Log.d(TAG, "🎵 Remote audio state changed - uid: $uid, state: $state, reason: $reason")
            runOnUiThread {
                when (state) {
                    Constants.REMOTE_AUDIO_STATE_STOPPED -> {
                        Log.d(TAG, "❌ Remote audio STOPPED")
                        Toast.makeText(this@ViewerActivity, "Audio stopped", Toast.LENGTH_SHORT).show()
                    }
                    Constants.REMOTE_AUDIO_STATE_STARTING -> {
                        Log.d(TAG, "🎵 Remote audio STARTING...")
                        Toast.makeText(this@ViewerActivity, "Audio starting...", Toast.LENGTH_SHORT).show()
                    }
                    Constants.REMOTE_AUDIO_STATE_DECODING -> {
                        Log.d(TAG, "✅ Remote audio DECODING - AUDIO SHOULD BE WORKING!")
                        Toast.makeText(this@ViewerActivity, "🔊 Audio connected!", Toast.LENGTH_LONG).show()
                        tvConnectionStatus?.text = "🔴 LIVE (Audio: ON)"
                    }
                    Constants.REMOTE_AUDIO_STATE_FROZEN -> {
                        Log.d(TAG, "❄️ Remote audio FROZEN")
                        Toast.makeText(this@ViewerActivity, "Audio frozen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
            // Debug: Log when we receive audio
            speakers?.forEach { speaker ->
                if (speaker.volume > 0) {
                    Log.d(TAG, "🔊 Receiving audio from uid ${speaker.uid} - volume: ${speaker.volume}")
                    runOnUiThread {
                        tvConnectionStatus?.text = "🔴 LIVE (Audio: ${speaker.volume})"
                    }
                }
            }
        }

        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            Log.d(TAG, "📹 Remote video state changed - uid: $uid, state: $state, reason: $reason")
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            runOnUiThread {
                Log.i(TAG, "Left channel")
                Toast.makeText(this@ViewerActivity, "Left channel", Toast.LENGTH_SHORT).show()
            }
        }


        override fun onError(err: Int) {
            Log.e(TAG, "❌ Error: $err - ${RtcEngine.getErrorDescription(err)}")
            runOnUiThread {
                Toast.makeText(this@ViewerActivity, "Error: $err", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            runOnUiThread {
                when (state) {
                    Constants.CONNECTION_STATE_CONNECTING -> tvConnectionStatus?.text = "Connecting..."
                    Constants.CONNECTION_STATE_CONNECTED -> tvConnectionStatus?.text = "Connected"
                    Constants.CONNECTION_STATE_DISCONNECTED -> tvConnectionStatus?.text = "Disconnected"
                    Constants.CONNECTION_STATE_FAILED -> tvConnectionStatus?.text = "Connection Failed"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer_layout)

        channelName = intent.getStringExtra("CHANNEL_NAME")

        checkAudioSettings()
        initViews()
        initializeAndJoinChannel()
        setupClickListeners()
    }

    private fun checkAudioSettings() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        Log.d(TAG, "🔊 Viewer Audio Settings:")
        Log.d(TAG, "- Media Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}")
        Log.d(TAG, "- Max Media Volume: ${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
        Log.d(TAG, "- Voice Call Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)}")
        Log.d(TAG, "- Ringer Mode: ${audioManager.ringerMode}")
        Log.d(TAG, "- Is Speakerphone On: ${audioManager.isSpeakerphoneOn}")
    }

    private fun initViews() {
        remoteVideoContainer = findViewById(R.id.remote_video_view_container)
        btnMuteAudio = findViewById(R.id.btn_mute_audio)
        btnLeaveChannel = findViewById(R.id.btn_leave_channel)
        tvChannelInfo = findViewById(R.id.tv_channel_info_viewer)
        tvConnectionStatus = findViewById(R.id.tv_connection_status)

        tvChannelInfo?.text = "👁️ Watching: $channelName"
        tvConnectionStatus?.text = "Connecting..."
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
                Log.d(TAG, "🎵 Configuring audio for viewer...")

                // Audio configuration for receiving
                engine.enableAudio()
                Log.d(TAG, "✅ Audio enabled")

                // Set audio profile for better quality
                engine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO)
                Log.d(TAG, "✅ Audio profile set")

                // Boost playback volume
                engine.adjustPlaybackSignalVolume(400) // Max boost
                Log.d(TAG, "✅ Playback volume boosted to 400")

                // Route to speaker
                engine.setDefaultAudioRoutetoSpeakerphone(true)
                Log.d(TAG, "✅ Audio routed to speakerphone")

                // Enable volume indication
                engine.enableAudioVolumeIndication(200, 3, true)
                Log.d(TAG, "✅ Volume indication enabled")

                // Video configuration
                engine.enableVideo()

                // Set channel profile and role
                engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
                engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)

                // Join channel
                val options = ChannelMediaOptions().apply {
                    channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
                    clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                    autoSubscribeAudio = true  // CRITICAL
                    autoSubscribeVideo = true
                }

                Log.d(TAG, "🚀 Joining channel as viewer: $channelName")
                val result = engine.joinChannel(null, channelName, 0, options)
                Log.d(TAG, "Join result: $result ${if (result == 0) "✅" else "❌"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error initializing Agora: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView: SurfaceView = RtcEngine.CreateRendererView(baseContext)
        remoteVideoContainer?.removeAllViews()
        remoteVideoContainer?.addView(surfaceView)

        val remoteVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        mRtcEngine?.setupRemoteVideo(remoteVideoCanvas)

        Log.d(TAG, "📹 Remote video setup for uid: $uid")
    }

    private fun setupClickListeners() {
        btnMuteAudio?.setOnClickListener {
            audioMuted = !audioMuted
            mRtcEngine?.muteAllRemoteAudioStreams(audioMuted)
            btnMuteAudio?.text = if (audioMuted) "🔇 Unmute Audio" else "🔊 Mute Audio"
            Log.d(TAG, "🔇 Remote audio muted: $audioMuted")

            val message = if (audioMuted) "Audio muted" else "Audio unmuted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        btnLeaveChannel?.setOnClickListener {
            finish()
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