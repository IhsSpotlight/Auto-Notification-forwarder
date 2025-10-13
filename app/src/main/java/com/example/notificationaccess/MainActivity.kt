// MainActivity.kt
package com.example.notificationaccess

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var serverUrlInput: EditText
    private lateinit var saveButton: Button
    private lateinit var permissionButton: Button
    private lateinit var toggleSendingButton: Button // NEW button
    private lateinit var statusTextView: TextView    // NEW text view for logs

    private lateinit var sharedPref: android.content.SharedPreferences
    private var isSendingEnabled = false

    // Broadcast Receiver for receiving logs/status from NotificationSender
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra(NotificationSender.EXTRA_STATUS_MESSAGE)
            if (message != null) {
                // Append the new message to the existing text
                statusTextView.append("\n$message")
                // Optional: Scroll to the bottom
                // Note: requires a ScrollView around statusTextView in XML
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Find views
        serverUrlInput = findViewById(R.id.etServerUrl)
        saveButton = findViewById(R.id.saveButton)
        permissionButton = findViewById(R.id.permissionButton)
        toggleSendingButton = findViewById(R.id.toggleSendingButton) // NEW
        statusTextView = findViewById(R.id.tvConnectionStatus)       // NEW

        // 1. Load and display saved URL
        val savedUrl = sharedPref.getString("server_url", "")
        serverUrlInput.setText(savedUrl)

        // 2. Load and set initial state of toggle button
        isSendingEnabled = sharedPref.getBoolean("is_sending_enabled", false)
        updateToggleSendingButtonUI()

        // 3. Set Listeners
        saveButton.setOnClickListener {
            val url = serverUrlInput.text.toString().trim()
            sharedPref.edit { putString("server_url", url) }
            statusTextView.text = "URL Saved. Toggle sending data to apply changes."
        }

        permissionButton.setOnClickListener {
            // Open the notification listener settings
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        toggleSendingButton.setOnClickListener {
            isSendingEnabled = !isSendingEnabled // Toggle the state
            sharedPref.edit { putBoolean("is_sending_enabled", isSendingEnabled) }
            updateToggleSendingButtonUI()

            // Explicitly notify the service (optional, but ensures state change is immediate)
            if (isNotificationServiceEnabled()) {
                val intent = Intent(this, MyNotificationService::class.java).apply {
                    // This is a dummy intent to wake up the service.
                    // The service will re-check the shared preferences on next event.
                }
                startService(intent) // or use a custom broadcast from MainActivity to service
            } else {
                statusTextView.text = "Please grant notification access first."
            }
        }

        // Initialize status text
        statusTextView.text = "Status: App initialized."
        if (NotificationSender.isRunning()) {
            statusTextView.append("\nHeartbeat is currently running.")
        }
    }

    override fun onResume() {
        super.onResume()
        // Register the broadcast receiver when the activity is visible
        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver,
            IntentFilter(NotificationSender.ACTION_STATUS_UPDATE)
        )
        // Check and update permission status
        updatePermissionButtonUI()
        // Update the sending toggle UI
        isSendingEnabled = sharedPref.getBoolean("is_sending_enabled", false)
        updateToggleSendingButtonUI()
    }

    override fun onPause() {
        super.onPause()
        // Unregister the broadcast receiver when the activity is hidden
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver)
    }

    // Helper to update the permission button based on access status
    private fun updatePermissionButtonUI() {
        if (isNotificationServiceEnabled()) {
            permissionButton.text = "✅ Access Granted"
            permissionButton.isEnabled = false
        } else {
            permissionButton.text = "❌ Grant Notification Access"
            permissionButton.isEnabled = true
        }
    }

    // Helper to update the toggle button based on the internal state
    private fun updateToggleSendingButtonUI() {
        if (isSendingEnabled) {
            toggleSendingButton.text = "🛑 Stop Sending Data"
            // You can change color/style here if desired
        } else {
            toggleSendingButton.text = "🟢 Start Sending Data"
        }
    }

    // Helper function to check if the notification listener service is enabled
    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat.isNullOrEmpty()) return false

        val names = flat.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && TextUtils.equals(packageName, cn.packageName)) {
                return true
            }
        }
        return false
    }
}