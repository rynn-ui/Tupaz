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
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tupaz.data.settings.ButtonSizeStorage
import com.tupaz.data.processing.ProcessingManager
import com.tupaz.data.processing.ProcessingStatus
import com.tupaz.ui.navigation.TupazNavHost
import com.tupaz.ui.theme.TupazTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.i("MainActivity", "POST_NOTIFICATIONS permission granted=$isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ProcessingManager.init(applicationContext)
        com.tupaz.data.storage.ModelStorage(applicationContext).ensureDefaultModelsProvisioned()

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

        val profileStorage = com.tupaz.data.profile.UserProfileStorage(applicationContext)
        val permissionStorage = com.tupaz.data.permission.PermissionStorage(applicationContext)

        val isOnboardingCompleted = profileStorage.isOnboardingCompleted()
        val hasSeenPermissionIntro = permissionStorage.hasCompletedPermissionIntro()

        val navigateToExtra = intent?.getStringExtra("navigate_to")
        val currentStatus = ProcessingManager.state.value.status
        val isNotificationLaunch = (navigateToExtra == "result")
        val hasActiveCompletion = ProcessingManager.consumeActiveSessionCompletion()

        val initialDestination = if (isNotificationLaunch ||
            currentStatus == ProcessingStatus.PROCESSING ||
            hasActiveCompletion) {
            "result"
        } else if (!hasSeenPermissionIntro) {
            "permissions"
        } else if (!isOnboardingCompleted) {
            "onboarding"
        } else {
            "home"
        }

        if (isOnboardingCompleted && profileStorage.isPendingSync()) {
            lifecycleScope.launch(Dispatchers.IO) {
                com.tupaz.data.firebase.FirebaseSyncManager(applicationContext).retryPendingSyncIfNeeded()
            }
        }

        val buttonSizeStorage = ButtonSizeStorage(applicationContext)

        setContent {
            val buttonSize by buttonSizeStorage.buttonSizeFlow.collectAsState()
            TupazTheme(buttonSize = buttonSize) {
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



