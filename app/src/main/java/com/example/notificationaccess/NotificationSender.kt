// NotificationSender.kt
package com.example.notificationaccess

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationSender {

    private val client = OkHttpClient()
    private const val TAG = "NotificationSender"
    private val handler = Handler(Looper.getMainLooper())
    private var serverUrlGlobal: String? = null
    private var isHeartbeatRunning = false

    // Global Context for LocalBroadcastManager (must be set by MyNotificationService)
    private var appContext: Context? = null

    // Constants for Local Broadcast
    const val ACTION_STATUS_UPDATE = "com.example.notificationaccess.STATUS_UPDATE"
    const val EXTRA_STATUS_MESSAGE = "status_message"

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private fun broadcastStatus(message: String) {
        if (appContext != null) {
            val intent = Intent(ACTION_STATUS_UPDATE).apply {
                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                putExtra(EXTRA_STATUS_MESSAGE, "[$timestamp] $message")
            }
            LocalBroadcastManager.getInstance(appContext!!).sendBroadcast(intent)
        }
    }

    fun sendNotificationToServer(
        serverUrl: String,
        title: String,
        content: String,
        packageName: String
    ) {
        serverUrlGlobal = serverUrl
        val json = JSONObject().apply {
            put("title", title)
            put("content", content)
            put("package", packageName)
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        broadcastStatus("🚀 Sending: $title ($packageName)")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val errorMsg = "❌ Failed: ${e.message}"
                Log.e(TAG, errorMsg)
                broadcastStatus(errorMsg)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseMsg = if (response.isSuccessful) {
                    "✅ Success. Code: ${response.code}"
                } else {
                    "⚠️ Server Error. Code: ${response.code}"
                }
                Log.d(TAG, responseMsg)
                broadcastStatus(responseMsg)
                response.close()
            }
        })
    }

    /**
     * 🟢 Start sending "hi" message every 30 seconds if no real notification comes
     */
    fun startHeartbeat(serverUrl: String) {
        serverUrlGlobal = serverUrl
        if (!isHeartbeatRunning) {
            isHeartbeatRunning = true
            Log.d(TAG, "💓 Heartbeat started")
            broadcastStatus("💓 Heartbeat started")
            handler.post(heartbeatRunnable)
        } else {
            // Restart the heartbeat with the potentially new URL
            handler.removeCallbacks(heartbeatRunnable)
            handler.post(heartbeatRunnable)
        }
    }

    /**
     * 🔴 Stop heartbeat if needed
     */
    fun stopHeartbeat() {
        if (isHeartbeatRunning) {
            isHeartbeatRunning = false
            handler.removeCallbacks(heartbeatRunnable)
            Log.d(TAG, "🛑 Heartbeat stopped")
            broadcastStatus("🛑 Heartbeat stopped")
        }
    }

    // Check if the heartbeat is running
    fun isRunning(): Boolean = isHeartbeatRunning


    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (isHeartbeatRunning && serverUrlGlobal != null) {
                // Only send a low-profile log for the regular heartbeat to avoid spam
                Log.d(TAG, "Heartbeat pulse...")
                sendNotificationToServer(
                    serverUrlGlobal!!,
                    "Heartbeat",
                    "hi",
                    "com.example.notificationaccess" // Use app's package for heartbeat
                )
                handler.postDelayed(this, 30_000) // every 30 seconds
            }
        }
    }
}