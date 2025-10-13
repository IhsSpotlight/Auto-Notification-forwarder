package com.example.notificationaccess

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var serverUrlInput: EditText
    private lateinit var saveButton: Button
    private lateinit var permissionButton: Button // Add a button for permission

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serverUrlInput = findViewById(R.id.etServerUrl)

        // You will need to add this button to your activity_main.xml layout
        permissionButton = findViewById(R.id.permissionButton)

        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedUrl = sharedPref.getString("server_url", "")
        serverUrlInput.setText(savedUrl)

        saveButton.setOnClickListener {
            val url = serverUrlInput.text.toString().trim()
            sharedPref.edit().putString("server_url", url).apply()
        }

        permissionButton.setOnClickListener {
            // When the button is clicked, open the notification listener settings
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        // When the user returns to the app, check if the permission is now enabled
        if (isNotificationServiceEnabled()) {
            // Permission is granted, you can update the UI if needed
            // For example, disable the permission button
            permissionButton.text = "Permission Granted"
            permissionButton.isEnabled = false
        } else {
            // Permission is not granted
            permissionButton.text = "Grant Notification Access"
            permissionButton.isEnabled = true
        }
    }

    // Helper function to check if the notification listener service is enabled
    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null) {
                    if (TextUtils.equals(packageName, cn.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
