package com.crmind.agoratesting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class LiveStreamActivity : ComponentActivity() {

    companion object {
        private const val APP_ID = "d9de868cb3af4d3993740451364ff302"
        private const val TOKEN = "945de4a5927840c2808013c780956093" // token generated for audience role
        private const val CHANNEL_NAME = "class10"
    }

    private var rtcEngine: RtcEngine? = null
    private var remoteContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteContainer = FrameLayout(this).apply { id = ViewGroup.generateViewId() }

        setContentView(remoteContainer)

        initAgoraEngine()
    }

    private fun initAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(baseContext, APP_ID, rtcEventHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        rtcEngine?.apply {
            setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            setClientRole(Constants.CLIENT_ROLE_AUDIENCE)

            enableVideo()

            joinChannel(TOKEN, CHANNEL_NAME, "", 0)
        }
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                remoteContainer?.removeAllViews()
            }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = SurfaceView(baseContext)
        remoteContainer?.addView(surfaceView)
        rtcEngine?.setupRemoteVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
