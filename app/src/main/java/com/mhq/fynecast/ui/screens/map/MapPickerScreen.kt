package com.mhq.fynecast.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mhq.fynecast.R
import com.mhq.fynecast.data.repository.PhotonLocationRepository
import com.mhq.fynecast.domain.models.CitySuggestionModel
import com.mhq.fynecast.ui.theme.MidnightBlue
import com.mhq.fynecast.ui.theme.NeonGreen
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@Composable
fun MapPickerScreen(
    onLocationPicked: (CitySuggestionModel) -> Unit,
    onBackClick: () -> Unit,
    photonLocationRepository: PhotonLocationRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // 1. FORCED INITIALIZATION: Do this before anything else
    remember { MapLibre.getInstance(context) }

    var selectedLocation by remember { mutableStateOf<CitySuggestionModel?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // 2. CREATE MAPVIEW: Managed locally
    val mapView = remember { MapView(context) }

    // 3. LIFECYCLE BRIDGE: Crucial for rendering tiles
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        // 4. STYLE: Ensure this URL is reachable
                        map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                        map.setMaxZoomPreference(10.0)

                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(30.0444, 31.2357))
                            .zoom(5.0)
                            .build()

                        map.addOnMapClickListener { point ->
                            isSearching = true
                            map.clear()
                            map.addMarker(MarkerOptions().position(point))

                            scope.launch {
                                try {
                                    val suggestion = photonLocationRepository.getCityNameOffMap(
                                        point.latitude, point.longitude
                                    )
                                    selectedLocation = suggestion ?: CitySuggestionModel(
                                        //fullName = "Point (${"%.3f".format(point.latitude)}, ${"%.3f".format(point.longitude)})",
                                        fullName = "Point (${point.latitude}, ${point.longitude})",
                                        latitude = point.latitude,
                                        longitude = point.longitude
                                    )
                                    selectedLocation = suggestion
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSearching = false
                                }
                            }
                            true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // UI Overlays
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .background(color = MidnightBlue.copy(alpha = 0.7f), shape = CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.back), tint = Color.White)
        }

        selectedLocation?.let { location ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightBlue.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = location.fullName, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onLocationPicked(location) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.confirm_location), color = MidnightBlue)
                        }
                    }
                }
            }
        }
    }
}
