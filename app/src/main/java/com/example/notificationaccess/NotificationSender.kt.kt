package com.example.notifforwarder

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object NotificationSender {
    private val client = OkHttpClient()

    fun sendNotificationToServer(
        serverUrl: String,
        title: String,
        content: String,
        packageName: String
    ) {
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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ Failed to send notification: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                println("✅ Notification sent: ${response.code}")
                response.close()
            }
        })
    }
}
