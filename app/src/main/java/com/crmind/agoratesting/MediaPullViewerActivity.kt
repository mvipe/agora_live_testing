//package com.crmind.agoratesting
//
//import android.os.Bundle
//import android.util.Log
//import android.widget.Button
//import android.widget.FrameLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import io.agora.mediaplayer.IMediaPlayerObserver
//import io.agora.mediaplayer.data.MediaPlayerSource
//import io.agora.rtc2.ChannelMediaOptions
//import io.agora.rtc2.IRtcEngineEventHandler
//import io.agora.rtc2.RtcEngine
//import io.agora.rtc2.RtcEngineConfig
//import io.agora.rtc2.video.VideoCanvas
//
//class MediaPullViewerActivity : AppCompatActivity() {
//    private var mRtcEngine: RtcEngine? = null
//    private var channelName: String? = null
//    private var mediaPullSourceId: Int = 100 // Unique ID for media pull source
//
//    // UI Elements (same as before)
//    private var remoteVideoContainer: FrameLayout? = null
//    private var btnLeaveChannel: Button? = null
//    private var tvConnectionStatus: TextView? = null
//
//    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
//
//        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
//            runOnUiThread {
//                Log.i(TAG, "✅ Joined channel successfully")
//                updateConnectionStatus("Connected", android.R.color.holo_green_light)
//
//                // Start media pull after joining channel
//                startMediaPull()
//            }
//        }
//
//        override fun onUserJoined(uid: Int, elapsed: Int) {
//            runOnUiThread {
//                Log.i(TAG, "🎥 Media source connected: $uid")
//                if (uid == mediaPullSourceId) {
//                    setupRemoteVideo(uid)
//                    updateConnectionStatus("🔴 LIVE", android.R.color.holo_red_light)
//                    Toast.makeText(this@MediaPullViewerActivity, "🔴 Stream is LIVE!", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//
//        override fun onUserOffline(uid: Int, reason: Int) {
//            runOnUiThread {
//                Log.i(TAG, "📱 Media source disconnected: $uid")
//                if (uid == mediaPullSourceId) {
//                    remoteVideoContainer?.removeAllViews()
//                    updateConnectionStatus("Stream Ended", android.R.color.darker_gray)
//                    Toast.makeText(this@MediaPullViewerActivity, "Stream ended", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//
//        override fun onError(err: Int) {
//            Log.e(TAG, "❌ Error: $err - ${RtcEngine.getErrorDescription(err)}")
//            runOnUiThread {
//                Toast.makeText(this@MediaPullViewerActivity, "Error: ${RtcEngine.getErrorDescription(err)}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_viewer_layout)
//
//        channelName = intent.getStringExtra("CHANNEL_NAME")
//        initViews()
//        initializeEngine()
//    }
//
//    private fun initializeEngine() {
//        try {
//            val config = RtcEngineConfig().apply {
//                mContext = baseContext
//                mAppId = APP_ID
//                mEventHandler = mRtcEventHandler
//            }
//
//            mRtcEngine = RtcEngine.create(config)
//
//            mRtcEngine?.let { engine ->
//                // Enable audio and video
//                engine.enableAudio()
//                engine.enableVideo()
//
//                // Set channel profile for live broadcasting
////                engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
////                engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
//
//                // Join channel
//                val options = ChannelMediaOptions().apply {
////                    channelProfile = Constants.CHANN
////                    clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
//                    autoSubscribeAudio = true
//                    autoSubscribeVideo = true
//                }
//
//                engine.joinChannel(null, channelName, 0, options)
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error initializing engine: ${e.message}")
//            e.printStackTrace()
//        }
//    }
//
//    private fun startMediaPull() {
//        mRtcEngine?.let { engine ->
//
//            // Your RTMP stream URL - this should be your OBS RTMP output URL
//            val rtmpUrl = "rtmp://your-rtmp-server.com/live/$channelName"
//
//            // Media pull configuration
//            val config = MediaPlayerSource().apply {
//                url = rtmpUrl
//                //tag = mediaPullSourceId
//            }
//
//            try {
//                // Start media pull
////                val result = engine.start(config)
////                Log.d(TAG, "Media pull started with result: $result")
////
////                if (result == 0) {
////                    Toast.makeText(this, "Connecting to stream...", Toast.LENGTH_SHORT).show()
////                } else {
////                    Toast.makeText(this, "Failed to connect to stream", Toast.LENGTH_SHORT).show()
////                }
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error starting media pull: ${e.message}")
//                // Fallback: Try direct URL approach
//                tryDirectStreamUrl()
//            }
//        }
//    }
//
//    private fun tryDirectStreamUrl() {
//        // Alternative approach using Agora's media player
//        mRtcEngine?.let { engine ->
//            try {
//                // Create media player
//                val mediaPlayer = engine.createMediaPlayer()
//
//                // Your stream URL - could be RTMP, HLS, or HTTP-FLV
//                val streamUrl = "https://your-stream-server.com/live/$channelName.m3u8" // HLS
//                // or "rtmp://your-rtmp-server.com/live/$channelName" // RTMP
//
//                mediaPlayer?.let { player ->
//                    // Set player event handler
//                    player.setPlayerObserver(object : IMediaPlayerObserver {
//                        override fun onPlayerStateChanged(state: MediaPlayerState, error: MediaPlayerError) {
//                            runOnUiThread {
//                                when (state) {
//                                    MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED -> {
//                                        Log.d(TAG, "Stream opened successfully")
//                                        player.play()
//                                    }
//                                    MediaPlayerState.PLAYER_STATE_PLAYING -> {
//                                        Log.d(TAG, "Stream is playing")
//                                        updateConnectionStatus("🔴 LIVE", android.R.color.holo_red_light)
//
//                                        // Publish the media player stream to channel
//                                        val options = ChannelMediaOptions().apply {
//                                            publishMediaPlayerAudioTrack = true
//                                            publishMediaPlayerVideoTrack = true
//                                            publishMediaPlayerId = player.getMediaPlayerId()
//                                        }
//                                        engine.updateChannelMediaOptions(options)
//                                    }
//                                    MediaPlayerState.PLAYER_STATE_FAILED -> {
//                                        Log.e(TAG, "Stream failed: $error")
//                                        updateConnectionStatus("Stream Failed", android.R.color.holo_red_light)
//                                    }
//                                }
//                            }
//                        }
//
//                        override fun onPositionChanged(position_ms: Long, timestamp_ms: Long) {}
//                        override fun onPlayerEvent(eventCode: MediaPlayerEvent, elapsedTime: Long, message: String?) {}
//                        override fun onMetadata(type: MediaPlayerMetadataType, data: ByteArray?) {}
//                        override fun onPlayBufferUpdated(playCachedBuffer: Long) {}
//                        override fun onPreloadEvent(src: String?, event: MediaPlayerPreloadEvent) {}
//                        override fun onCompleted() {}
//                        override fun onAgoraCDNTokenWillExpire() {}
//                        override fun onPlayerSrcInfoChanged(from: SrcInfo?, to: SrcInfo?) {}
//                        override fun onPlayerInfoUpdated(info: PlayerUpdatedInfo?) {}
//                        override fun onAudioVolumeIndication(volume: Int) {}
//                    })
//
//                    // Open the stream
//                    player.open(streamUrl, 0)
//                }
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error with media player: ${e.message}")
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private fun setupRemoteVideo(uid: Int) {
//        val surfaceView = RtcEngine.CreateRendererView(baseContext)
//        remoteVideoContainer?.removeAllViews()
//        remoteVideoContainer?.addView(surfaceView)
//
//        val remoteVideoCanvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
//        mRtcEngine?.setupRemoteVideo(remoteVideoCanvas)
//
//        Log.d(TAG, "Remote video setup for uid: $uid")
//    }
//
//    private fun initViews() {
//        remoteVideoContainer = findViewById(R.id.remote_video_view_container)
//        btnLeaveChannel = findViewById(R.id.btn_leave_channel)
//        tvConnectionStatus = findViewById(R.id.tv_connection_status)
//
//        tvConnectionStatus?.text = "Connecting..."
//
//        btnLeaveChannel?.setOnClickListener {
//            finish()
//        }
//    }
//
//    private fun updateConnectionStatus(status: String, colorRes: Int) {
//        tvConnectionStatus?.text = status
//        tvConnectionStatus?.setTextColor(ContextCompat.getColor(this, colorRes))
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mRtcEngine?.let { engine ->
//            engine.leaveChannel()
//            RtcEngine.destroy()
//        }
//    }
//
//    companion object {
//        private const val TAG = "MediaPullViewer"
//        private const val APP_ID = "d9de868cb3af4d3993740451364ff302"
//    }
//}