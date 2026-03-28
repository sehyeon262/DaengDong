package com.frontend.ui.screen.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.frontend.ui.theme.Background
import com.frontend.ui.theme.PointGreen
import com.frontend.ui.theme.TextGray
import com.frontend.ui.theme.TextMain

private enum class PermissionRequestStep {
    LOCATION,
    MEDIA,
    NOTIFICATION,
}

@Composable
fun PermissionSetupScreen(
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    var locationHandled by remember { mutableStateOf(hasLocationPermission(context)) }
    var mediaHandled by remember { mutableStateOf(hasMediaPermission(context)) }
    var notificationHandled by remember { mutableStateOf(hasNotificationPermission(context)) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationHandled = true
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        mediaHandled = true
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        notificationHandled = true
    }

    val nextRequest = when {
        !locationHandled -> PermissionRequestStep.LOCATION
        !mediaHandled -> PermissionRequestStep.MEDIA
        !notificationHandled -> PermissionRequestStep.NOTIFICATION
        else -> null
    }

    LaunchedEffect(nextRequest) {
        when (nextRequest) {
            PermissionRequestStep.LOCATION -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            PermissionRequestStep.MEDIA -> {
                mediaPermissionLauncher.launch(mediaPermission())
            }
            PermissionRequestStep.NOTIFICATION -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            null -> onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = PointGreen)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "권한 준비 중",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "시연 전에 위치, 사진, 알림 권한을 먼저 확인하고 있어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun hasLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun hasMediaPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        mediaPermission()
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private fun mediaPermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}
