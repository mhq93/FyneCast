package com.mhq.fynecast.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.home.domain.models.CitySuggestionModel
import com.mhq.fynecast.map.ui.MapPickerViewModel
import com.mhq.fynecast.map.ui.components.ErrorCard
import com.mhq.fynecast.map.ui.components.LoadingCard
import com.mhq.fynecast.map.ui.components.LocationConfirmCard
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

@Composable
fun MapPickerScreen(
    viewModel: MapPickerViewModel,
    langCode: String,
    onLocationPicked: (CitySuggestionModel) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    remember { MapLibre.getInstance(context) }
    val mapView = remember { MapView(context) }

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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.setStyle("https://tiles.openfreemap.org/styles/liberty")
                        map.setMaxZoomPreference(10.0)
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(30.0444, 31.2357))
                            .zoom(5.0)
                            .build()

                        map.addOnMapClickListener { point ->
                            map.clear()
                            map.addMarker(MarkerOptions().position(point))

                            viewModel.onMapClicked(point.latitude, point.longitude, langCode)
                            true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .background(
                    color = AbyssalVoid.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.back),
                tint = Color.White
            )
        }

        if (uiState.isSearching) {
            LoadingCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        } else {
            uiState.errorMessage?.let { message ->
                ErrorCard(
                    message = message,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } ?: uiState.selectedLocation?.let { location ->
                LocationConfirmCard(
                    location = location,
                    onLocationPicked = onLocationPicked,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}