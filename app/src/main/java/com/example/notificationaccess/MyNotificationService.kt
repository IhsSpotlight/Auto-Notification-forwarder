// MyNotificationService.kt
package com.example.notificationaccess

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationService : NotificationListenerService() {

    // New property to cache the state
    private var isSendingEnabled = false
    private lateinit var sharedPref: android.content.SharedPreferences

    override fun onCreate() {
        super.onCreate()
        // Initialize the sender with application context for broadcasting
        NotificationSender.initialize(applicationContext)
        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Initial load of the state
        isSendingEnabled = sharedPref.getBoolean("is_sending_enabled", false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isSendingEnabled) {
            Log.d("MyNotificationService", "Skipping notification: Sending is disabled by user.")
            return // Do nothing if the user has disabled sending
        }

        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString("android.title") ?: "No Title"
        val text = extras.getCharSequence("android.text")?.toString() ?: "No Content"
        val packageName = sbn.packageName

        val serverUrl = sharedPref.getString("server_url", null)

        if (!serverUrl.isNullOrEmpty()) {
            NotificationSender.sendNotificationToServer(serverUrl, title, text, packageName)
        } else {
            Log.w("MyNotificationService", "⚠️ Server URL not set. Cannot send data.")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // You can add logic here to handle removal if isSendingEnabled is true
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // When connected, check the state and start/stop heartbeat
        val serverUrl = sharedPref.getString("server_url", null)

        // Start heartbeat only if sending is enabled AND a URL is available
        if (isSendingEnabled && !serverUrl.isNullOrEmpty()) {
            NotificationSender.startHeartbeat(serverUrl)
        } else {
            NotificationSender.stopHeartbeat()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Stop heartbeat when the listener connection is lost
        NotificationSender.stopHeartbeat()
    }
}