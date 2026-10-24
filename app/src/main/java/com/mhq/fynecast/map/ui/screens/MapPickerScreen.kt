package com.mhq.fynecast.map.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhq.fynecast.R
import com.mhq.fynecast.core.ui.theme.AbyssalVoid
import com.mhq.fynecast.core.ui.theme.ElectricLimeGreen
import com.mhq.fynecast.map.presentation.MapPickerViewModel
import com.mhq.fynecast.map.ui.components.ErrorCard
import com.mhq.fynecast.map.ui.components.LoadingCard
import com.mhq.fynecast.map.ui.components.LocationConfirmCard
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import androidx.core.graphics.createBitmap

private const val MARKER_ICON_ID = "map_picker_marker"
private const val MARKER_SOURCE_ID = "map_picker_marker_source"
private const val MARKER_LAYER_ID = "map_picker_marker_layer"
private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val MAX_ZOOM = 14.0     // street level; the old 10.0 stopped at a whole-city view
private const val CITY_ZOOM = 8.0     // opening zoom when we know where to look
private const val WORLD_ZOOM = 1.5    // opening zoom when we don't know where to look

@Composable
fun MapPickerScreen(
    mapPickerViewModel: MapPickerViewModel,
    langCode: String,
    initialLatitude: Double?,
    initialLongitude: Double?,
    onLocationConfirmed: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by mapPickerViewModel.uiState.collectAsStateWithLifecycle()
    val latestLangCode by rememberUpdatedState(langCode)

    remember { MapLibre.getInstance(context) }
    val mapView = remember { MapView(context) }
    var markerSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    val markerFillColor = ElectricLimeGreen.toArgb()
    val markerStrokeColor = AbyssalVoid.toArgb()

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
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // The marker follows the ViewModel state, so it survives rotation and shows the moment
    // a tap happens, even while the place name is still being resolved.
    LaunchedEffect(markerSource, uiState.markerLatitude, uiState.markerLongitude) {
        val source = markerSource ?: return@LaunchedEffect
        val latitude = uiState.markerLatitude
        val longitude = uiState.markerLongitude
        if (latitude != null && longitude != null) {
            source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(longitude, latitude)))
        } else {
            source.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.setMaxZoomPreference(MAX_ZOOM)

                        // After a rotation the old marker is the best place to look; otherwise
                        // open on the current city, or on a world view if we have neither.
                        val savedState = mapPickerViewModel.uiState.value
                        val startLatitude = savedState.markerLatitude ?: initialLatitude
                        val startLongitude = savedState.markerLongitude ?: initialLongitude
                        map.cameraPosition = if (startLatitude != null && startLongitude != null) {
                            CameraPosition.Builder()
                                .target(LatLng(startLatitude, startLongitude))
                                .zoom(CITY_ZOOM)
                                .build()
                        } else {
                            CameraPosition.Builder()
                                .target(LatLng(20.0, 0.0))
                                .zoom(WORLD_ZOOM)
                                .build()
                        }

                        map.addOnMapClickListener { point ->
                            mapPickerViewModel.onMapClicked(
                                point.latitude,
                                point.longitude,
                                latestLangCode
                            )
                            true
                        }

                        map.setStyle(MAP_STYLE_URL) { style ->
                            localizeLabels(style, latestLangCode)
                            style.addImage(
                                MARKER_ICON_ID,
                                createMarkerBitmap(markerFillColor, markerStrokeColor)
                            )
                            // The marker is a plain GeoJSON point drawn by a symbol layer, so
                            // no annotation plugin is needed.
                            val source = GeoJsonSource(MARKER_SOURCE_ID)
                            style.addSource(source)
                            style.addLayer(
                                SymbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
                                    PropertyFactory.iconImage(MARKER_ICON_ID),
                                    PropertyFactory.iconAllowOverlap(true),
                                    PropertyFactory.iconIgnorePlacement(true)
                                )
                            )
                            markerSource = source
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
                    .navigationBarsPadding()
            )
        } else {
            uiState.errorMessage?.let { errorMessage ->
                ErrorCard(
                    message = errorMessage.asString(context),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            } ?: uiState.selectedPlace?.let { place ->
                LocationConfirmCard(
                    place = place,
                    onConfirm = onLocationConfirmed,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

/**
 * We draw a simple dot bitmap using the app's own theme colors and register it with the
 * style. Swap this out for a real drawable asset (via
 * ContextCompat.getDrawable(context, R.drawable.xxx).toBitmap()) whenever one exists.
 */
private fun createMarkerBitmap(fillColor: Int, strokeColor: Int): Bitmap {
    val size = 64
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val radius = size / 2f - 4f

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    canvas.drawCircle(size / 2f, size / 2f, radius, fillPaint)
    canvas.drawCircle(size / 2f, size / 2f, radius, strokePaint)
    return bitmap
}

/**
 * Shows map labels in the app language (e.g. "ar" or "en"). Every symbol layer whose label
 * uses a name field is switched to "name:<lang>", falling back to the local name when the
 * place has no translation. Call this before adding our own marker layer.
 */
private fun localizeLabels(style: Style, langCode: String) {
    val localizedName = Expression.coalesce(
        Expression.get("name:$langCode"),
        Expression.get("name")
    )
    style.layers.filterIsInstance<SymbolLayer>().forEach { layer ->
        if (layer.textField.toString().contains("name")) {
            layer.setProperties(PropertyFactory.textField(localizedName))
        }
    }
}