package com.crmind.agoratesting

import android.Manifest

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var etChannelName: EditText? = null
    private var btnStartBroadcast: Button? = null
    private var btnJoinAsViewer: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
            checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
            checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)
        ) {
            // Permissions already granted
        }
    }

    private fun initViews() {
        etChannelName = findViewById<EditText>(R.id.et_channel_name)
        btnStartBroadcast = findViewById<Button>(R.id.btn_start_broadcast)
        btnJoinAsViewer = findViewById<Button>(R.id.btn_join_as_viewer)
    }

    private fun setupClickListeners() {
        btnStartBroadcast!!.setOnClickListener { v: View? ->
            val channelName = etChannelName!!.text.toString().trim { it <= ' ' }
            if (channelName.isEmpty()) {
                Toast.makeText(this, "Please enter channel name", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val intent = Intent(
                this,
                BroadcastActivity::class.java
            )
            intent.putExtra("CHANNEL_NAME", channelName)
            startActivity(intent)
        }

        btnJoinAsViewer!!.setOnClickListener { v: View? ->
            val channelName = etChannelName!!.text.toString().trim { it <= ' ' }
            if (channelName.isEmpty()) {
                Toast.makeText(this, "Please enter channel name", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val intent = Intent(
                this,
                ViewerActivity::class.java
            )
            intent.putExtra("CHANNEL_NAME", channelName)
            startActivity(intent)
        }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQ_ID) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Permission denied. App may not work properly.",
                        Toast.LENGTH_LONG
                    ).show()
                    break
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}