package com.frontend.ui.screen.home

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frontend.ui.screen.home.components.CharacterSection
import com.frontend.ui.screen.home.components.HomeHeader
import com.frontend.ui.screen.home.components.WeatherInfoRow
import com.frontend.ui.screen.home.components.WeeklyReportSection
import com.frontend.ui.theme.Background
import com.frontend.ui.theme.TextGray
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationLoaded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            fetchLocation(context) { lat, lng, fallbackAddress ->
                scope.launch {
                    val address = fallbackAddress ?: withContext(Dispatchers.IO) {
                        getAddressFromLatLng(context, lat, lng)
                    }
                    viewModel.loadHomeData(lat, lng, address)
                    locationLoaded = true
                }
            }
        } else {
            viewModel.loadHomeData(35.2322, 128.5469, "마산 내서읍")
            locationLoaded = true
        }
    }

    LaunchedEffect(Unit) {
        if (!locationLoaded) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "오류가 발생했습니다",
                        fontSize = 14.sp,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        fetchLocation(context) { lat, lng, fallbackAddress ->
                            scope.launch {
                                val address = fallbackAddress ?: withContext(Dispatchers.IO) {
                                    getAddressFromLatLng(context, lat, lng)
                                }
                                viewModel.loadHomeData(lat, lng, address)
                            }
                        }
                    }) {
                        Text("다시 시도")
                    }
                }
            }
        }

        state.data != null -> {
            val data = state.data!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                HomeHeader(
                    location = state.address,
                    dogName = data.user?.dogName ?: "",
                    profileImageUrl = data.user?.dogProfileImageUrl
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (data.walk != null) {
                    CharacterSection(
                        characterType = data.walk.characterType,
                        walkStatus = data.walk.walkStatus,
                        walkMessage = data.walk.walkMessage,
                        temperature = data.weather?.temperature ?: 20
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (data.weather != null) {
                    WeatherInfoRow(weatherInfo = data.weather)
                }

                Spacer(modifier = Modifier.height(24.dp))

                WeeklyReportSection(weeklySummary = data.weeklySummary)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocation(
    context: android.content.Context,
    onResult: (Double, Double, String?) -> Unit
) {
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    val isInKorea = { lat: Double, lng: Double ->
        lat in 33.0..39.0 && lng in 124.0..132.0
    }

    fusedClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        CancellationTokenSource().token
    ).addOnSuccessListener { location ->
        if (location != null && isInKorea(location.latitude, location.longitude)) {
            onResult(location.latitude, location.longitude, null)
        } else {
            onResult(35.2322, 128.5469, "마산 내서읍")
        }
    }.addOnFailureListener {
        onResult(35.2322, 128.5469, "마산 내서읍")
    }
}

private fun getAddressFromLatLng(
    context: android.content.Context,
    lat: Double,
    lng: Double
): String {
    return try {
        val geocoder = Geocoder(context, Locale.KOREAN)
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val city = addr.adminArea ?: addr.locality ?: ""
            val district = addr.subLocality ?: addr.locality ?: ""
            if (district.isNotEmpty() && district != city) "$city $district"
            else city.ifEmpty { "현재 위치" }
        } else {
            "현재 위치"
        }
    } catch (e: Exception) {
        "현재 위치"
    }
}
