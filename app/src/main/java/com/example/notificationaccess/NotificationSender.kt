package com.example.notificationaccess   // ⚠️ use your correct package name

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object NotificationSender {

    private val client = OkHttpClient()
    private const val TAG = "NotificationSender"
    private val handler = Handler(Looper.getMainLooper())
    private var serverUrlGlobal: String? = null
    private var isHeartbeatRunning = false

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

        Log.d(TAG, "🚀 Sending notification to server: $serverUrl")
        Log.d(TAG, "📦 Payload: $json")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ Failed to send notification: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "✅ Notification sent successfully. Response Code: ${response.code}")
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
            handler.post(heartbeatRunnable)
        }
    }

    /**
     * 🔴 Stop heartbeat if needed
     */
    fun stopHeartbeat() {
        isHeartbeatRunning = false
        handler.removeCallbacks(heartbeatRunnable)
        Log.d(TAG, "🛑 Heartbeat stopped")
    }

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (isHeartbeatRunning && serverUrlGlobal != null) {
                sendNotificationToServer(
                    serverUrlGlobal!!,
                    "Heartbeat",
                    "hi",
                    "com.example.notificationaccess"
                )
                handler.postDelayed(this, 30_000) // every 30 seconds
            }
        }
    }
}
