package com.example.notifforwarder

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MyNotificationService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString("android.title") ?: "No Title"
        val text = extras.getCharSequence("android.text")?.toString() ?: "No Content"
        val packageName = sbn.packageName

        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val serverUrl = sharedPref.getString("server_url", null)

        if (!serverUrl.isNullOrEmpty()) {
            NotificationSender.sendNotificationToServer(serverUrl, title, text, packageName)
        } else {
            println("⚠️ Server URL not set")
        }
    }
}
