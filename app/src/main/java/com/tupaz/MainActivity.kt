package com.tupaz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.data.processing.ProcessingStatus
import com.tupaz.ui.navigation.TupazNavHost
import com.tupaz.ui.theme.TupazTheme

/**
 * Main application Activity host.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.i("MainActivity", "POST_NOTIFICATIONS permission granted=$isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ProcessingManager.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val navigateToExtra = intent?.getStringExtra("navigate_to")
        val currentStatus = ProcessingManager.state.value.status
        val initialDestination = if (navigateToExtra == "result" ||
            currentStatus == ProcessingStatus.PROCESSING ||
            currentStatus == ProcessingStatus.COMPLETED) {
            "result"
        } else {
            "home"
        }

        setContent {
            TupazTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TupazNavHost(startDestination = initialDestination)
                }
            }
        }
    }
}

